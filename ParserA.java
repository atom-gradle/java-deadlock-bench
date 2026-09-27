/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.pta.cs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.graph.callgraph.*;
import pascal.taie.analysis.pta.*;
import pascal.taie.analysis.pta.core.cs.CSCallGraph;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.*;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.analysis.pta.pts.PointsToSetFactory;
import pascal.taie.config.AnalysisOptions;
import pascal.taie.ir.exp.*;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.*;
import pascal.taie.language.classes.*;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.ir.stmt.*;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * dedicated to synchronized, new Thread/Runnable, AnonymousClass
 * Object & customized objects, Lock & ReentrantLock
 */
public class Solver {

    private static final Logger logger = LogManager.getLogger(Solver.class);

    private final AnalysisOptions options;

    private final HeapModel heapModel;

    private final ContextSelector contextSelector;

    private CSManager csManager;

    private CSCallGraph callGraph;

    private PointerFlowGraph pointerFlowGraph;

    private WorkList workList;

    private PointerAnalysisResult result;

    // 每个锁获取点：方法、Monitor 语句、锁变量（CSVar）
    private final List<LockAcquisition> lockAcquisitions = new ArrayList<>();

    // 锁顺序边：A -> B 表示先持有 A 再获取 B
    private final Set<LockOrderEdge> lockOrderEdges = new HashSet<>();

    private final Set<CSObj> threadObjs = new HashSet<>();

    // 每个 CSMethod 入口处，调用者已经持有的锁对象集合（跨方法传播）
    private final Map<CSMethod, Set<HeldLock>> entryLocks = new HashMap<>();

    private final Map<CSVar, Boolean> lockVarIsStatic = new HashMap<>();

    private static final List<String> LOCK_TYPES = List.of(
            "java.util.concurrent.locks.Lock", "java.util.concurrent.locks.ReentrantLock",
            "java.util.concurrent.locks.ReentrantReadWriteLock", "java.util.concurrent.locks.ReadWriteLock");

    private static class LockAcquisition {
        final CSMethod method;
        //final Monitor stmt;
        final Stmt stmt;// to support ReentrantLock
        final CSVar lockVar;
        final boolean isEnter;
        int nestingDepth;

        LockAcquisition(CSMethod method, Stmt stmt, CSVar lockVar, boolean isEnter) {
            this.method = method;
            this.stmt = stmt;
            this.lockVar = lockVar;
            this.isEnter = isEnter;
        }
        @Override public String toString() {
            return method.getMethod() + "@" + stmt.getLineNumber() + " " + lockVar;
        }
    }

    private static class LockOrderEdge {
        final LockAcquisition fromLa;   // 持有者：held
        final LockAcquisition toLa;     // 获取者：current
        final CSObj fromObj;            // 具体的锁对象（held 的 points-to 元素）
        final CSObj toObj;              // 具体的锁对象（la 的 points-to 元素）

        LockOrderEdge(LockAcquisition fromLa, LockAcquisition toLa,
                      CSObj fromObj, CSObj toObj) {
            this.fromLa = fromLa;
            this.toLa = toLa;
            this.fromObj = fromObj;
            this.toObj = toObj;
        }

        private String edgeKey(LockOrderEdge e) {
            return objKey(e.fromObj) + "->" + objKey(e.toObj)
                    + "|" + laKey(e.fromLa) + "->" + laKey(e.toLa);
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

        @Override public boolean equals(Object o) {
            if (!(o instanceof LockOrderEdge e)) return false;
            return edgeKey(this).equals(edgeKey(e));
        }
        @Override public int hashCode() {
            return edgeKey(this).hashCode();
        }
        @Override public String toString() {
            return fromObj + " -> " + toObj + "\n" +
                    fromLa + " -> " + toLa + "\n";
        }
    }

    Solver(AnalysisOptions options, HeapModel heapModel,
           ContextSelector contextSelector) {
        this.options = options;
        this.heapModel = heapModel;
        this.contextSelector = contextSelector;
    }

    void solve() {
        initialize();
        analyze();
    }

    private void initialize() {
        csManager = new MapBasedCSManager();
        callGraph = new CSCallGraph(csManager);
        pointerFlowGraph = new PointerFlowGraph();
        workList = new WorkList();
        // process program entry, i.e., main method
        Context defContext = contextSelector.getEmptyContext();
        JMethod main = World.get().getMainMethod();
        CSMethod csMethod = csManager.getCSMethod(defContext, main);
        callGraph.addEntryMethod(csMethod);
        addReachable(csMethod);

        for (JClass clazz : World.get().getClassHierarchy().allClasses().toList()) {
            if (!clazz.isApplication()) continue;
            for (JMethod m : clazz.getDeclaredMethods()) {
                if (m.getName().equals("<clinit>")) {
                    CSMethod csClinit = csManager.getCSMethod(defContext, m);
                    callGraph.addEntryMethod(csClinit);
                    break;
                }
            }
        }
    }

    /**
     * Processes new reachable context-sensitive method.
     */
    private void addReachable(CSMethod csMethod) {
        // TODO - finish me
        JMethod method = csMethod.getMethod();
        if (!shouldAnalyze(method)) return;
        if(callGraph.addReachableMethod(csMethod)) {
            List<Stmt> stmts = csMethod.getMethod().getIR().getStmts();
            StmtProcessor stmtProcessor = new StmtProcessor(csMethod);
            for(var stmt : stmts)
                stmt.accept(stmtProcessor);
        }
    }

    /**
     * Processes the statements in context-sensitive new reachable methods.
     */
    private class StmtProcessor implements StmtVisitor<Void> {

        private final CSMethod csMethod;

        private final Context context;

        private StmtProcessor(CSMethod csMethod) {
            this.csMethod = csMethod;
            this.context = csMethod.getContext();
        }

        // TODO - if you choose to implement addReachable()
        //  via visitor pattern, then finish me
        // x = new Object()
        @Override
        public Void visit(New stmt) {
            Var lValue = stmt.getLValue();
            CSVar csVar = csManager.getCSVar(context, lValue);
            Obj obj = heapModel.getObj(stmt);
            CSObj csObj = csManager.getCSObj(
                    contextSelector.selectHeapContext(csMethod, obj), obj);

            // 记录 Thread 对象
            Type type = stmt.getRValue().getType();
            if (type instanceof ClassType ct
                    && ct.getName().equals("java.lang.Thread"))
                threadObjs.add(csObj);

            PointsToSet pointsToSet = PointsToSetFactory.make(csObj);
            workList.addEntry(csVar, pointsToSet);
            return null;
        }

        // y = x
        @Override
        public Void visit(Copy stmt) {
            Var lValue = stmt.getLValue();
            CSVar leftCSVar = csManager.getCSVar(context, lValue);
            Var rValue = stmt.getRValue();
            CSVar rightCSVar = csManager.getCSVar(context, rValue);
            addPFGEdge(rightCSVar, leftCSVar);
            return null;
        }

        // handle static methods
        @Override
        public Void visit(Invoke stmt) {
            System.out.println("[StmtProcessor]: INVOKE: " + stmt);
            if(stmt.isStatic()) {
                // dispatch
                JMethod callee = resolveCallee(null, stmt);

                // select：为 callee 选上下文
                CSCallSite csCallSite = csManager.getCSCallSite(context, stmt);
                Context calleeContext = contextSelector.selectContext(csCallSite, callee);
                CSMethod csCallee = csManager.getCSMethod(calleeContext, callee);

                // 加调用图边
                CallKind kind = CallGraphs.getCallKind(stmt);
                if (callGraph.addEdge(new Edge<>(kind, csCallSite, csCallee))) {
                    addReachable(csCallee);

                    // handle params
                    List<Var> args = stmt.getInvokeExp().getArgs();   // 调用点的实参
                    List<Var> params = callee.getIR().getParams();    // 被调用者的形参
                    for (int i = 0; i < args.size(); i++)
                        addPFGEdge(csManager.getCSVar(context, args.get(i)), csManager.getCSVar(calleeContext, params.get(i)));

                    // handle return val, ret -> recvObj
                    if (stmt.getLValue() != null) {
                        List<Var> returnVars = callee.getIR().getReturnVars();
                        for (var returnVal : returnVars)
                            addPFGEdge(csManager.getCSVar(context, returnVal),
                                    csManager.getCSVar(context, stmt.getLValue()));

                    }
                }
            } else if(stmt.isSpecial()) {
                System.out.println("[StmtProcessor]: invokespecial");
                InvokeExp invokeExp = stmt.getInvokeExp();
                if(invokeExp instanceof InvokeSpecial specialExp) {
                    Var lValue = stmt.getLValue();
                    if(lValue != null) {
                        CSVar csVar = csManager.getCSVar(context, lValue);
                        //csVar.setPointsToSet(PointsToSetFactory.make());
                    }
                }
            } else if (stmt.isDynamic()) {
                var dynamicExp = (InvokeDynamic) stmt.getInvokeExp();
                System.out.println("invokedynamic");
                MethodHandle implHandle = null;
                for (Literal arg : dynamicExp.getBootstrapArgs()) {
                    if (arg instanceof MethodHandle mh) implHandle = mh;
                }
                JMethod lambdaMethod = implHandle != null
                        ? implHandle.getMethodRef().resolve() : null;

                System.out.println(implHandle + "  " + lambdaMethod);

                if (lambdaMethod != null) {
                    // 直接让 lambdaMethod 可达
                    CSCallSite csCallSite = csManager.getCSCallSite(context, stmt);
                    Context lambdaContext = contextSelector.selectContext(csCallSite, lambdaMethod);
                    CSMethod csLambda = csManager.getCSMethod(lambdaContext, lambdaMethod);
                    callGraph.addEdge(new Edge<>(CallKind.SPECIAL, csCallSite, csLambda));

                    addReachable(csLambda);

                    // 让lambda方法中的方法也可达
                    for (Stmt statement : lambdaMethod.getIR().getStmts()) {
                        if (statement instanceof Invoke invokeStmt) {
                            JMethod resolve = invokeStmt.getInvokeExp().getMethodRef().resolve();
                            System.out.println("[resolve]: " + resolve.getSignature());
                            CSCallSite csCallSite1 = csManager.getCSCallSite(context, invokeStmt);
                            contextSelector.selectContext(csCallSite1, resolve);
                            CSMethod csMethod1 = csManager.getCSMethod(lambdaContext, lambdaMethod);
                            callGraph.addEdge(new Edge<>(CallKind.VIRTUAL, csCallSite1, csMethod1));
                            addReachable(csMethod1);
                        }
                    }
                }
            } else {
                // instance or virtual
                System.out.println("invoke instance/virtual");
                InvokeExp invokeExp = stmt.getInvokeExp();
                // handle ReentrantLock.lock/unlock
                if (invokeExp instanceof InvokeInstanceExp instanceExp) {
                    int[] nesting = {0};
                    Var base = instanceExp.getBase();
                    if (base != null) {
                        Type t = base.getType();
                        if (t instanceof ClassType ct && LOCK_TYPES.contains(ct.getName())) {
                            JMethod m = invokeExp.getMethodRef().resolve();
                            System.out.println("[handleInvoke]: " + m.getSignature());
                            if (m.getName().equals("lock") || m.getName().equals("unlock")) {
                                System.out.println("[handleInvoke]: handle lock() and unlock()");
                                CSVar csLockVar = csManager.getCSVar(context, base);
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
                var instanceExp = (InvokeInstanceExp) invokeExp;
                JMethod m = stmt.getInvokeExp().getMethodRef().resolve();
                Var base = instanceExp.getBase();
                // 普通 instance/virtual 调用：遍历 base 的 points-to，对每个 recvObj 建边
                if (base != null && m != null) {
                    CSVar csBase = csManager.getCSVar(context, base);

                    // base 的 points-to 里每个 CSObj 都是一个可能的接收者
                    for (CSObj recvObj : csBase.getPointsToSet()) {
                        // 1) 解析 callee：这里用 resolveCallee(recvObj, stmt)
                        JMethod callee = resolveCallee(recvObj, stmt);
                        if (callee == null) continue;
                        if (!shouldAnalyze(callee)) continue;

                        // 2) 建调用点 + 选 callee 上下文
                        CSCallSite csCallSite = csManager.getCSCallSite(context, stmt);
                        Context calleeContext = contextSelector.selectContext(csCallSite, recvObj, callee);
                        CSMethod csCallee = csManager.getCSMethod(calleeContext, callee);

                        // 3) this -> this
                        Var mThis = callee.getIR().getThis();
                        if (mThis != null) {
                            Pointer thisPtr = csManager.getCSVar(calleeContext, mThis);
                            workList.addEntry(thisPtr, PointsToSetFactory.make(recvObj));
                        }

                        // 4) 加调用图边 + addReachable
                        CallKind kind = CallGraphs.getCallKind(stmt);
                        if (callGraph.addEdge(new Edge<>(kind, csCallSite, csCallee))) {
                            addReachable(csCallee);

                            // 5) 实参 -> 形参
                            List<Var> args = stmt.getInvokeExp().getArgs();
                            List<Var> params = callee.getIR().getParams();
                            for (int i = 0; i < args.size() && i < params.size(); i++) {
                                addPFGEdge(csManager.getCSVar(context, args.get(i)),
                                        csManager.getCSVar(calleeContext, params.get(i)));
                            }

                            // 6) 返回值 -> lhs
                            Var lhs = stmt.getLValue();
                            if (lhs != null) {
                                for (Var retVar : callee.getIR().getReturnVars()) {
                                    addPFGEdge(csManager.getCSVar(calleeContext, retVar),
                                            csManager.getCSVar(context, lhs));
                                }
                            }
                        }
                    }
                }
            }
            return null;
        }

        // handle static fields
        // x.f = y
        // premises: y -> o2, x -> o1, conclusion: o1.f -> o2
        @Override
        public Void visit(StoreField stmt) {
            if(stmt.getFieldAccess() instanceof StaticFieldAccess) {
                JField field = stmt.getFieldAccess().getFieldRef().resolve();
                StaticField staticField = csManager.getStaticField(field);
                CSVar varPtr = csManager.getCSVar(context, stmt.getRValue());
                addPFGEdge(varPtr, staticField);
            }
            return null;
        }

        // y = x.f
        // premises: x -> o, x.f -> o.f, conclusion: y -> o.f
        @Override
        public Void visit(LoadField stmt) {
            if(stmt.getFieldAccess() instanceof StaticFieldAccess) {
                JField field = stmt.getFieldAccess().getFieldRef().resolve();
                StaticField staticField = csManager.getStaticField(field);
                CSVar varPtr = csManager.getCSVar(context, stmt.getLValue());
                addPFGEdge(staticField, varPtr);
                lockVarIsStatic.put(varPtr, true);   // 记录
            }
            return null;
        }

        private int currentNestingDepth = 0;

        @Override
        public Void visit(Monitor stmt) {
            if (stmt.isEnter()) {
                Var lockVar = stmt.getObjectRef();
                CSVar csLockVar = csManager.getCSVar(context, lockVar);
                LockAcquisition la = new LockAcquisition(csMethod, stmt, csLockVar, true);
                la.nestingDepth = ++currentNestingDepth;
                lockAcquisitions.add(la);
            } else {
                currentNestingDepth--;
            }
            return null;
        }

    }

    private boolean shouldAnalyze(JMethod callee) {
        System.out.println("[shouldAnalyze]: " + callee.getSignature());
        if (callee == null) return false;
        if (callee.getDeclaringClass().isApplication()) return true;
        // 特殊放行 Thread 的关键方法
        String cls = callee.getDeclaringClass().getName();
        String signature = callee.getSignature();
        if (cls.equals("java.lang.Thread") || cls.contains("java.lang.invoke") || signature.contains("<init>")) {
            String name = callee.getName();
            System.out.println("Method Name: " + name);
            return name.equals("<init>") || name.equals("start") || name.equals("run") || name.equals("start0");
        }
        return false;
    }

    /**
     * Adds an edge "source -> target" to the PFG.
     */
    private void addPFGEdge(Pointer source, Pointer target) {
        // TODO - finish me
        if(pointerFlowGraph.addEdge(source, target)) {
            if(!source.getPointsToSet().isEmpty())
                workList.addEntry(target, source.getPointsToSet());
        }
    }

    /**
     * Processes work-list entries until the work-list is empty.
     */
    private void analyze() {
        // TODO - finish me
        callGraph.entryMethods().forEach(this::addReachable);

        while (!workList.isEmpty()) {
            WorkList.Entry entry = workList.pollEntry();
            Pointer pointer = entry.pointer();
            PointsToSet pts = entry.pointsToSet();
            PointsToSet delta = propagate(pointer, pts);
            if (delta.isEmpty()) continue;

            if (pointer instanceof CSVar csVar) {
                Context context = csVar.getContext();
                Var var = csVar.getVar();
                JMethod method = var.getMethod();
                CSMethod csMethod = csManager.getCSMethod(context, method);
                for (var obj : delta) {
                    // store: x.f = y
                    for (var storeField : var.getStoreFields()) {
                        System.out.println("[analyze]: handle store fields!");
                        JField field = storeField.getFieldAccess().getFieldRef().resolve();
                        System.out.println(field);
                        System.out.println(storeField.getRValue().getName());
                        Pointer fieldPtr = csManager.getInstanceField(obj, field);
                        Pointer yPtr = csManager.getCSVar(context, storeField.getRValue());
                        addPFGEdge(yPtr, fieldPtr);
                    }
                    // load: y = x.f
                    for (var loadField : var.getLoadFields()) {
                        JField field = loadField.getFieldAccess().getFieldRef().resolve();
                        Pointer fieldPtr = csManager.getInstanceField(obj, field);
                        Pointer yPtr = csManager.getCSVar(context, loadField.getLValue());
                        addPFGEdge(fieldPtr, yPtr);
                    }
                    // call
                    processCall(csVar, obj, csMethod);
                }
            }
        }

        // === 指针分析结束 ===
        dumpThreadTarget();
        // 1. 跨方法传播 EntryLocks
        computeEntryLocks();
        // 2. 建锁顺序图
        buildLockOrderGraph();
        dumpLockOrderGraph();
        // 3. 环检测
        detectDeadlock();
        // 4. export data
        exportGraph();
    }

    private void buildLockOrderGraph() {
        // 分组 + 排除 <clinit>
        Map<CSMethod, List<LockAcquisition>> byMethod = groupLockAcquisitions();

        for (Map.Entry<CSMethod, List<LockAcquisition>> e : byMethod.entrySet()) {
            CSMethod m = e.getKey();
            List<Stmt> stmts = m.getMethod().getIR().getStmts();

            // 按 IR 顺序排序
            List<LockAcquisition> events = new ArrayList<>(e.getValue());
            events.sort(Comparator.comparingInt(la -> stmts.indexOf(la.stmt)));

            // ★ 初始栈 = EntryLocks(m)，每个锁的 la 为 null（跨方法继承，无本方法来源）
            Deque<HeldLock> heldStack = new ArrayDeque<>(entryLocks.getOrDefault(m, new HashSet<>()));
            // 不再需要 new HeldLock(o, null)，entryLocks 里已经是 HeldLock

            for (LockAcquisition la : events) {
                if (la.isEnter) {
                    // 当前要获取的锁对象集合
                    Set<CSObj> toObjs = new HashSet<>();
                    la.lockVar.getPointsToSet().forEach(toObjs::add);

                    if (!toObjs.isEmpty()) {
                        // 对栈中每个已持有的锁，建一条 held -> current 的边
                        for (HeldLock held : heldStack) {
                            for (CSObj toObj : toObjs) {
                                LockOrderEdge lockOrderEdge = new LockOrderEdge(
                                        held.la(),      // 持有者来源（可能 null）
                                        la,             // 获取者来源（非 null）
                                        held.obj(),
                                        toObj);
                                System.out.println(lockOrderEdge);
                                lockOrderEdges.add(lockOrderEdge);
                            }
                        }
                    }

                    // 压栈：每个 toObj 都带来源 la
                    for (CSObj toObj : toObjs)
                        heldStack.push(new HeldLock(toObj, la));
                } else {
                    // 出栈：移除该锁对象的栈帧（所有匹配的）
                    Set<CSObj> toRemove = new HashSet<>();
                    la.lockVar.getPointsToSet().forEach(toRemove::add);
                    heldStack.removeIf(h -> toRemove.contains(h.obj()));
                }
            }
        }
    }

    private record HeldLock(CSObj obj, LockAcquisition la) {}

    private void dumpThreadTarget() {
        JField targetField = World.get()
                .getClassHierarchy()
                .getClass("java.lang.Thread")
                .getDeclaredField("target");

        if (targetField == null) {
            System.out.println("[dumpThreadTarget] Thread.target not found");
            return;
        }

        System.out.println("[dumpThreadTarget] Thread objects: " + threadObjs.size());
        for (CSObj threadObj : threadObjs) {
            Pointer fieldPtr = csManager.getInstanceField(threadObj, targetField);
            PointsToSet pts = fieldPtr.getPointsToSet();
            System.out.println("  target of " + threadObj + " -> " + pts);
        }
    }

    private void dumpLockOrderGraph() {
        System.out.println("=== Lock Order Graph ===");
        if (lockOrderEdges.isEmpty()) {
            System.out.println("  (empty)");
            return;
        }
        for (LockOrderEdge edge : lockOrderEdges)
            System.out.println("  " + edge);
    }

    private void detectDeadlock() {
        // 把 lockOrderEdges 转成邻接表：节点用 CSObj 标识
        // 边：from(集合里的每个 CSObj) -> to(集合里的每个 CSObj)
        Map<CSObj, Set<CSObj>> adj = new HashMap<>();
        for (LockOrderEdge e : lockOrderEdges)
            adj.computeIfAbsent(e.fromObj, k -> new HashSet<>()).add(e.toObj);

        // DFS 找环
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
            path.addLast(node);   // 补上环的起点
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

    /**
     * 第一遍：沿调用图迭代传播 EntryLocks。
     * EntryLocks(callee) = EntryLocks(caller) ∪ LocksHeldAt(caller, callSite)
     * 迭代到不动点（处理递归/环）。
     */
    private void computeEntryLocks() {
        // 1) 按 CSMethod 分组锁事件（复用 buildLockOrderGraph 的分组逻辑）
        Map<CSMethod, List<LockAcquisition>> byMethod = groupLockAcquisitions();

        // 2) 初始化：entry methods 入口持锁为空集
        for (CSMethod entry : callGraph.entryMethods().toList())
            entryLocks.computeIfAbsent(entry, k -> new HashSet<>());
        // 其他可达方法也先占位，避免 NPE
        for (CSMethod m : callGraph.reachableMethods().toList())
            entryLocks.computeIfAbsent(m, k -> new HashSet<>());

        // 3) 迭代到不动点
        boolean changed = true;
        int round = 0;
        while (changed) {
            changed = false;
            round++;

            for (CSMethod caller : callGraph.reachableMethods().toList()) {
                Set<HeldLock> callerEntry = entryLocks.get(caller);
                List<LockAcquisition> events = byMethod.getOrDefault(caller, List.of());
//                if (events.isEmpty()) continue;

                Map<Stmt, Set<HeldLock>> heldAtCallSite =
                        computeHeldAtCallSites(caller, events, callerEntry);

                // 用 getCallSitesIn + edgesOutOf 拿到 caller 的所有出边
                for (CSCallSite csCallSite : callGraph.getCallSitesIn(caller)) {
                    System.out.println(csCallSite);
                    Stmt callStmt = csCallSite.getCallSite();   // CSCallSite 拿底层 Invoke
                    Set<HeldLock> heldAt = heldAtCallSite.getOrDefault(callStmt, new HashSet<>());

                    for (CSMethod callee : callGraph.getCalleesOf(csCallSite)) {
                        Set<HeldLock> newEntry = new HashSet<>();
                        newEntry.addAll(callerEntry);
                        newEntry.addAll(heldAt);
                        if(entryLocks.get(callee) == null) continue;
                        if (entryLocks.get(callee).addAll(newEntry))
                            changed = true;
                    }
                }
            }
            logger.info("computeEntryLocks round {} {}", round, changed ? "changed" : "converged");
        }
    }

    /**
     * 在方法 m 内，以 entry 为初始栈，线性模拟 enter/exit，
     * 返回"每个调用语句处持有的锁对象集合"。
     */
    private Map<Stmt, Set<HeldLock>> computeHeldAtCallSites(
            CSMethod m,
            List<LockAcquisition> events,
            Set<HeldLock> entry) {

        List<Stmt> stmts = m.getMethod().getIR().getStmts();

        // 1) 先把锁事件按 IR 顺序排好
        List<LockAcquisition> sorted = new ArrayList<>(events);
        sorted.sort(Comparator.comparingInt(la -> stmts.indexOf(la.stmt)));

        // 2) 把"方法内所有调用语句"也按 IR 顺序排好，作为插桩点
        List<Stmt> callSites = new ArrayList<>();
        for (Stmt s : stmts)
            if (s instanceof Invoke) callSites.add(s);
        callSites.sort(Comparator.comparingInt(stmts::indexOf));

        // 3) 归并遍历：按 IR 顺序，遇到锁事件就改栈，遇到调用点就拍快照
        Map<Stmt, Set<HeldLock>> result = new HashMap<>();
        Deque<HeldLock> stack = new ArrayDeque<>(entry);   // 初始栈来自跨方法

        int i = 0, j = 0;
        while (i < sorted.size() || j < callSites.size()) {
            int lockIdx = i < sorted.size() ? stmts.indexOf(sorted.get(i).stmt) : Integer.MAX_VALUE;
            int callIdx = j < callSites.size() ? stmts.indexOf(callSites.get(j)) : Integer.MAX_VALUE;

            if (lockIdx <= callIdx && i < sorted.size()) {
                LockAcquisition la = sorted.get(i++);
                if (la.isEnter) {
                    // 压栈：把 points-to 里的每个对象都压进去
                    for (CSObj o : la.lockVar.getPointsToSet())
                        stack.push(new HeldLock(o, la));
                } else {
                    // 出栈：移除 points-to 里的对象
                    Set<CSObj> toRemove = new HashSet<>();
                    la.lockVar.getPointsToSet().forEach(toRemove::add);
                    stack.removeIf(toRemove::contains);
                }
            } else if (j < callSites.size()) {
                Stmt cs = callSites.get(j++);
                // 拍快照：调用点处持有的锁对象集合
                Set<HeldLock> snapshot = new HashSet<>(stack);
                result.put(cs, snapshot);
            } else {
                break;
            }
        }
        return result;
    }

    private Map<CSMethod, List<LockAcquisition>> groupLockAcquisitions() {
        Map<CSMethod, List<LockAcquisition>> byMethod = new HashMap<>();
        for (LockAcquisition la : lockAcquisitions) {
            // 排除 <clinit>：单线程执行，不构成死锁
            if (la.method.getMethod().getName().equals("<clinit>")) continue;
            byMethod.computeIfAbsent(la.method, k -> new ArrayList<>()).add(la);
        }
        return byMethod;
    }

    /**
     * Propagates pointsToSet to pt(pointer) and its PFG successors,
     * returns the difference set of pointsToSet and pt(pointer).
     */
    private PointsToSet propagate(Pointer pointer, PointsToSet pointsToSet) {
        // TODO - finish me
        PointsToSet pointsToSet1 = pointer.getPointsToSet();
        PointsToSet delta = PointsToSetFactory.make();
        for(var obj : pointsToSet)
            if(!pointsToSet1.contains(obj))
                delta.addObject(obj);

        pointer.getPointsToSet().addAll(delta);

        for(var succ : pointerFlowGraph.getSuccsOf(pointer))
            workList.addEntry(succ, delta);
        return delta;
    }

    /**
     * Processes instance calls when points-to set of the receiver variable changes.
     *
     * @param recv    the receiver variable
     * @param recvObj set of new discovered objects pointed by the variable.
     */
    private void processCall(CSVar recv, CSObj recvObj, CSMethod csMethod) {
        // TODO - finish me
        System.out.println("call method: " + csMethod.getMethod().getSignature());

        Context recvContext = recv.getContext();
        for (var callSite : recv.getVar().getInvokes()) {
            JMethod callee = resolveCallee(recvObj, callSite);
            if (callee == null) continue;
            if (!shouldAnalyze(callee)) continue;

            if (callee.getDeclaringClass().getName().equals("java.lang.Thread")
                    && callee.getName().equals("<init>")) {
                System.out.println("init------");
                // 找到 target 字段
                JField targetField = World.get().getClassHierarchy()
                        .getClass("java.lang.Thread").getDeclaredField("target");
                // this.target 字段指针
                Pointer targetPtr = csManager.getInstanceField(recvObj, targetField);
                // Thread.<init>(Runnable) 的第一个参数就是 target
                List<Var> args = callSite.getInvokeExp().getArgs();
                if (!args.isEmpty()) {
                    // 注意：这里 args 是调用点上下文里的实参
                    CSVar argVar = csManager.getCSVar(recvContext, args.get(0));
                    // this.target = runnable
                    addPFGEdge(argVar, targetPtr);
                }
            }

            if (callee.getDeclaringClass().getName().equals("java.lang.Thread")
                    && callee.getName().equals("start")) {
                System.out.println("start-----");
                // Thread.target 字段
                JField targetField = World.get().getClassHierarchy()
                        .getClass("java.lang.Thread").getDeclaredField("target");
                System.out.println("Thread.target: " + targetField);
                // recvObj 是 Thread 对象，拿到它的 target 字段
                Pointer targetPtr = csManager.getInstanceField(recvObj, targetField);
                System.out.println(targetPtr);
                System.out.println(targetPtr.getType());
                System.out.println("[start] targetPtr pts = " + targetPtr.getPointsToSet());

                // 遍历 target 的 points-to，每个都是一个 Runnable
                for (CSObj targetObj : targetPtr.getPointsToSet()) {
                    Obj obj = targetObj.getObject();
                    //JMethod targetRun = lambdaImplMethods.get(obj);   // 直接查 map
                    System.out.println("=== target obj ===");
                    System.out.println("  class: " + obj.getClass().getName());
                    System.out.println("  toString: " + obj);
                    System.out.println("  type: " + obj.getType());

                    // targetObj 是 CSObj，其类型是 Runnable
                    JMethod threadRun = World.get().getClassHierarchy()
                            .getClass("java.lang.Thread")
                            .getDeclaredMethod("run");
                    MethodRef runRef = threadRun.getRef();

                    // targetObj 的实际类型（RunnableA / RunnableB）
                    Type type = targetObj.getObject().getType();

                    // 在 type 的类层次里解析 run()
                    //JMethod targetRun = World.get().getClassHierarchy()
                            //.resolveMethod(runRef);   // ← 看下面说明
                    /*
                    JMethod targetRun = World.get().getClassHierarchy()
                            .dispatch(type, runRef);   // 或 resolveMethod + 类型过滤


                     */
                    JMethod targetRun = World.get().getClassHierarchy()
                            .dispatch(type, runRef);   // 根据 type 解析实际的 run()
                    if (targetRun == null) continue;

                    // 建 start() -> targetRun 的调用边
                    CSCallSite csCallSite = csManager.getCSCallSite(recvContext, callSite);
                    Context calleeContext = contextSelector.selectContext(csCallSite, targetObj, targetRun);
                    CSMethod csCallee = csManager.getCSMethod(calleeContext, targetRun);
                    if (callGraph.addEdge(new Edge<>(CallKind.VIRTUAL, csCallSite, csCallee)))
                        addReachable(csCallee);
                }
            }

            CSCallSite csCallSite = csManager.getCSCallSite(recvContext, callSite);
            Context calleeContext = contextSelector.selectContext(csCallSite, recvObj, callee);
            CSMethod csCallee = csManager.getCSMethod(calleeContext, callee);

            // this → this
            Var mThis = callee.getIR().getThis();
            System.out.println("this: " + mThis.getName());
            Pointer thisPtr = csManager.getCSVar(calleeContext, mThis);
            workList.addEntry(thisPtr, PointsToSetFactory.make(recvObj));

            // 调用图边 + addReachable
            CallKind kind = CallGraphs.getCallKind(callSite);
            if (callGraph.addEdge(new Edge<>(kind, csCallSite, csCallee))) {
                addReachable(csCallee);

                // 实参 → 形参
                List<Var> args = callSite.getInvokeExp().getArgs();
                List<Var> params = callee.getIR().getParams();
                for (int i = 0; i < args.size(); i++)
                    addPFGEdge(csManager.getCSVar(recvContext, args.get(i)),
                            csManager.getCSVar(calleeContext, params.get(i)));

                // 返回值 → lhs
                Var lhs = callSite.getLValue();
                if(lhs == null) continue;
                for (Var retVar : callee.getIR().getReturnVars())
                    addPFGEdge(csManager.getCSVar(calleeContext, retVar),
                            csManager.getCSVar(recvContext, lhs));
            }
        }
    }

    /*
    private void exportGraph() {
        String fileName = "graph-" + System.currentTimeMillis() + ".txt";
        try (FileWriter writer = new FileWriter(fileName)) {
            // 1) find out cycles from lockOrderEdges
            List<List<LockOrderEdge>> cycles = findAllCycles();

            int cycleId = 0;
            for (List<LockOrderEdge> cycle : cycles) {
                writer.write("CYCLE " + cycleId++ + "\n");

                // 2) 收集环上所有不同的锁获取点（节点）
                //    节点 key = (fromLa, fromObj) 或 (toLa, toObj)
                Map<String, Integer> nodeIdMap = new LinkedHashMap<>();
                List<LockAcquisition> nodeLaList = new ArrayList<>();
                List<CSObj> nodeObjList = new ArrayList<>();

                for (LockOrderEdge e : cycle) {
                    String fromKey = nodeKey(e.fromLa, e.fromObj);
                    if (!nodeIdMap.containsKey(fromKey)) {
                        nodeIdMap.put(fromKey, nodeIdMap.size());
                        nodeLaList.add(e.fromLa);
                        nodeObjList.add(e.fromObj);
                    }
                    String toKey = nodeKey(e.toLa, e.toObj);
                    if (!nodeIdMap.containsKey(toKey)) {
                        nodeIdMap.put(toKey, nodeIdMap.size());
                        nodeLaList.add(e.toLa);
                        nodeObjList.add(e.toObj);
                    }
                }

                // 3) 输出节点
                for (int i = 0; i < nodeLaList.size(); i++) {
                    LockAcquisition la = nodeLaList.get(i);
                    CSObj obj = nodeObjList.get(i);
                    if (la == null) {
                        // 跨方法继承来的锁，没有本方法来源，但 obj 有效
                        writer.write(String.format(
                                "NODE %d,%s,%s,%s,%d,%b,%b,%d,%d\n",
                                i,
                                "?",
                                obj.getObject().toString(),
                                "?",
                                -1,
                                isStaticLock(obj),
                                false,
                                0,
                                obj.getObject() != null ? 1 : 0
                        ));
                        continue;
                    }
                    String var = la.lockVar != null ? la.lockVar.getVar().toString() : "?";
                    String objStr = obj.getObject().toString();
                    String method = la.method.getMethod().getDeclaringClass().getSimpleName()
                            + "." + la.method.getMethod().getName();
                    int line = la.stmt != null ? la.stmt.getLineNumber() : -1;
                    boolean isStatic = isStaticLock(obj);
                    boolean isSync = Modifier.hasSynchronized(
                            la.method.getMethod().getModifiers());
                    int nesting = la.nestingDepth;
                    int ptsSize = la.lockVar != null ? la.lockVar.getPointsToSet().size() : 1;

                    writer.write(String.format(
                            "NODE %d,%s,%s,%b,%d\n",
                            i, var, objStr,
                            isStatic, ptsSize
                    ));
                }

                // 4) 输出边
                for (LockOrderEdge e : cycle) {
                    int fromId = nodeIdMap.get(nodeKey(e.fromLa, e.fromObj));
                    int toId = nodeIdMap.get(nodeKey(e.toLa, e.toObj));

                    String fromMethod = e.fromLa != null
                            ? e.fromLa.method.getMethod().getDeclaringClass().getSimpleName()
                            + "." + e.fromLa.method.getMethod().getName()
                            : "?";
                    int fromLine = e.fromLa != null && e.fromLa.stmt != null
                            ? e.fromLa.stmt.getLineNumber() : -1;
                    String toMethod = e.toLa != null
                            ? e.toLa.method.getMethod().getDeclaringClass().getSimpleName()
                            + "." + e.toLa.method.getMethod().getName()
                            : "?";
                    int toLine = e.toLa != null && e.toLa.stmt != null
                            ? e.toLa.stmt.getLineNumber() : -1;

                    String fromType = e.fromObj.getObject().getType().toString();
                    String toType = e.toObj.getObject().getType().toString();
                    boolean sameClass = fromType.equals(toType);

                    int callDistance = -1;
                    if (e.fromLa != null && e.toLa != null)
                        callDistance = computeCallDistance(e.fromLa.method, e.toLa.method);

                    // ===== 新增：may_alias =====
                    // 两个锁变量的 points-to 集合是否有交集
                    boolean mayAlias = false;
                    if (e.fromLa != null && e.toLa != null
                            && e.fromLa.lockVar != null && e.toLa.lockVar != null) {
                        Set<CSObj> fromPts = new HashSet<>();
                        e.fromLa.lockVar.getPointsToSet().forEach(fromPts::add);
                        for (CSObj o : e.toLa.lockVar.getPointsToSet()) {
                            if (fromPts.contains(o)) {
                                mayAlias = true;
                                break;
                            }
                        }
                    }

                    // ===== 新增：same_lock_obj =====
                    // 这条边两端的锁对象是否就是同一个 CSObj
                    boolean sameLockObj = e.fromObj.equals(e.toObj);

                    writer.write(String.format(
                            "EDGE %d,%d,%s,%d,%s,%d,%d,%b,%b\n",
                            fromId, toId,
                            fromMethod, fromLine,
                            toMethod, toLine,
                            callDistance,
                            mayAlias, sameLockObj));
                }

                // 5) 标签：先写 0，人工标注
                writer.write("LABEL 0\n");
                writer.write("\n");
            }

            System.out.println("Graph data written to: " + fileName);
            System.out.println("Total cycles: " + cycleId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

     */

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
            for (CSCallSite cs : callGraph.getCallSitesIn(cur)) {
                for (CSMethod succ : callGraph.getCalleesOf(cs)) {
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

    /** 从方法沿调用图向上找最近的 run()，返回其所属类名作为线程 ID */
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
            for (CSCallSite cs : callGraph.getCallersOf(cur)) {
                CSMethod caller = callGraph.getContainerOf(cs);
                if (caller != null && visited.add(caller)) {
                    queue.add(caller);
                }
            }
        }
        // 找不到 run()，退化为"类名.方法名前缀"
        return m.getMethod().getDeclaringClass().getName();
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
        // 1) 建邻接表：节点 = CSObj，边 = LockOrderEdge
        Map<CSObj, List<LockOrderEdge>> adj = new HashMap<>();
        for (LockOrderEdge e : lockOrderEdges) {
            adj.computeIfAbsent(e.fromObj, k -> new ArrayList<>()).add(e);
        }

        // 用 Map 去重：key = 锁对象序列，value = 第一次遇到的环
        Map<String, List<LockOrderEdge>> uniqueCycles = new LinkedHashMap<>();

        // 2) 对每个节点都作为起点单独 DFS
        for (CSObj start : adj.keySet()) {
            Deque<LockOrderEdge> path = new ArrayDeque<>();
            Set<CSObj> inStack = new HashSet<>();
            dfsFindCyclesFromStart(start, start, adj, inStack, path, uniqueCycles);
        }

        return new ArrayList<>(uniqueCycles.values());
    }

    /**
     * 从 start 出发，在当前 DFS 栈上寻找回到 start 的简单环。
     *
     * @param start      本次 DFS 的起点，只有回到这个点才算一个环
     * @param cur        当前节点
     * @param adj        邻接表
     * @param inStack    当前递归栈上的节点集合
     * @param path       当前递归栈上的边（按顺序）
     * @param outCycles  收集到的环（已规范化去重）
     */
    private void dfsFindCyclesFromStart(CSObj start,
                                        CSObj cur,
                                        Map<CSObj, List<LockOrderEdge>> adj,
                                        Set<CSObj> inStack,
                                        Deque<LockOrderEdge> path,
                                        Map<String, List<LockOrderEdge>> outCycles) {

        inStack.add(cur);

        List<LockOrderEdge> outEdges = adj.getOrDefault(cur, Collections.emptyList());
        for (LockOrderEdge e : outEdges) {
            CSObj next = e.toObj;

            if (next.equals(start)) {
                // 找到一个回到起点的环：path + e
                List<LockOrderEdge> cycle = new ArrayList<>(path);
                cycle.add(e);
                List<LockOrderEdge> normalized = normalizeCycle(cycle);
                String key = cycleObjKey(normalized);
                outCycles.putIfAbsent(key, normalized);
            } else if (!inStack.contains(next)) {
                path.addLast(e);
                dfsFindCyclesFromStart(start, next, adj, inStack, path, outCycles);
                path.removeLast();
            }
        }

        inStack.remove(cur);
    }

    /**
     * 用锁对象序列作为环的语义 key，用于按“锁对象环”去重。
     * 形如 "L3->L4->L5->L3"。
     */
    private String cycleObjKey(List<LockOrderEdge> cycle) {
        if (cycle == null || cycle.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (LockOrderEdge e : cycle) {
            sb.append(objKey(e.fromObj)).append("->");
        }
        // 最后补上第一条边的 fromObj，形成闭环
        sb.append(objKey(cycle.get(0).fromObj));
        return sb.toString();
    }

    private String objKey(CSObj obj) {
        return obj == null || obj.getObject() == null ? "?" : obj.getObject().toString();
    }

    /**
     * 把一个环旋转到“最小边”开头，用于 Set 去重。
     * 这里假设 LockOrderEdge 已经正确实现了 equals/hashCode。
     */
    private List<LockOrderEdge> normalizeCycle(List<LockOrderEdge> cycle) {
        if (cycle == null || cycle.isEmpty()) return cycle;

        int n = cycle.size();
        int bestIdx = 0;
        for (int i = 1; i < n; i++) {
            if (compareEdge(cycle.get(i), cycle.get(bestIdx)) < 0) {
                bestIdx = i;
            }
        }

        List<LockOrderEdge> normalized = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            normalized.add(cycle.get((bestIdx + i) % n));
        }
        return normalized;
    }

    /**
     * 边之间的比较，用于找“最小边”。
     * 先比 fromObj，再比 toObj，再比 fromLa/toLa 的方法+行号，保证稳定。
     */
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
            if (la.lockVar.getPointsToSet().contains(obj))
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

            // 正确：从当前方法拿调用点，再从调用点拿出边
            for (CSCallSite csCallSite : callGraph.getCallSitesIn(cur)) {
                for (Edge<CSCallSite, CSMethod> e : callGraph.edgesOutOf(csCallSite).toList()) {
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

    /**
     * Resolves the callee of a call site with the receiver object.
     *
     * @param recv the receiver object of the method call. If the callSite
     *             is static, this parameter is ignored (i.e., can be null).
     * @param callSite the call site to be resolved.
     * @return the resolved callee.
     */
    private JMethod resolveCallee(CSObj recv, Invoke callSite) {
        Type type = recv != null ? recv.getObject().getType() : null;
        return CallGraphs.resolveCallee(type, callSite);
    }

    PointerAnalysisResult getResult() {
        if (result == null) {
            result = new PointerAnalysisResultImpl(csManager, callGraph);
        }
        return result;
    }
}
