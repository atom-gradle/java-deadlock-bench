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
 * static and non-static lock objects
 */
public class MyPlugin implements Plugin {

    private Solver solver;

    private final List<LockAcquisition> lockAcquisitions = new ArrayList<>();
    private final Set<LockOrderEdge> lockOrderEdges = new HashSet<>();
    private final Set<CSObj> threadObjs = new HashSet<>();
    private final Map<CSMethod, Set<HeldLock>> entryLocks = new HashMap<>();
    private final Map<CSVar, Boolean> lockVarIsStatic = new HashMap<>();
    private final Map<Obj, JMethod> lambdaImplMethods = new HashMap<>();

    // 已处理过的 (recvObj, callSite) 对，用于 processCall 去重
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

            // ★ 加 null 检查
            if (lambdaMethod == null) {
                System.out.println("[handleInvoke]: lambda method unresolved, skip");
                return;   // 或者 continue 到后面的逻辑
            }

            System.out.println(implHandle + " " + lambdaMethod.getSignature());

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

    private JMethod resolveInvoke(JMethod caller, Invoke invoke) {
        // 先试 resolveCallee
        JMethod callee = resolveCallee(null, invoke);
        if (callee != null) return callee;
        // 兜底：直接用 MethodRef.resolve()
        return invoke.getInvokeExp().getMethodRef().resolve();
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

        dumpThreadTarget();
        computeEntryLocks();

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

        Set<CSObj> lockObjs = new HashSet<>();

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
                if (la.isEnter) {
                    Set<CSObj> toObjs = new HashSet<>(la.lockObjs);   // ★ 直接用
                    if (toObjs.isEmpty())
                        solver.getPointsToSetOf(la.lockVar).forEach(toObjs::add);   // 兜底
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
                            + ", lockVar identity=" + System.identityHashCode(la.lockVar)
                            + ", toObjs=" + toObjs
                            + ", pts=" + solver.getPointsToSetOf(la.lockVar));
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

                /*
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

                 */
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

    private void exportGraph() {
        String fileName = "graph-" + System.currentTimeMillis() + ".txt";
        try (FileWriter writer = new FileWriter(fileName)) {

            // ===== 预处理：获锁点索引 =====
            Map<String, LockAcquisition> acquisitionMap = new LinkedHashMap<>();
            Map<String, Set<CSObj>> acqToObjs = new HashMap<>();
            Map<CSObj, Set<String>> objToAcqs = new HashMap<>();
            for (LockAcquisition la : lockAcquisitions) {
                String k = acqKey(la);
                acquisitionMap.putIfAbsent(k, la);
                Set<CSObj> objs = acqToObjs.computeIfAbsent(k, x -> new HashSet<>());
                la.lockVar.getPointsToSet().forEach(objs::add);
            }
            for (var e : acqToObjs.entrySet())
                for (CSObj o : e.getValue())
                    objToAcqs.computeIfAbsent(o, k -> new LinkedHashSet<>()).add(e.getKey());

            // ===== 找环 =====
            List<List<LockOrderEdge>> cycles = findAllCycles();

            int cycleId = 0;
            for (List<LockOrderEdge> cycle : cycles) {
                writer.write("CYCLE " + cycleId++ + "\n\n");

                // ---------- Layer 1: 锁对象节点 ----------
                Map<String, Integer> lockNodeId = new LinkedHashMap<>();
                List<CSObj> lockNodeObj = new ArrayList<>();

                for (LockOrderEdge e : cycle) {
                    for (CSObj o : List.of(e.fromObj, e.toObj)) {
                        String key = objKey(o);
                        if (!lockNodeId.containsKey(key)) {
                            lockNodeId.put(key, lockNodeId.size());
                            lockNodeObj.add(o);
                        }
                    }
                }

                writer.write("# Layer 1: Lock Objects\n");
                for (int i = 0; i < lockNodeObj.size(); i++) {
                    CSObj o = lockNodeObj.get(i);
                    writer.write(String.format(
                            "LOCK_NODE %d,%s,%s,%b,%d\n",
                            i, objKey(o),
                            o.getObject().getType().toString(),
                            isStaticLock(o),
                            o.getObject() != null ? 1 : 0));
                }

                // ---------- Layer 1: 锁顺序边 ----------
                for (LockOrderEdge e : cycle) {
                    int fromId = lockNodeId.get(objKey(e.fromObj));
                    int toId = lockNodeId.get(objKey(e.toObj));

                    String fromM = e.fromLa != null
                            ? e.fromLa.method.getMethod().getDeclaringClass().getSimpleName()
                            + "." + e.fromLa.method.getMethod().getName() : "?";
                    int fromL = e.fromLa != null && e.fromLa.stmt != null
                            ? e.fromLa.stmt.getLineNumber() : -1;
                    String toM = e.toLa != null
                            ? e.toLa.method.getMethod().getDeclaringClass().getSimpleName()
                            + "." + e.toLa.method.getMethod().getName() : "?";
                    int toL = e.toLa != null && e.toLa.stmt != null
                            ? e.toLa.stmt.getLineNumber() : -1;

                    int dist = (e.fromLa != null && e.toLa != null)
                            ? computeCallDistance(e.fromLa.method, e.toLa.method) : -1;

                    boolean mayAlias = false;
                    if (e.fromLa != null && e.toLa != null
                            && e.fromLa.lockVar != null && e.toLa.lockVar != null) {
                        Set<CSObj> fp = new HashSet<>();
                        e.fromLa.lockVar.getPointsToSet().forEach(fp::add);
                        for (CSObj o : e.toLa.lockVar.getPointsToSet())
                            if (fp.contains(o)) { mayAlias = true; break; }
                    }
                    boolean sameLockObj = e.fromObj.equals(e.toObj);

                    writer.write(String.format(
                            "LOCK_EDGE %d,%d,%s,%d,%s,%d,%d,%b,%b\n",
                            fromId, toId, fromM, fromL, toM, toL,
                            dist, mayAlias, sameLockObj));
                }
                writer.write("\n");

                // ---------- Layer 2: 获锁点节点 ----------
                // 环上所有边的 fromLa / toLa 涉及的获锁点
                Map<String, Integer> acqNodeId = new LinkedHashMap<>();
                List<LockAcquisition> acqNodeList = new ArrayList<>();

                for (LockOrderEdge e : cycle) {
                    for (LockAcquisition la : List.of(e.fromLa, e.toLa)) {
                        if (la == null) continue;
                        String k = acqKey(la);
                        if (!acqNodeId.containsKey(k)) {
                            acqNodeId.put(k, acqNodeId.size());
                            acqNodeList.add(la);
                        }
                    }
                }

                writer.write("# Layer 2: Lock Acquisitions\n");
                for (int i = 0; i < acqNodeList.size(); i++) {
                    LockAcquisition la = acqNodeList.get(i);
                    String method = la.method.getMethod().getDeclaringClass().getSimpleName()
                            + "." + la.method.getMethod().getName();
                    int line = la.stmt != null ? la.stmt.getLineNumber() : -1;
                    String var = la.lockVar != null ? la.lockVar.getVar().toString() : "?";
                    boolean isStatic = false;
                    if (la.lockVar != null && !la.lockVar.getPointsToSet().isEmpty()) {
                        CSObj anyObj = la.lockVar.getPointsToSet().iterator().next();
                        isStatic = isStaticLock(anyObj);
                    }
                    boolean isSync = Modifier.hasSynchronized(la.method.getMethod().getModifiers());
                    int nesting = la.nestingDepth;
                    int ptsSize = la.lockVar != null ? la.lockVar.getPointsToSet().size() : 1;

                    writer.write(String.format(
                            "ACQ_NODE %d,%s,%d,%s,%b,%b,%d,%d\n",
                            i, method, line, var,
                            isStatic, isSync, nesting, ptsSize));
                }

                // ---------- Layer 2: 获锁点之间的调用图边 ----------
                // 对环上每对相邻的获锁点，看它们在调用图上是否可达
                for (int i = 0; i < acqNodeList.size(); i++) {
                    for (int j = 0; j < acqNodeList.size(); j++) {
                        if (i == j) continue;
                        LockAcquisition a = acqNodeList.get(i);
                        LockAcquisition b = acqNodeList.get(j);
                        int d = computeCallDistance(a.method, b.method);
                        if (d >= 0 && d <= 5) {   // 限制距离，避免边爆炸
                            boolean sameMethod = a.method.equals(b.method);
                            boolean nested = a.nestingDepth < b.nestingDepth;
                            writer.write(String.format(
                                    "CALL_EDGE %d,%d,%d,%b,%b\n",
                                    i, j, d, sameMethod, nested));
                        }
                    }
                }
                writer.write("\n");

                // ---------- 跨层边 ----------
                writer.write("# Cross-layer edges\n");
                for (int i = 0; i < acqNodeList.size(); i++) {
                    LockAcquisition la = acqNodeList.get(i);
                    for (CSObj o : la.lockVar.getPointsToSet()) {
                        Integer lockId = lockNodeId.get(objKey(o));
                        if (lockId != null) {
                            writer.write(String.format("ACQ_TO_LOCK %d,%d\n", i, lockId));
                            writer.write(String.format("LOCK_TO_ACQ %d,%d\n", lockId, i));
                        }
                    }
                }
                writer.write("\n");

                // ---------- 标注 ----------
                //writer.write("LABEL_CYCLE 1\n");   // 这个环是死锁候选
                CycleFeatures feat = classifyCycle(cycle);

                writer.write("LABEL_CYCLE " + (feat.isReal ? 1 : 0) + "\n");
                writer.write(String.format(
                        "CYCLE_FEATURES thread_count=%d max_thread_occurrence=%d "
                                + "all_edges_same_thread=%b all_edges_reachable=%b "
                                + "has_non_adjacent_edge=%b edge_count=%d\n",
                        feat.threadCount, feat.maxThreadOccurrence,
                        feat.allEdgesSameThread, feat.allEdgesReachable,
                        feat.hasNonAdjacentEdge, feat.edgeCount));
                // 环上的锁对象节点标 1
                StringBuilder nodeLabels = new StringBuilder();
                for (int i = 0; i < lockNodeObj.size(); i++)
                    nodeLabels.append(i).append(",");
                writer.write("LABEL_LOCK_NODES " + nodeLabels + "\n");
                // 环上的锁边标 1
                StringBuilder edgeLabels = new StringBuilder();
                for (int i = 0; i < cycle.size(); i++)
                    edgeLabels.append(i).append(",");
                writer.write("LABEL_LOCK_EDGES " + edgeLabels + "\n");
                writer.write("LABEL_ACQ_NODES ");
                for (int i = 0; i < acqNodeList.size(); i++)
                    writer.write(i + ",");
                writer.write("\n");

                writer.write("\n");
            }

            System.out.println("Graph data written to: " + fileName);
            System.out.println("Total cycles: " + cycleId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** 获锁点唯一 key */
    private String acqKey(LockAcquisition la) {
        if (la == null) return "?";
        String m = la.method.getMethod().toString();
        int line = la.stmt != null ? la.stmt.getLineNumber() : -1;
        String v = la.lockVar != null ? la.lockVar.getVar().toString() : "?";
        return m + "@" + line + "#" + v + "#" + la.isEnter;
    }

    private String nodeKey(LockAcquisition la, CSObj obj) {
        // 节点只按锁对象区分，不再带 method/line
        return obj.getObject().toString();
    }

    private boolean hasNonAdjacentEdge(List<LockOrderEdge> cycle) {
        for (LockOrderEdge e : cycle) {
            if (e.fromLa == null || e.toLa == null) continue;
            // 如果 fromLa 和 toLa 之间，在调用链上还存在别的 LockAcquisition
            // 说明这条边"跳过"了中间锁
            if (hasIntermediateLock(e.fromLa, e.toLa)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasIntermediateLock(LockAcquisition from, LockAcquisition to) {
        // 用 computeCallDistance + 遍历 from 到 to 的路径
        // 简化版：只看 from 到 to 的调用距离是否 > 1，且中间经过的获锁点数 > 0
        // 精确版需要 BFS 记录路径
        List<CSMethod> path = shortestPath(from.method, to.method);
        if (path == null || path.size() <= 2) return false;

        // 路径中间的方法里，是否有 LockAcquisition
        for (int i = 1; i < path.size() - 1; i++) {
            CSMethod mid = path.get(i);
            for (LockAcquisition la : lockAcquisitions) {
                if (la.method.equals(mid)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** BFS 找 from 到 to 的最短调用路径，返回方法序列（含两端），无路径返回 null */
    private List<CSMethod> shortestPath(CSMethod from, CSMethod to) {
        if (from.equals(to)) return List.of(from);

        Queue<CSMethod> queue = new ArrayDeque<>();
        Map<CSMethod, CSMethod> parent = new HashMap<>();
        Set<CSMethod> visited = new HashSet<>();
        queue.add(from);
        visited.add(from);

        while (!queue.isEmpty()) {
            CSMethod cur = queue.poll();
            for (CSCallSite cs : solver.getCallGraph().getCallSitesIn(cur)) {
                for (CSMethod succ : solver.getCallGraph().getCalleesOf(cs)) {
                    if (!visited.add(succ)) continue;
                    parent.put(succ, cur);
                    if (succ.equals(to)) {
                        // 回溯路径
                        LinkedList<CSMethod> path = new LinkedList<>();
                        CSMethod p = to;
                        while (p != null) {
                            path.addFirst(p);
                            p = parent.get(p);
                        }
                        return path;
                    }
                    queue.add(succ);
                }
            }
        }
        return null;
    }

    private CycleFeatures classifyCycle(List<LockOrderEdge> cycle) {
        CycleFeatures f = new CycleFeatures();
        f.edgeCount = cycle.size();

        // 1) 收集每条边的线程归属
        List<String> edgeThreads = new ArrayList<>();
        boolean allSame = true;
        boolean allReachable = true;

        for (LockOrderEdge e : cycle) {
            String fromThread = e.fromLa != null
                    ? threadIdOf(e.fromLa.method) : "?";
            String toThread = e.toLa != null
                    ? threadIdOf(e.toLa.method) : "?";

            // 边的两端必须同线程
            if (!fromThread.equals(toThread)) {
                allSame = false;
            }
            edgeThreads.add(fromThread);

            // 边的两端在线程内必须可达
            if (e.fromLa == null || e.toLa == null
                    || computeCallDistance(e.fromLa.method, e.toLa.method) < 0) {
                allReachable = false;
            }
        }

        // 2) 统计线程出现次数
        Map<String, Integer> threadCount = new HashMap<>();
        for (String t : edgeThreads) {
            threadCount.merge(t, 1, Integer::sum);
        }
        f.threadCount = threadCount.size();
        f.maxThreadOccurrence = threadCount.values().stream()
                .mapToInt(Integer::intValue).max().orElse(0);
        f.allEdgesSameThread = allSame;
        f.allEdgesReachable = allReachable;

        // 3) 是否有"跳过中间锁"的边
        f.hasNonAdjacentEdge = hasNonAdjacentEdge(cycle);

        // 4) 最终判定
        f.isReal = allSame                  // 每条边同线程
                && allReachable             // 每条边线程内可达
                && f.threadCount >= 2       // 至少两个线程
                && f.maxThreadOccurrence <= 1; // 每个线程最多出现一次

        return f;
    }

    /*
    // 从方法沿调用图向上找最近的 run()，返回其所属类名作为线程 ID
    private String threadIdOf(CSMethod m) {
        // BFS 向上找 caller，直到遇到名为 run 的方法
        Queue<CSMethod> queue = new ArrayDeque<>();
        Set<CSMethod> visited = new HashSet<>();
        queue.add(m);
        visited.add(m);

        while (!queue.isEmpty()) {
            CSMethod cur = queue.poll();
            JMethod jm = cur.getMethod();
            if (jm.getName().equals("run") && jm.getParamCount() == 0) {
                return jm.getDeclaringClass().getName();
            }
            for (CSCallSite cs : solver.getCallGraph().getCallersOf(cur)) {
                CSMethod caller = solver.getCallGraph().getContainerOf(cs);
                if (caller != null && visited.add(caller)) {
                    queue.add(caller);
                }
            }
        }
        // 找不到 run()，退化为"类名.方法名前缀"
        return m.getMethod().getDeclaringClass().getName();
    }
    */
    // ============================================================
    // 线程身份判定
    // ============================================================

    /**
     * 返回一个 CSMethod 所属的"线程身份"。
     * - 普通 Runnable.run 链路：向上找 run()，再找承载它的 Thread 对象
     * - lambda 方法（lambda$xxx$N）：从 invokedynamic 溯源，找承载它的 Thread 对象
     * - 兜底：用声明类名
     */
    /**
     * 返回一个 CSMethod 所属的"线程身份"。
     * 沿 caller 向上 BFS：
     * - 遇到 run() → Thread.start 溯源
     * - 遇到 lambda$xxx → invokedynamic 溯源
     * - 都找不到 → 声明类名
     */
    private String threadIdOf(CSMethod m) {
        Queue<CSMethod> queue = new ArrayDeque<>();
        Set<CSMethod> visited = new HashSet<>();
        queue.add(m);
        visited.add(m);

        while (!queue.isEmpty()) {
            CSMethod cur = queue.poll();
            JMethod jm = cur.getMethod();

            // 1) run()：走 Thread.start 溯源
            if (jm.getName().equals("run") && jm.getParamCount() == 0) {
                String tid = findThreadInstanceId(cur);
                if (tid != null) return tid;
                return jm.getDeclaringClass().getName();
            }

            // 2) lambda$：走 invokedynamic 溯源
            if (jm.getName().startsWith("lambda$")) {
                String tid = findThreadForLambda(jm);
                if (tid != null) return tid;
                // 失败则继续向上找 caller
                // 兜底：用 lambda 方法名区分
                return jm.getName();
            }

            // 3) 继续向上
            for (CSCallSite cs : solver.getCallGraph().getCallersOf(cur)) {
                CSMethod caller = solver.getCallGraph().getContainerOf(cs);
                if (caller != null && visited.add(caller)) {
                    queue.add(caller);
                }
            }
        }

        return m.getMethod().getDeclaringClass().getName();
    }

    /**
     * 普通 Runnable 链路：
     * 从 m 沿 caller BFS 向上找名为 run() 的方法，
     * 再从 run() 向上找 Thread.start() 的接收者对象，作为线程 ID。
     * 找不到时退化为声明类名。
     */
    private String findThreadByRunChain(CSMethod m) {
        Queue<CSMethod> queue = new ArrayDeque<>();
        Set<CSMethod> visited = new HashSet<>();
        queue.add(m);
        visited.add(m);

        while (!queue.isEmpty()) {
            CSMethod cur = queue.poll();
            JMethod jm = cur.getMethod();

            if (jm.getName().equals("run") && jm.getParamCount() == 0) {
                String tid = findThreadInstanceId(cur);
                if (tid != null) return tid;
                return jm.getDeclaringClass().getName();
            }

            for (CSCallSite cs : solver.getCallGraph().getCallersOf(cur)) {
                CSMethod caller = solver.getCallGraph().getContainerOf(cs);
                if (caller != null && visited.add(caller)) {
                    queue.add(caller);
                }
            }
        }
        return m.getMethod().getDeclaringClass().getName();
    }

    /**
     * 从 run() 的 CSMethod 向上找 Thread.start() 的接收者对象，返回其身份字符串。
     * 找不到时返回 null。
     */
    private String findThreadInstanceId(CSMethod runMethod) {
        Queue<CSMethod> queue = new ArrayDeque<>();
        Set<CSMethod> visited = new HashSet<>();
        queue.add(runMethod);
        visited.add(runMethod);

        while (!queue.isEmpty()) {
            CSMethod cur = queue.poll();
            JMethod jm = cur.getMethod();
            if (jm.getIR() == null) continue;

            // 看这个方法里有没有对某个 Thread 对象调用 start()
            for (Stmt stmt : jm.getIR().getStmts()) {
                if (!(stmt instanceof Invoke invoke)) continue;
                InvokeExp exp = invoke.getInvokeExp();
                if (!(exp instanceof InvokeInstanceExp inst)) continue;
                JMethod callee = exp.getMethodRef().resolve();
                if (callee == null) continue;
                if (!callee.getDeclaringClass().getName().equals("java.lang.Thread")) continue;
                if (!callee.getName().equals("start")) continue;

                // 拿到接收者 thread 对象的 CSObj
                CSVar baseVar = solver.getCSManager().getCSVar(
                        cur.getContext(), inst.getBase());
                for (CSObj threadObj : solver.getPointsToSetOf(baseVar)) {
                    return threadObj.getObject().toString();
                }
            }

            for (CSCallSite cs : solver.getCallGraph().getCallersOf(cur)) {
                CSMethod caller = solver.getCallGraph().getContainerOf(cs);
                if (caller != null && visited.add(caller)) {
                    queue.add(caller);
                }
            }
        }
        return null;
    }

    /**
     * lambda 溯源：
     * 遍历所有可达方法，找哪个 invokedynamic 引用了这个 lambdaMethod；
     * 再从这个 invokedynamic 的返回值（Runnable 对象）出发，
     * 找它被传给哪个 Thread.<init>，返回那个 Thread 对象的身份。
     */
    private String findThreadForLambda(JMethod lambdaMethod) {
        for (CSMethod csMethod : solver.getCallGraph().reachableMethods().toList()) {
            JMethod m = csMethod.getMethod();
            if (m.getIR() == null) continue;

            for (Stmt stmt : m.getIR().getStmts()) {
                if (!(stmt instanceof Invoke invoke)) continue;
                if (!(invoke.getInvokeExp() instanceof InvokeDynamic dyn)) continue;

                // 找 bootstrap args 里的 MethodHandle
                MethodHandle mh = null;
                for (Literal arg : dyn.getBootstrapArgs()) {
                    if (arg instanceof MethodHandle h) mh = h;
                }
                if (mh == null) continue;

                JMethod impl = mh.getMethodRef().resolve();
                if (impl == null || !impl.equals(lambdaMethod)) continue;

                // 找到了：这个 invokedynamic 的返回值被谁用？
                Var lhs = invoke.getLValue();
                if (lhs == null) continue;

                // 看 lhs 流向哪个 Thread
                String tid = findThreadForRunnableVar(csMethod, lhs);
                if (tid != null) return tid;
            }
        }
        return null;
    }

    /**
     * 在 method 的 IR 里找：runnableVar 被传给哪个 Thread.<init>(Runnable)，
     * 返回那个 Thread 对象的身份。
     */
    private String findThreadForRunnableVar(CSMethod method, Var runnableVar) {
        JMethod jm = method.getMethod();
        if (jm.getIR() == null) return null;

        for (Stmt stmt : jm.getIR().getStmts()) {
            if (!(stmt instanceof Invoke invoke)) continue;
            if (!(invoke.getInvokeExp() instanceof InvokeInstanceExp inst)) continue;

            JMethod callee = invoke.getInvokeExp().getMethodRef().resolve();
            if (callee == null) continue;
            if (!callee.getDeclaringClass().getName().equals("java.lang.Thread")) continue;
            if (!callee.getName().equals("<init>")) continue;

            // Thread.<init>(Runnable)，runnable 是第 0 个参数
            List<Var> args = invoke.getInvokeExp().getArgs();
            if (args.isEmpty()) continue;
            if (!args.get(0).equals(runnableVar)) continue;

            // 找到了：thread 对象是 invoke 的 base
            CSVar baseVar = solver.getCSManager().getCSVar(
                    method.getContext(), inst.getBase());
            for (CSObj threadObj : solver.getPointsToSetOf(baseVar)) {
                return threadObj.getObject().toString();
            }
        }
        return null;
    }

    static class CycleFeatures {
        boolean isReal;              // 最终 0/1
        int threadCount;             // 环涉及几个线程
        int maxThreadOccurrence;     // 一个线程在环里最多出现几次
        boolean allEdgesSameThread;  // 是否所有边同线程
        boolean allEdgesReachable;   // 是否所有边在线程内可达
        boolean hasNonAdjacentEdge;  // 是否有"跳过中间锁"的边
        int edgeCount;               // 边数
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
