import pascal.taie.World;
import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.CallGraphs;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.graph.flowgraph.FlowKind;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.*;
import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.*;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.*;
import pascal.taie.language.classes.*;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * specialized for new Thread(Lambda)
 * creating Threads with Lambda Expression
 * and Non-Static lock fields
 */
public class ParserB implements Plugin {

    private Solver solver;

    // ===== 锁相关状态 =====
    private final List<LockAcquisition> lockAcquisitions = new ArrayList<>();
    private final Set<LockOrderEdge> lockOrderEdges = new HashSet<>();
    private final Set<CSObj> threadObjs = new HashSet<>();
    private final Map<CSMethod, Set<HeldLock>> entryLocks = new HashMap<>();
    private final Map<CSVar, Boolean> lockVarIsStatic = new HashMap<>();
    private final Map<Obj, JMethod> lambdaImplMethods = new HashMap<>();
    private final Set<CSMethod> bridgedRunMethods = new LinkedHashSet<>();

    /** 已处理过的 (recvObj, callSite) 对，用于 processCall 去重 */
    private final Set<String> processedCalls = new HashSet<>();

    private static final List<String> LOCK_TYPES = List.of(
            "java.util.concurrent.locks.Lock",
            "java.util.concurrent.locks.ReentrantLock",
            "java.util.concurrent.locks.ReentrantReadWriteLock",
            "java.util.concurrent.locks.ReadWriteLock");

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
    }

    @Override
    public void onStart() {
        System.out.println("[DeadlockPlugin] started");
    }

    // ============================================================
    // 1) 新的 CSMethod 可达：处理锁事件 + invokedynamic
    // ============================================================
    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        JMethod method = csMethod.getMethod();
        if (method.getIR() == null) return;

        System.out.println("[onNewCSMethod]: " + method.getSignature());

        Context context = csMethod.getContext();
        int[] nesting = {0};   // 方法内嵌套深度

        for (Stmt stmt : method.getIR().getStmts()) {
            if (stmt instanceof Monitor monitor) {
                handleMonitor(csMethod, monitor, nesting);
            } else if (stmt instanceof Invoke invoke) {
                handleInvoke(csMethod, invoke, nesting);
            } else if (stmt instanceof LoadField loadField) {
                // 记录静态字段读取，供 isStaticLock 用
                if (loadField.getFieldAccess() instanceof StaticFieldAccess) {
                    CSVar csVar = solver.getCSManager().getCSVar(context, loadField.getLValue());
                    lockVarIsStatic.put(csVar, true);
                }
            } else if (stmt instanceof New newStmt) {
                // 记录 Thread 对象
                Type type = newStmt.getRValue().getType();
                if (type instanceof ClassType ct
                        && ct.getName().equals("java.lang.Thread")) {
                    CSVar csVar = solver.getCSManager().getCSVar(context, newStmt.getLValue());
                    // 注意：此处只是占位，实际 threadObjs 在 onNewPointsToSet 里加
                }
            }
        }
    }

    // ============================================================
    // 2) 新的 points-to：Thread.start 桥接 + 实例调用处理
    // ============================================================
    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        Var var = csVar.getVar();
        JMethod method = var.getMethod();
        if (method == null) return;
        Context context = csVar.getContext();
        CSMethod csMethod = solver.getCSManager().getCSMethod(context, method);

        // ★ 检查 var 是否是某个 invokedynamic 的 lValue
        JMethod lambdaMethod = findLambdaMethodForVar(method, var);

        for (CSObj obj : pts) {
            // 记录 Thread 对象
            Type objType = obj.getObject().getType();
            if (objType instanceof ClassType ct
                    && ct.getName().equals("java.lang.Thread")) {
                threadObjs.add(obj);
            }

            // 关联 lambda：Tai-e 造的 lambda 对象 → lambdaMethod
            if (lambdaMethod != null)
                lambdaImplMethods.put(obj.getObject(), lambdaMethod);

            processCall(csVar, obj, csMethod);
        }
    }

    /** 如果 var 是某个 invokedynamic 的 lValue，返回其 lambdaMethod；否则 null */
    private JMethod findLambdaMethodForVar(JMethod method, Var var) {
        if (method.getIR() == null) return null;
        for (Stmt stmt : method.getIR().getStmts()) {
            if (stmt instanceof Invoke invoke
                    && invoke.getLValue() == var
                    && invoke.getInvokeExp() instanceof InvokeDynamic dynamicExp) {
                MethodHandle implHandle = null;
                for (Literal arg : dynamicExp.getBootstrapArgs()) {
                    if (arg instanceof MethodHandle mh) implHandle = mh;
                }
                return implHandle != null
                        ? implHandle.getMethodRef().resolve() : null;
            }
        }
        return null;
    }

    // ============================================================
    // 3) Monitor 处理
    // ============================================================
    private void handleMonitor(CSMethod csMethod, Monitor stmt, int[] nesting) {
        Context context = csMethod.getContext();
        Var lockVar = stmt.getObjectRef();
        CSVar csLockVar = solver.getCSManager().getCSVar(context, lockVar);

        if (stmt.isEnter()) {
            LockAcquisition la = new LockAcquisition(csMethod, stmt, csLockVar, true);
            la.nestingDepth = ++nesting[0];
            lockAcquisitions.add(la);
        } else {
            LockAcquisition la = new LockAcquisition(csMethod, stmt, csLockVar, false);
            la.nestingDepth = nesting[0]--;
            lockAcquisitions.add(la);
        }
    }

    // ============================================================
    // 4) Invoke 处理：ReentrantLock.lock/unlock + invokedynamic
    // ============================================================
    private void handleInvoke(CSMethod csMethod, Invoke stmt, int[] nesting) {
        Context context = csMethod.getContext();
        InvokeExp invokeExp = stmt.getInvokeExp();

        // 4.1 ReentrantLock.lock/unlock
        if (invokeExp instanceof InvokeInstanceExp instanceExp) {
            Var base = instanceExp.getBase();
            if (base != null) {
                Type t = base.getType();
                if (t instanceof ClassType ct && LOCK_TYPES.contains(ct.getName())) {
                    JMethod m = invokeExp.getMethodRef().resolve();
                    System.out.println("[handleInvoke]: " + m.getSignature());
                    if (m.getName().equals("lock") || m.getName().equals("unlock")) {
                        System.out.println("[handleInvoke]: handle lock() and unlock()");
                        CSVar csLockVar = solver.getCSManager().getCSVar(context, base);
                        boolean isEnter = m.getName().equals("lock");
                        LockAcquisition la = new LockAcquisition(csMethod, stmt, csLockVar, isEnter);
                        if (isEnter) {
                            la.nestingDepth = ++nesting[0];
                        } else {
                            la.nestingDepth = nesting[0]--;
                        }
                        lockAcquisitions.add(la);
                    }
                }
            }
        }

        // 4.2 invokedynamic → lambda
        if (invokeExp instanceof InvokeDynamic dynamicExp) {
            System.out.println("[handleInvoke]: invokedynamic");
            MethodHandle implHandle = null;
            for (Literal arg : dynamicExp.getBootstrapArgs()) {
                if (arg instanceof MethodHandle mh) implHandle = mh;
            }
            JMethod lambdaMethod = implHandle != null
                    ? implHandle.getMethodRef().resolve() : null;

            System.out.println(implHandle + " " + lambdaMethod.getSignature());

            if (lambdaMethod != null) {
                // 直接让 lambdaMethod 可达
                CSCallSite csCallSite = solver.getCSManager().getCSCallSite(context, stmt);
                Context lambdaContext = solver.getContextSelector().selectContext(csCallSite, lambdaMethod);
                CSMethod csLambda = solver.getCSManager().getCSMethod(lambdaContext, lambdaMethod);
                solver.addCallEdge(new Edge<>(CallKind.VIRTUAL, csCallSite, csLambda));

                // 给 invokedynamic 的返回值分配一个 CSObj
                Var lhs = stmt.getLValue();
                if (lhs != null) {
                    CSVar csLhs = solver.getCSManager().getCSVar(context, lhs);

                    // 1) 构造一个 mock Obj，代表这个 lambda 对象
                    //    desc:     用于 toString / 调试
                    //    alloc:    分配点，用 invokedynamic 语句本身
                    //    type:     lambda 表达式的类型（Runnable）
                    //    container: 所在方法（csMethod 的 JMethod）
                    //    isFunctional: 是否是函数式对象
                    Type lambdaType = stmt.getInvokeExp().getType();   // 或者 lhs.getType()
                    Obj lambdaObj = solver.getHeapModel().getMockObj(
                            () -> "Lambda@" + stmt.getLineNumber(),   // Descriptor
                            stmt,                                     // alloc site
                            lambdaType,                               // Type
                            csMethod.getMethod(),                     // container JMethod
                            true                                      // isFunctional
                    );

                    // 2) 为这个 Obj 选堆上下文，得到 CSObj
                    CSObj csLambdaObj = solver.getCSManager().getCSObj(
                            solver.getContextSelector().selectHeapContext(csMethod, lambdaObj),
                            lambdaObj);

                    // 3) 把 CSObj 加到 lhs 的 points-to 集合
                    PointsToSet csObjs = solver.makePointsToSet();
                    csObjs.addObject(csLambdaObj);
                    solver.addPointsTo(csLhs, csObjs);
                }
            }
        }
    }

    // ============================================================
    // 5) processCall：Thread.<init> / Thread.start 桥接
    // ============================================================
    private void processCall(CSVar recv, CSObj recvObj, CSMethod csMethod) {
        Context recvContext = recv.getContext();
        for (Invoke callSite : recv.getVar().getInvokes()) {
            JMethod callee = resolveCallee(recvObj, callSite);
            // [processCall] recv=[]:<Deadlock_Reentrant: void main(java.lang.String[])>/%v3
            // recvObj=[]:NewObj{<Deadlock_Reentrant: void main(java.lang.String[])>[0@L15] new java.lang.Thread}
            // callSite=<Deadlock_Reentrant: void main(java.lang.String[])>[3@L15] invokespecial %v3.<init>(%v4)
            // callee=<java.lang.Thread: void <init>(java.lang.Runnable)>
            // [processCall]: init- <java.lang.Thread: java.lang.Runnable target>
            System.out.println("[processCall] recv=" + recv
                    + ", recvObj=" + recvObj
                    + ", callSite=" + callSite
                    + ", callee=" + callee);
            if (callee == null) continue;

            // ---- Thread.<init>(Runnable) ----
            if (isThreadMethod(callee, "<init>")) {
                JField targetField = World.get().getClassHierarchy()
                        .getClass("java.lang.Thread").getDeclaredField("target");
                System.out.println("[processCall]: init- " + targetField);
                if (targetField != null) {
                    // thread.target
                    Pointer targetPtr = solver.getCSManager()
                            .getInstanceField(recvObj, targetField);
                    List<Var> args = callSite.getInvokeExp().getArgs();
                    if (!args.isEmpty()) {
                        // runnable
                        CSVar argVar = solver.getCSManager()
                                .getCSVar(recvContext, args.get(0));
                        // thread.target = runnable
                        // 建边，供 PFG 用
                        solver.addPFGEdge(argVar, targetPtr, FlowKind.INSTANCE_STORE);
                    }
                }
            }

            // ---- Thread.start() ----
            if (isThreadMethod(callee, "start")) {
                // 去重：同一 (recvObj, callSite) 只处理一次
                String key = System.identityHashCode(recvObj) + "@"
                        + System.identityHashCode(callSite);
                if (!processedCalls.add(key)) continue;

                JField targetField = World.get().getClassHierarchy()
                        .getClass("java.lang.Thread").getDeclaredField("target");
                System.out.println("[processCall]: start- " + targetField);
                Pointer targetPtr = solver.getCSManager()
                        .getInstanceField(recvObj, targetField);
                PointsToSet targetPts = solver.getPointsToSetOf(targetPtr);

                System.out.println(targetPts);

                for (CSObj targetObj : targetPts) {
                    Obj obj = targetObj.getObject();
                    System.out.println("[start] target obj = " + obj
                            + ", type = " + obj.getType()
                            + ", lambdaImplMethods size = " + lambdaImplMethods.size());
                    JMethod targetRun = lambdaImplMethods.get(obj);
                    System.out.println("  lookup = " + targetRun);

                    if (targetRun == null) {
                        targetRun = resolveRunByType(obj.getType());
                    }
                    if (targetRun == null) continue;

                    CSCallSite csCallSite = solver.getCSManager()
                            .getCSCallSite(recvContext, callSite);
                    Context calleeContext = solver.getContextSelector()
                            .selectContext(csCallSite, targetObj, targetRun);
                    CSMethod csCallee = solver.getCSManager()
                            .getCSMethod(calleeContext, targetRun);
                    solver.addCallEdge(new Edge<>(CallKind.VIRTUAL, csCallSite, csCallee));

                    Var mThis = targetRun.getIR().getThis();
                    if (mThis != null) {
                        Pointer thisPtr = solver.getCSManager().getCSVar(calleeContext, mThis);
                        PointsToSet csObjs = solver.makePointsToSet();
                        csObjs.addObject(targetObj);
                        solver.addPointsTo(thisPtr, csObjs);
                    }
                }
            }
        }
    }

    private boolean isThreadMethod(JMethod m, String name) {
        return m.getDeclaringClass().getName().equals("java.lang.Thread")
                && m.getName().equals(name);
    }

    private JMethod resolveRunByType(Type type) {
        if (!(type instanceof ClassType ct)) return null;
        JClass cls = ct.getJClass();
        if (cls == null) return null;
        for (JMethod m : cls.getDeclaredMethods()) {
            if (m.getName().equals("run") && !m.isAbstract()) {
                return m;
            }
        }
        JClass superCls = cls.getSuperClass();
        if (superCls != null) return resolveRunByType(superCls.getType());
        return null;
    }

    private JMethod resolveCallee(CSObj recv, Invoke callSite) {
        Type type = recv != null ? recv.getObject().getType() : null;
        return CallGraphs.resolveCallee(type, callSite);
    }

    /**
     * 第一趟：遍历 main 的 IR，找到所有 Thread.<init>(Runnable) 调用，
     * 把 argVar 的 points-to 手动加到 threadObj.target 字段上。
     *
     * 注意：不使用 addPFGEdge，因为 onFinish 阶段 worklist 不会再跑，
     * 必须直接 addPointsTo。
     */
    private void bridgeThreadInitPass() {
        JField targetField = World.get().getClassHierarchy()
                .getClass("java.lang.Thread").getDeclaredField("target");
        if (targetField == null) {
            System.out.println("[bridgeThreadInitPass] Thread.target not found");
            return;
        }

        JMethod mainMethod = World.get().getMainMethod();
        if (mainMethod == null || mainMethod.getIR() == null) return;

        Context emptyCtx = solver.getContextSelector().getEmptyContext();
        CSMethod csMain = solver.getCSManager().getCSMethod(emptyCtx, mainMethod);

        int updated = 0;

        for (Stmt stmt : mainMethod.getIR().getStmts()) {
            if (!(stmt instanceof Invoke invoke)) continue;

            JMethod callee = resolveInvoke(null, invoke);
            if (!isThreadMethod(callee, "<init>")) continue;

            InvokeExp invokeExp = invoke.getInvokeExp();
            if (!(invokeExp instanceof InvokeInstanceExp instanceExp)) continue;

            Var base = instanceExp.getBase();
            if (base == null) continue;
            CSVar csBase = solver.getCSManager().getCSVar(csMain.getContext(), base);
            PointsToSet threadPts = solver.getPointsToSetOf(csBase);

            List<Var> args = invokeExp.getArgs();
            if (args.isEmpty()) continue;
            CSVar argVar = solver.getCSManager().getCSVar(csMain.getContext(), args.get(0));
            PointsToSet argPts = solver.getPointsToSetOf(argVar);

            System.out.println("[bridgeThreadInitPass] invoke=" + invoke
                    + ", threadPts=" + threadPts
                    + ", argVar=" + argVar
                    + ", argPts=" + argPts);

            for (CSObj threadObj : threadPts) {
                Pointer targetPtr = solver.getCSManager()
                        .getInstanceField(threadObj, targetField);

                for (CSObj o : argPts) {
                    targetPtr.getPointsToSet().addObject(o);   // ★ 直接加
                }
            }
        }

        System.out.println("[bridgeThreadInitPass] updated " + updated + " target points-to");
    }

    /**
     * 第二趟：遍历 main 的 IR，找到所有 Thread.start() 调用，
     * 读 threadObj.target 字段的 points-to，建 start -> run 边，
     * 并把 targetObj 传给 run() 的 this。
     */
    private void bridgeThreadStartPass() {
        JField targetField = World.get().getClassHierarchy()
                .getClass("java.lang.Thread").getDeclaredField("target");
        if (targetField == null) {
            System.out.println("[bridgeThreadStartPass] Thread.target not found");
            return;
        }

        JMethod mainMethod = World.get().getMainMethod();
        if (mainMethod == null || mainMethod.getIR() == null) return;

        Context emptyCtx = solver.getContextSelector().getEmptyContext();
        CSMethod csMain = solver.getCSManager().getCSMethod(emptyCtx, mainMethod);

        int bridged = 0;

        for (Stmt stmt : mainMethod.getIR().getStmts()) {
            if (!(stmt instanceof Invoke invoke)) continue;

            JMethod callee = resolveInvoke(null, invoke);
            if (!isThreadMethod(callee, "start")) continue;

            InvokeExp invokeExp = invoke.getInvokeExp();
            if (!(invokeExp instanceof InvokeInstanceExp instanceExp)) continue;

            Var base = instanceExp.getBase();
            if (base == null) continue;
            CSVar csBase = solver.getCSManager().getCSVar(csMain.getContext(), base);
            PointsToSet threadPts = solver.getPointsToSetOf(csBase);

            for (CSObj threadObj : threadPts) {
                Pointer targetPtr = solver.getCSManager()
                        .getInstanceField(threadObj, targetField);
                PointsToSet targetPts = solver.getPointsToSetOf(targetPtr);

                System.out.println("[bridgeThreadStartPass] thread=" + threadObj
                        + ", targetPts=" + targetPts);

                for (CSObj targetObj : targetPts) {
                    Obj obj = targetObj.getObject();

                    // 1) lambda 对象 → lambdaMethod
                    JMethod targetRun = lambdaImplMethods.get(obj);
                    // 2) 匿名类 / 静态内部类 → resolveRunByType
                    if (targetRun == null) {
                        targetRun = resolveRunByType(obj.getType());
                    }
                    if (targetRun == null) continue;

                    CSCallSite csCallSite = solver.getCSManager()
                            .getCSCallSite(csMain.getContext(), invoke);
                    Context calleeContext = solver.getContextSelector()
                            .selectContext(csCallSite, targetObj, targetRun);
                    CSMethod csCallee = solver.getCSManager()
                            .getCSMethod(calleeContext, targetRun);
                    solver.addCallEdge(new Edge<>(CallKind.VIRTUAL, csCallSite, csCallee));
                    bridgedRunMethods.add(csCallee);   // ★ 收集

                    // ★ 把 targetObj 传给 run() 的 this
                    /*
                    Var mThis = targetRun.getIR().getThis();
                    if (mThis != null) {
                        CSVar csThis = solver.getCSManager().getCSVar(calleeContext, mThis);
                        PointsToSet pts = solver.makePointsToSet();
                        pts.addObject(targetObj);
                        solver.addPointsTo(csThis, pts);   // 用 addPointsTo，虽然 onFinish 阶段不生效
                    }*/

                    bridged++;
                    System.out.println("[bridgeThreadStartPass] bridged "
                            + targetObj + " -> " + targetRun);
                }
            }
        }

        System.out.println("[bridgeThreadStartPass] total bridged = " + bridged);
    }

    /**
     * 第三趟：遍历所有可达方法，收集 ReentrantLock.lock() / unlock()。
     * 因为 onFinish 阶段 onNewCSMethod 不会再触发，
     * 新可达的 run() 里的 lock()/unlock() 需要手动收集。
     */
    /*
    private void collectLockAcquisitionsFromReachableMethods() {
        // 拷贝，避免 ConcurrentModificationException
        List<CSMethod> reachable = new ArrayList<>(
                solver.getCallGraph().reachableMethods().toList());

        int added = 0;

        for (CSMethod csMethod : reachable) {
            JMethod method = csMethod.getMethod();
            if (method.getIR() == null) continue;

            Context context = csMethod.getContext();
            int[] nesting = {0};

            for (Stmt stmt : method.getIR().getStmts()) {
                if (!(stmt instanceof Invoke invoke)) continue;

                InvokeExp invokeExp = invoke.getInvokeExp();
                if (!(invokeExp instanceof InvokeInstanceExp instanceExp)) continue;

                Var base = instanceExp.getBase();
                if (base == null) continue;

                Type t = base.getType();
                if (!(t instanceof ClassType ct)) continue;
                if (!LOCK_TYPES.contains(ct.getName())) continue;

                JMethod m = invokeExp.getMethodRef().resolve();
                if (m == null) continue;
                if (!m.getName().equals("lock") && !m.getName().equals("unlock")) continue;

                // 去重：同一 (csMethod, stmt) 只加一次
                boolean exists = lockAcquisitions.stream().anyMatch(la ->
                        la.method.equals(csMethod) && la.stmt.equals(invoke));
                if (exists) continue;

                CSVar csLockVar = solver.getCSManager().getCSVar(context, base);
                boolean isEnter = m.getName().equals("lock");
                LockAcquisition la = new LockAcquisition(csMethod, invoke, csLockVar, isEnter);
                if (isEnter) {
                    la.nestingDepth = ++nesting[0];
                } else {
                    la.nestingDepth = nesting[0]--;
                }
                lockAcquisitions.add(la);
                added++;

                System.out.println("[collectLock] added " + la);
            }
        }

        System.out.println("[collectLock] total added = " + added
                + ", lockAcquisitions.size = " + lockAcquisitions.size());
    }
*/
    private void collectLockAcquisitionsFromReachableMethods() {
        // 从 bridgedRunMethods 出发，递归遍历所有被调用的方法
        Deque<CSMethod> workList = new ArrayDeque<>(bridgedRunMethods);
        Set<CSMethod> visited = new HashSet<>(bridgedRunMethods);

        int added = 0;

        while (!workList.isEmpty()) {
            CSMethod csMethod = workList.poll();
            JMethod method = csMethod.getMethod();
            if (method.getIR() == null) continue;

            System.out.println("[collectLock] scanning " + method.getSignature());

            Context context = csMethod.getContext();
            int[] nesting = {0};

            for (Stmt stmt : method.getIR().getStmts()) {
                Map<Var, Set<CSObj>> fieldPtsMap = new HashMap<>();
                // ---- LoadField: $r1 = LOCK_A ----
                /*
                if (stmt instanceof LoadField loadField) {
                    if (loadField.getFieldAccess() instanceof StaticFieldAccess) {
                        JField field = loadField.getFieldAccess().getFieldRef().resolve();
                        StaticField staticField = solver.getCSManager().getStaticField(field);
                        CSVar varPtr = solver.getCSManager().getCSVar(context, loadField.getLValue());

                        // ★ 直接手动传播 points-to
                        //PointsToSet staticPts = solver.getPointsToSetOf(staticField);
                        Set<CSObj> staticPts = new HashSet<>();
                        solver.getPointsToSetOf(staticField).forEach(staticPts::add);
                        fieldPtsMap.put(loadField.getLValue(), staticPts);   // ★ 存起来
                        System.out.println("[collectLock] LoadField: " + loadField
                                + ", staticField=" + staticField
                                + ", staticPts=" + staticPts);

                        // ★ 加在这里
                        System.out.println("[collectLock] varPtr identity = "
                                + System.identityHashCode(varPtr)
                                + ", pts after add = " + solver.getPointsToSetOf(varPtr));

                        lockVarIsStatic.put(varPtr, true);
                    }
                    continue;
                }

                 */
                if (stmt instanceof LoadField loadField) {
                    if (loadField.getFieldAccess() instanceof StaticFieldAccess) {
                        JField field = loadField.getFieldAccess().getFieldRef().resolve();
                        StaticField staticField = solver.getCSManager().getStaticField(field);
                        Set<CSObj> staticPts = new HashSet<>();
                        solver.getPointsToSetOf(staticField).forEach(staticPts::add);
                        fieldPtsMap.put(loadField.getLValue(), staticPts);
                        System.out.println("[collectLock] LoadField: " + loadField
                                + ", staticPts=" + staticPts);
                        lockVarIsStatic.put(solver.getCSManager().getCSVar(context, loadField.getLValue()), true);
                    }
                    continue;
                }

                // ---- StoreField: LOCK_A = $r1 ----
                if (stmt instanceof StoreField storeField) {
                    if (storeField.getFieldAccess() instanceof StaticFieldAccess) {
                        JField field = storeField.getFieldAccess().getFieldRef().resolve();
                        StaticField staticField = solver.getCSManager().getStaticField(field);
                        CSVar varPtr = solver.getCSManager().getCSVar(context, storeField.getRValue());
                        solver.addPFGEdge(varPtr, staticField, FlowKind.STATIC_STORE);
                        System.out.println("[collectLock] StoreField: " + storeField
                                + ", varPtr=" + varPtr + ", staticField=" + staticField);
                    }
                    continue;
                }
                // ---- Monitor ----
                if (stmt instanceof Monitor monitor) {
                    Var lockVar = monitor.getObjectRef();
                    CSVar csLockVar = solver.getCSManager().getCSVar(context, lockVar);
                    boolean isEnter = monitor.isEnter();

                    boolean exists = lockAcquisitions.stream().anyMatch(la ->
                            la.method.equals(csMethod) && la.stmt.equals(monitor));
                    if (exists) continue;

                    LockAcquisition la = new LockAcquisition(csMethod, monitor, csLockVar, isEnter);
                    if (isEnter) {
                        la.nestingDepth = ++nesting[0];
                    } else {
                        la.nestingDepth = nesting[0]--;
                    }
                    lockAcquisitions.add(la);
                    added++;
                    System.out.println("[collectLock] added (Monitor) " + la);
                }
                // ---- ReentrantLock.lock/unlock ----
                else if (stmt instanceof Invoke invoke) {
                    InvokeExp invokeExp = invoke.getInvokeExp();

                    // 1) 尝试解析 callee，递归遍历
                    JMethod callee = null;
                    if (invokeExp instanceof InvokeInstanceExp instanceExp) {
                        Var base = instanceExp.getBase();
                        if (base != null) {
                            CSVar csBase = solver.getCSManager().getCSVar(context, base);
                            for (CSObj recvObj : solver.getPointsToSetOf(csBase)) {
                                callee = resolveCallee(recvObj, invoke);
                                if (callee != null) break;
                            }
                        }
                    } else {
                        callee = resolveCallee(null, invoke);
                    }
                    if (callee == null) {
                        callee = invokeExp.getMethodRef().resolve();
                    }

                    // 2) 如果是 ReentrantLock.lock/unlock，收集
                    if (callee != null) {
                        String cls = callee.getDeclaringClass().getName();
                        String name = callee.getName();
                        if (LOCK_TYPES.contains(cls) && (name.equals("lock") || name.equals("unlock"))) {
                            if (invokeExp instanceof InvokeInstanceExp instanceExp) {
                                Var base = instanceExp.getBase();
                                if (base != null) {
                                    CSVar csLockVar = solver.getCSManager().getCSVar(context, base);
                                    boolean isEnter = name.equals("lock");

                                    LockAcquisition la = new LockAcquisition(csMethod, invoke, csLockVar, isEnter);
                                    Set<CSObj> objs = fieldPtsMap.get(base);
                                    if (objs != null) la.lockObjs.addAll(objs);
                                    if (la.lockObjs.isEmpty()) {
                                        solver.getPointsToSetOf(csLockVar).forEach(la.lockObjs::add);
                                    }
                                    if (isEnter) {
                                        la.nestingDepth = ++nesting[0];
                                    } else {
                                        la.nestingDepth = nesting[0]--;
                                    }
                                    lockAcquisitions.add(la);
                                    added++;
                                    System.out.println("[collectLock] added (ReentrantLock) " + la
                                            + ", lockObjs=" + la.lockObjs);
                                }
                            }
                        }
                        // 3) 递归：把 callee 加进 worklist
                        //    ★ 就是这里，替换掉原来的 visited.add / workList.add
                        if (callee.getDeclaringClass().isApplication()) {
                            CSMethod csCallee = solver.getCSManager()
                                    .getCSMethod(context, callee);   // 用 caller 的 context，避免 selectContext
                            if (visited.add(csCallee)) {
                                workList.add(csCallee);
                            }
                        }
                    }
                }
            }
        }

        System.out.println("[collectLock] total added = " + added
                + ", lockAcquisitions.size = " + lockAcquisitions.size());
    }
    private JMethod resolveInvoke(JMethod caller, Invoke invoke) {
        // 先试 resolveCallee
        JMethod callee = resolveCallee(null, invoke);
        if (callee != null) return callee;
        // 兜底：直接用 MethodRef.resolve()
        return invoke.getInvokeExp().getMethodRef().resolve();
    }

    private void fillLockVarPointsTo() {
        for (LockAcquisition la : lockAcquisitions) {
            if (la.lockVar == null) continue;
            if (!solver.getPointsToSetOf(la.lockVar).isEmpty()) continue;

            // 找 la.stmt 里的 base
            if (la.stmt instanceof Invoke invoke
                    && invoke.getInvokeExp() instanceof InvokeInstanceExp instanceExp) {
                Var base = instanceExp.getBase();
                if (base == null) continue;

                // 找 base 对应的 LoadField
                JMethod method = la.method.getMethod();
                for (Stmt s : method.getIR().getStmts()) {
                    if (s instanceof LoadField lf
                            && lf.getLValue() == base
                            && lf.getFieldAccess() instanceof StaticFieldAccess) {
                        JField field = lf.getFieldAccess().getFieldRef().resolve();
                        StaticField staticField = solver.getCSManager().getStaticField(field);
                        PointsToSet staticPts = solver.getPointsToSetOf(staticField);
                        System.out.println("[fillLockVar] la=" + la
                                + ", staticPts=" + staticPts);
                        for (CSObj o : staticPts) {
                            la.lockVar.getPointsToSet().addObject(o);   // ★ 直接加
                        }
                    }
                }
            }
        }
    }

    // ============================================================
    // 6) onFinish：锁图构建 + 环检测 + 导出
    // ============================================================
    @Override
    public void onFinish() {
        System.out.println("[onFinish]: " + lockAcquisitions.size()
                + " lock acquisitions, " + lockOrderEdges.size() + " lock order edges");
        System.out.println("[onFinish]: reachableMethods: " + solver.getCallGraph().reachableMethods().toList().size());
        System.out.println(solver.getCallGraph().reachableMethods().toList());

        // ★ 分三趟桥接 Thread
        //bridgeThreadInitPass();    // 第一趟：Thread.<init> -> target 字段
        //bridgeThreadStartPass();   // 第二趟：Thread.start -> run
        //collectLockAcquisitionsFromReachableMethods();  // 第三趟：收集 lock()/unlock()

        dumpThreadTarget();
        computeEntryLocks();

        //fillLockVarPointsTo();

        buildLockOrderGraph();
        dumpLockOrderGraph();
        detectDeadlock();
        exportGraph();
    }

    // ---------- 以下是原 Solver 的锁分析逻辑，几乎原样搬过来 ----------

    public static class LockAcquisition {
        final CSMethod method;
        final Stmt stmt;
        final CSVar lockVar;
        final boolean isEnter;
        int nestingDepth;

        Set<CSObj> lockObjs = new HashSet<>();   // ★

        public LockAcquisition(CSMethod method, Stmt stmt, CSVar lockVar, boolean isEnter) {
            this.method = method;
            this.stmt = stmt;
            this.lockVar = lockVar;
            this.isEnter = isEnter;
        }

        @Override
        public String toString() {
            return method.getMethod() + "@" + stmt.getLineNumber() + " " + lockVar;
        }
    }

    public static class LockOrderEdge {
        final LockAcquisition fromLa;
        final LockAcquisition toLa;
        final CSObj fromObj;
        final CSObj toObj;

        public LockOrderEdge(LockAcquisition fromLa, LockAcquisition toLa,
                             CSObj fromObj, CSObj toObj) {
            this.fromLa = fromLa;
            this.toLa = toLa;
            this.fromObj = fromObj;
            this.toObj = toObj;
        }

        private String edgeKey() {
            return objKey(fromObj) + "->" + objKey(toObj)
                    + "|" + laKey(fromLa) + "->" + laKey(toLa);
        }

        private String objKey(CSObj obj) {
            return obj == null || obj.getObject() == null ? "?" : obj.getObject().toString();
        }

        private String laKey(LockAcquisition la) {
            if (la == null) return "?";
            String m = la.method.getMethod().toString();
            int line = la.stmt != null ? la.stmt.getLineNumber() : -1;
            String v = la.lockVar != null ? la.lockVar.getVar().toString() : "?";
            return m + "@" + line + "#" + v;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof LockOrderEdge e)) return false;
            return edgeKey().equals(e.edgeKey());
        }

        @Override
        public int hashCode() {
            return edgeKey().hashCode();
        }

        @Override
        public String toString() {
            return fromObj + " -> " + toObj + "\n" + fromLa + " -> " + toLa + "\n";
        }
    }

    private record HeldLock(CSObj obj, LockAcquisition la) {}

    private void dumpThreadTarget() {
        JField targetField = World.get().getClassHierarchy()
                .getClass("java.lang.Thread").getDeclaredField("target");
        if (targetField == null) {
            System.out.println("[dumpThreadTarget] Thread.target not found");
            return;
        }
        System.out.println("[dumpThreadTarget] Thread objects: " + threadObjs.size());
        for (CSObj threadObj : threadObjs) {
            Pointer fieldPtr = solver.getCSManager().getInstanceField(threadObj, targetField);
            System.out.println("  target of " + threadObj + " -> "
                    + solver.getPointsToSetOf(fieldPtr));
        }
    }

    private void dumpLockOrderGraph() {
        System.out.println("=== Lock Order Graph ===");
        if (lockOrderEdges.isEmpty()) {
            System.out.println("  (empty)");
            return;
        }
        for (LockOrderEdge edge : lockOrderEdges) System.out.println("  " + edge);
    }

    private void buildLockOrderGraph() {
        Map<CSMethod, List<LockAcquisition>> byMethod = groupLockAcquisitions();
        for (Map.Entry<CSMethod, List<LockAcquisition>> e : byMethod.entrySet()) {
            CSMethod m = e.getKey();
            List<Stmt> stmts = m.getMethod().getIR().getStmts();
            List<LockAcquisition> events = new ArrayList<>(e.getValue());
            events.sort(Comparator.comparingInt(la -> stmts.indexOf(la.stmt)));

            Deque<HeldLock> heldStack = new ArrayDeque<>(
                    entryLocks.getOrDefault(m, new HashSet<>()));

            for (LockAcquisition la : events) {
                /*
                if (la.isEnter) {
                    Set<CSObj> toObjs = new HashSet<>(la.lockObjs);   // ★ 直接用
                    if (toObjs.isEmpty()) {
                        solver.getPointsToSetOf(la.lockVar).forEach(toObjs::add);   // 兜底
                    }
                    //Set<CSObj> toObjs = new HashSet<>();
                    //la.lockVar.getPointsToSet().forEach(toObjs::add);
                    //solver.getPointsToSetOf(la.lockVar).forEach(toObjs::add);
                    // ★ 加在这里
                    System.out.println("[buildLockOrderGraph] la=" + la
                            + ", la.lockVar identity = " + System.identityHashCode(la.lockVar)
                            + ", pts = " + solver.getPointsToSetOf(la.lockVar)
                            + ", toObjs=" + toObjs);

                    System.out.println("[buildLockOrderGraph] la=" + la
                            + ", lockVar=" + la.lockVar
                            + ", toObjs=" + toObjs);
                    for (HeldLock held : heldStack) {
                        for (CSObj toObj : toObjs) {
                            lockOrderEdges.add(new LockOrderEdge(
                                    held.la(), la, held.obj(), toObj));
                        }
                    }
                    for (CSObj toObj : toObjs) heldStack.push(new HeldLock(toObj, la));
                } else {
                    Set<CSObj> toRemove = new HashSet<>();
                    la.lockVar.getPointsToSet().forEach(toRemove::add);
                    heldStack.removeIf(h -> toRemove.contains(h.obj()));
                }

                 */
                if (la.isEnter) {
                    Set<CSObj> toObjs = new HashSet<>(la.lockObjs);
                    if (toObjs.isEmpty()) {
                        solver.getPointsToSetOf(la.lockVar).forEach(toObjs::add);
                    }
                    System.out.println("[buildLockOrderGraph] la=" + la
                            + ", toObjs=" + toObjs
                            + ", heldStack=" + heldStack);
                    for (HeldLock held : heldStack) {
                        for (CSObj toObj : toObjs) {
                            lockOrderEdges.add(new LockOrderEdge(held.la(), la, held.obj(), toObj));
                        }
                    }
                    for (CSObj toObj : toObjs) heldStack.push(new HeldLock(toObj, la));
                } else {
                    Set<CSObj> toRemove = new HashSet<>(la.lockObjs);
                    if (toRemove.isEmpty()) {
                        solver.getPointsToSetOf(la.lockVar).forEach(toRemove::add);
                    }
                    heldStack.removeIf(h -> toRemove.contains(h.obj()));
                }
            }
        }
    }

    private Map<CSMethod, List<LockAcquisition>> groupLockAcquisitions() {
        Map<CSMethod, List<LockAcquisition>> byMethod = new HashMap<>();
        for (LockAcquisition la : lockAcquisitions) {
            if (la.method.getMethod().getName().equals("<clinit>")) continue;
            byMethod.computeIfAbsent(la.method, k -> new ArrayList<>()).add(la);
        }
        return byMethod;
    }

    private void computeEntryLocks() {
        Map<CSMethod, List<LockAcquisition>> byMethod = groupLockAcquisitions();

        for (CSMethod m : solver.getCallGraph().reachableMethods().toList()) {
            entryLocks.computeIfAbsent(m, k -> new HashSet<>());
        }

        boolean changed = true;
        int round = 0;
        while (changed) {
            changed = false;
            round++;
            for (CSMethod caller : solver.getCallGraph().reachableMethods().toList()) {
                Set<HeldLock> callerEntry = entryLocks.get(caller);
                List<LockAcquisition> events = byMethod.getOrDefault(caller, List.of());
                Map<Stmt, Set<HeldLock>> heldAtCallSite =
                        computeHeldAtCallSites(caller, events, callerEntry);

                for (CSCallSite csCallSite : solver.getCallGraph().getCallSitesIn(caller)) {
                    Stmt callStmt = csCallSite.getCallSite();
                    Set<HeldLock> heldAt = heldAtCallSite.getOrDefault(callStmt, new HashSet<>());
                    for (CSMethod callee : solver.getCallGraph().getCalleesOf(csCallSite)) {
                        Set<HeldLock> newEntry = new HashSet<>();
                        newEntry.addAll(callerEntry);
                        newEntry.addAll(heldAt);
                        Set<HeldLock> calleeEntry = entryLocks.get(callee);
                        if (calleeEntry == null) continue;
                        if (calleeEntry.addAll(newEntry)) changed = true;
                    }
                }
            }
            System.out.println("computeEntryLocks round " + round + " "
                    + (changed ? "changed" : "converged"));
        }
    }


    /*
    private void computeEntryLocks() {
        Map<CSMethod, List<LockAcquisition>> byMethod = groupLockAcquisitions();

        // ★ 收集所有需要遍历的方法
        Set<CSMethod> allMethods = new LinkedHashSet<>();
        allMethods.addAll(solver.getCallGraph().reachableMethods().toList());
        allMethods.addAll(bridgedRunMethods);
        for (LockAcquisition la : lockAcquisitions) {
            allMethods.add(la.method);
        }

        for (CSMethod m : allMethods) {
            entryLocks.computeIfAbsent(m, k -> new HashSet<>());
        }

        boolean changed = true;
        int round = 0;
        while (changed) {
            changed = false;
            round++;
            for (CSMethod caller : allMethods) {
                Set<HeldLock> callerEntry = entryLocks.get(caller);
                List<LockAcquisition> events = byMethod.getOrDefault(caller, List.of());
                Map<Stmt, Set<HeldLock>> heldAtCallSite =
                        computeHeldAtCallSites(caller, events, callerEntry);

                // ★ 不用 getCallSitesIn / getCalleesOf，改成遍历 caller 的 IR
                for (Stmt stmt : caller.getMethod().getIR().getStmts()) {
                    if (!(stmt instanceof Invoke invoke)) continue;

                    // 解析 callee
                    JMethod callee = null;
                    InvokeExp invokeExp = invoke.getInvokeExp();
                    if (invokeExp instanceof InvokeInstanceExp instanceExp) {
                        Var base = instanceExp.getBase();
                        if (base != null) {
                            CSVar csBase = solver.getCSManager().getCSVar(caller.getContext(), base);
                            for (CSObj recvObj : solver.getPointsToSetOf(csBase)) {
                                callee = resolveCallee(recvObj, invoke);
                                if (callee != null) break;
                            }
                        }
                    } else {
                        callee = resolveCallee(null, invoke);
                    }
                    if (callee == null) {
                        callee = invokeExp.getMethodRef().resolve();
                    }
                    if (callee == null) continue;
                    if (!callee.getDeclaringClass().isApplication()) continue;

                    CSMethod csCallee = solver.getCSManager()
                            .getCSMethod(caller.getContext(), callee);
                    if (!allMethods.contains(csCallee)) continue;

                    Set<HeldLock> heldAt = heldAtCallSite.getOrDefault(stmt, new HashSet<>());
                    Set<HeldLock> newEntry = new HashSet<>();
                    newEntry.addAll(callerEntry);
                    newEntry.addAll(heldAt);
                    Set<HeldLock> calleeEntry = entryLocks.get(csCallee);
                    if (calleeEntry == null) continue;
                    if (calleeEntry.addAll(newEntry)) changed = true;
                }
            }
            System.out.println("computeEntryLocks round " + round + " "
                    + (changed ? "changed" : "converged"));
        }
    }

     */

    private Map<Stmt, Set<HeldLock>> computeHeldAtCallSites(
            CSMethod m, List<LockAcquisition> events, Set<HeldLock> entry) {
        List<Stmt> stmts = m.getMethod().getIR().getStmts();
        List<LockAcquisition> sorted = new ArrayList<>(events);
        sorted.sort(Comparator.comparingInt(la -> stmts.indexOf(la.stmt)));

        List<Stmt> callSites = new ArrayList<>();
        for (Stmt s : stmts) if (s instanceof Invoke) callSites.add(s);
        callSites.sort(Comparator.comparingInt(stmts::indexOf));

        Map<Stmt, Set<HeldLock>> result = new HashMap<>();
        Deque<HeldLock> stack = new ArrayDeque<>(entry);

        int i = 0, j = 0;
        while (i < sorted.size() || j < callSites.size()) {
            int lockIdx = i < sorted.size() ? stmts.indexOf(sorted.get(i).stmt) : Integer.MAX_VALUE;
            int callIdx = j < callSites.size() ? stmts.indexOf(callSites.get(j)) : Integer.MAX_VALUE;

            if (lockIdx <= callIdx && i < sorted.size()) {
                LockAcquisition la = sorted.get(i++);
                if (la.isEnter) {
                    for (CSObj o : solver.getPointsToSetOf(la.lockVar))
                        stack.push(new HeldLock(o, la));
                } else {
                    Set<CSObj> toRemove = new HashSet<>();
                    solver.getPointsToSetOf(la.lockVar).forEach(toRemove::add);
                    stack.removeIf(h -> toRemove.contains(h.obj()));
                }
            } else if (j < callSites.size()) {
                Stmt cs = callSites.get(j++);
                result.put(cs, new HashSet<>(stack));
            } else break;
        }
        return result;
    }

    private void detectDeadlock() {
        Map<CSObj, Set<CSObj>> adj = new HashMap<>();
        for (LockOrderEdge e : lockOrderEdges)
            adj.computeIfAbsent(e.fromObj, k -> new HashSet<>()).add(e.toObj);

        Set<CSObj> visited = new HashSet<>();
        Set<CSObj> inStack = new HashSet<>();
        Deque<CSObj> path = new ArrayDeque<>();
        for (CSObj node : adj.keySet()) {
            if (dfsFindCycle(node, adj, visited, inStack, path)) {
                System.out.println("=== Deadlock detected! Cycle: " + path);
                return;
            }
        }
        System.out.println("=== No deadlock detected ===");
    }

    private boolean dfsFindCycle(CSObj node, Map<CSObj, Set<CSObj>> adj,
                                 Set<CSObj> visited, Set<CSObj> inStack,
                                 Deque<CSObj> path) {
        if (inStack.contains(node)) {
            path.addLast(node);
            return true;
        }
        if (visited.contains(node)) return false;
        visited.add(node);
        inStack.add(node);
        path.addLast(node);
        for (CSObj next : adj.getOrDefault(node, new HashSet<>()))
            if (dfsFindCycle(next, adj, visited, inStack, path)) return true;
        inStack.remove(node);
        path.removeLast();
        return false;
    }

    // ---------- 导出 ----------

    private void exportGraph() {
        String fileName = "graph-" + System.currentTimeMillis() + ".txt";
        try (FileWriter writer = new FileWriter(fileName)) {
            List<List<LockOrderEdge>> cycles = findAllCycles();
            int cycleId = 0;
            for (List<LockOrderEdge> cycle : cycles) {
                writer.write("CYCLE " + cycleId++ + "\n");

                Map<String, Integer> nodeIdMap = new LinkedHashMap<>();
                List<LockAcquisition> nodeLaList = new ArrayList<>();
                List<CSObj> nodeObjList = new ArrayList<>();

                for (LockOrderEdge e : cycle) {
                    String fromKey = nodeKey(e.fromObj);
                    if (!nodeIdMap.containsKey(fromKey)) {
                        nodeIdMap.put(fromKey, nodeIdMap.size());
                        nodeLaList.add(e.fromLa);
                        nodeObjList.add(e.fromObj);
                    }
                    String toKey = nodeKey(e.toObj);
                    if (!nodeIdMap.containsKey(toKey)) {
                        nodeIdMap.put(toKey, nodeIdMap.size());
                        nodeLaList.add(e.toLa);
                        nodeObjList.add(e.toObj);
                    }
                }

                for (int i = 0; i < nodeLaList.size(); i++) {
                    LockAcquisition la = nodeLaList.get(i);
                    CSObj obj = nodeObjList.get(i);
                    if (la == null) {
                        writer.write(String.format("NODE %d,%s,%s,%s,%d,%b,%b,%d,%d\n",
                                i, "?", obj.getObject().toString(), "?", -1,
                                isStaticLock(obj), false, 0, 1));
                        continue;
                    }
                    String var = la.lockVar != null ? la.lockVar.getVar().toString() : "?";
                    String method = la.method.getMethod().getDeclaringClass().getSimpleName()
                            + "." + la.method.getMethod().getName();
                    int line = la.stmt != null ? la.stmt.getLineNumber() : -1;
                    boolean isSync = Modifier.hasSynchronized(la.method.getMethod().getModifiers());
                    int nesting = la.nestingDepth;
                    int ptsSize = la.lockVar != null ? solver.getPointsToSetOf(la.lockVar).size() : 1;
                    writer.write(String.format("NODE %d,%s,%s,%s,%d,%b,%b,%d,%d\n",
                            i, var, obj.getObject().toString(), method, line,
                            isStaticLock(obj), isSync, nesting, ptsSize));
                }

                for (LockOrderEdge e : cycle) {
                    int fromId = nodeIdMap.get(nodeKey(e.fromObj));
                    int toId = nodeIdMap.get(nodeKey(e.toObj));
                    String fromType = e.fromObj.getObject().getType().toString();
                    String toType = e.toObj.getObject().getType().toString();
                    boolean sameClass = fromType.equals(toType);
                    int callDistance = -1;
                    if (e.fromLa != null && e.toLa != null)
                        callDistance = computeCallDistance(e.fromLa.method, e.toLa.method);
                    boolean mayAlias = false;
                    if (e.fromLa != null && e.toLa != null
                            && e.fromLa.lockVar != null && e.toLa.lockVar != null) {
                        Set<CSObj> fromPts = new HashSet<>();
                        solver.getPointsToSetOf(e.fromLa.lockVar).forEach(fromPts::add);
                        for (CSObj o : solver.getPointsToSetOf(e.toLa.lockVar))
                            if (fromPts.contains(o)) { mayAlias = true; break; }
                    }
                    boolean sameLockObj = e.fromObj.equals(e.toObj);
                    writer.write(String.format("EDGE %d,%d,%b,%d,%b,%b\n",
                            fromId, toId, sameClass, callDistance, mayAlias, sameLockObj));
                }
                writer.write("LABEL 0\n\n");
            }
            System.out.println("Graph data written to: " + fileName);
            System.out.println("Total cycles: " + cycleId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String nodeKey(CSObj obj) {
        return obj.getObject().toString();
    }

    private List<List<LockOrderEdge>> findAllCycles() {
        Map<CSObj, List<LockOrderEdge>> adj = new HashMap<>();
        for (LockOrderEdge e : lockOrderEdges)
            adj.computeIfAbsent(e.fromObj, k -> new ArrayList<>()).add(e);

        Map<String, List<LockOrderEdge>> uniqueCycles = new LinkedHashMap<>();
        for (CSObj start : adj.keySet()) {
            Deque<LockOrderEdge> path = new ArrayDeque<>();
            Set<CSObj> inStack = new HashSet<>();
            dfsFindCyclesFromStart(start, start, adj, inStack, path, uniqueCycles);
        }
        return new ArrayList<>(uniqueCycles.values());
    }

    private void dfsFindCyclesFromStart(CSObj start, CSObj cur,
                                        Map<CSObj, List<LockOrderEdge>> adj,
                                        Set<CSObj> inStack,
                                        Deque<LockOrderEdge> path,
                                        Map<String, List<LockOrderEdge>> outCycles) {
        inStack.add(cur);
        for (LockOrderEdge e : adj.getOrDefault(cur, Collections.emptyList())) {
            CSObj next = e.toObj;
            if (next.equals(start)) {
                List<LockOrderEdge> cycle = new ArrayList<>(path);
                cycle.add(e);
                List<LockOrderEdge> normalized = normalizeCycle(cycle);
                outCycles.putIfAbsent(cycleObjKey(normalized), normalized);
            } else if (!inStack.contains(next)) {
                path.addLast(e);
                dfsFindCyclesFromStart(start, next, adj, inStack, path, outCycles);
                path.removeLast();
            }
        }
        inStack.remove(cur);
    }

    private String cycleObjKey(List<LockOrderEdge> cycle) {
        if (cycle == null || cycle.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (LockOrderEdge e : cycle) sb.append(objKey(e.fromObj)).append("->");
        sb.append(objKey(cycle.get(0).fromObj));
        return sb.toString();
    }

    private String objKey(CSObj obj) {
        return obj == null || obj.getObject() == null ? "?" : obj.getObject().toString();
    }

    private List<LockOrderEdge> normalizeCycle(List<LockOrderEdge> cycle) {
        if (cycle == null || cycle.isEmpty()) return cycle;
        int n = cycle.size();
        int bestIdx = 0;
        for (int i = 1; i < n; i++)
            if (compareEdge(cycle.get(i), cycle.get(bestIdx)) < 0) bestIdx = i;
        List<LockOrderEdge> normalized = new ArrayList<>(n);
        for (int i = 0; i < n; i++) normalized.add(cycle.get((bestIdx + i) % n));
        return normalized;
    }

    private int compareEdge(LockOrderEdge a, LockOrderEdge b) {
        int c = compareObj(a.fromObj, b.fromObj);
        if (c != 0) return c;
        c = compareObj(a.toObj, b.toObj);
        if (c != 0) return c;
        c = compareLa(a.fromLa, b.fromLa);
        if (c != 0) return c;
        return compareLa(a.toLa, b.toLa);
    }

    private int compareObj(CSObj a, CSObj b) {
        String sa = a == null || a.getObject() == null ? "" : a.getObject().toString();
        String sb = b == null || b.getObject() == null ? "" : b.getObject().toString();
        return sa.compareTo(sb);
    }

    private int compareLa(LockAcquisition a, LockAcquisition b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        int c = a.method.getMethod().toString().compareTo(b.method.getMethod().toString());
        if (c != 0) return c;
        int la = a.stmt != null ? a.stmt.getLineNumber() : -1;
        int lb = b.stmt != null ? b.stmt.getLineNumber() : -1;
        return Integer.compare(la, lb);
    }

    private boolean isStaticLock(CSObj obj) {
        for (LockAcquisition la : lockAcquisitions) {
            if (la.lockVar == null) continue;
            if (solver.getPointsToSetOf(la.lockVar).contains(obj))
                return lockVarIsStatic.getOrDefault(la.lockVar, false);
        }
        return false;
    }

    private int computeCallDistance(CSMethod from, CSMethod to) {
        if (from.equals(to)) return 0;
        Queue<CSMethod> queue = new ArrayDeque<>();
        Map<CSMethod, Integer> dist = new HashMap<>();
        queue.add(from);
        dist.put(from, 0);
        while (!queue.isEmpty()) {
            CSMethod cur = queue.poll();
            int d = dist.get(cur);
            for (CSCallSite csCallSite : solver.getCallGraph().getCallSitesIn(cur)) {
                for (Edge<CSCallSite, CSMethod> e
                        : solver.getCallGraph().edgesOutOf(csCallSite).toList()) {
                    CSMethod succ = e.getCallee();
                    if (!dist.containsKey(succ)) {
                        if (succ.equals(to)) return d + 1;
                        dist.put(succ, d + 1);
                        queue.add(succ);
                    }
                }
            }
        }
        return -1;
    }

    // ---------- 供外部查询结果 ----------
    public Set<LockOrderEdge> getLockOrderEdges() {
        return lockOrderEdges;
    }
}