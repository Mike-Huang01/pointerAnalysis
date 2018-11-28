import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.spark.ondemand.genericutil.Stack;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.*;

public class AndersonAnalysis extends ForwardFlowAnalysis
{
    // 打印答案用代码
    static Map<String, Set<String>> answers = new HashMap<String, Set<String>>();
    static Set<String> allAllocID = new HashSet<String>();
    static public String getAnswer(boolean fallback)
    {
        Comparator<String> cmp = new Comparator<String>()
        {
            @Override
            public int compare(String o1, String o2)
            {
                if (o1.matches("^\\d+$") && o2.matches("^\\d+$")) {
                    return new Integer(o1).compareTo(new Integer(o2));
                } else {
                    return o1.compareTo(o2);
                }
            }
        };

        StringBuilder answer = new StringBuilder();
        List<String> keys = new ArrayList<String>(answers.keySet());
        Collections.sort(keys, cmp);
        for (String key: keys) {
            answer.append(key + ":");

            Set<String> valSet = answers.get(key);

            if (fallback) {
                valSet = new HashSet<String>(valSet);
                if (valSet.contains("#unk")) {
                    valSet.remove("#unk");
                    valSet.addAll(allAllocID);
                }
                valSet.remove("#null");
            }

            List<String> vals = new ArrayList<String>(valSet);
            Collections.sort(vals, cmp);
            for (String val : vals) {
                answer.append(" " + val);
            }
            answer.append("\n");
        }
        return answer.toString();
    }

    ////////


    public void printDetails()
    {
        Map<String, Set<String>> r;
        Iterator unitIt = curGraph.iterator();
        while (unitIt.hasNext()){
            System.out.println(curPrefix + " -------------");

            Unit s = (Unit) unitIt.next();
            System.out.println(curPrefix + " instr  " + s.toString());

            r = (Map<String, Set<String>>) getFlowBefore(s);
            System.out.println(curPrefix + " before " + r.toString());

            r = (Map<String, Set<String>>) getFlowAfter(s);
            System.out.println(curPrefix + " after  " + r.toString());

        }

    }





    private static int allocId; // 用于记录new语句对应的ID
    private static Set<String> andersonStack = new HashSet<String>();; // 记录调用栈上各函数出现次数

    DirectedGraph curGraph;
    String curPrefix;
    Map<String, Set<String>> initSet, returnSet; // 初始flowSet，返回值returnSet

    public AndersonAnalysis(DirectedGraph _curGraph, String _curPrefix, Map<String, Set<String>> _initSet)
    {
        super(_curGraph);
        curGraph = _curGraph;
        curPrefix = _curPrefix;
        initSet = _initSet;
        returnSet = new HashMap<String, Set<String>>();
        doAnalysis();
    }

    static DirectedGraph getGraph(SootMethod m)
    {
        return new ExceptionalUnitGraph(m.retrieveActiveBody());
    }


    static String getMethodUniqueString(SootMethod m)
    {
        return m.toString();
    }
    public static void leaveMethod(SootMethod m)
    {
        andersonStack.remove(getMethodUniqueString(m));
    }
    public static boolean tryEnterMethod(SootMethod m)
    {
        if (andersonStack.contains(getMethodUniqueString(m))) {
            return false;
        }
        andersonStack.add(getMethodUniqueString(m));
        return true;
    }

    // 数据流分析的元素是 Map<变量名, Set<对应变量可能指向的位置>>
    // “变量可能指向的位置”的格式：
    //    一个正整数
    //    #unk 表示不知道
    //    #null 表示null
    // “变量名”的格式：
    //    普通变量名    代表局部变量 如 r1 $r0
    //    #数字.域      代表堆上对象的某个域 如#1.somefield
    //    %this,%数字   代表传入参数
    //    !ret        代表返回值

    @Override
    protected Object newInitialFlow() {
        return new HashMap<String, Set<String>>();
    }
    @Override
    protected Object entryInitialFlow() {
        Map<String, Set<String>> ret = new HashMap<String, Set<String>>();
        copy(initSet, ret);
        return ret;
    }



    @Override
    protected void merge(Object _in1, Object _in2, Object _out)
    {
        Map<String, Set<String>> a, b, c;
        a = (Map<String, Set<String>>) _in1;
        b = (Map<String, Set<String>>) _in2;
        c = (Map<String, Set<String>>) _out;

        // 合并时针对对应变量，做Set上的并集
        // c = a 并 b
        c.clear(); c.putAll(a);
        for (Map.Entry<String, Set<String>> e: b.entrySet()) {
            if (!c.containsKey(e.getKey())) c.put(e.getKey(), new HashSet<String>());
            ((Set<String>) c.get(e.getKey())).addAll(e.getValue());
        }
    }

    @Override
    protected void copy(Object _src, Object _dst)
    {
        Map<String, Set<String>> src, dst;
        src = (Map<String, Set<String>>) _src;
        dst = (Map<String, Set<String>>) _dst;

        // 深拷贝
        dst.clear();
        for (Map.Entry<String, Set<String>> e: src.entrySet()) {
            dst.put(e.getKey(), new HashSet<String>(e.getValue()));
        }
    }

    // 新建一个flowSet，将子程序需要的元素放入
    Map<String, Set<String>> filterInitSetForInvoke(Map<String, Set<String>> src)
    {
        Map<String, Set<String>> ret = new HashMap<String, Set<String>>();
        for (Map.Entry<String, Set<String>> e: src.entrySet()) {
            if (e.getKey().startsWith("#")) {
                ret.put(e.getKey(), new HashSet<String>(e.getValue()));
            }
        }
        return ret;
    }
    // 将调用结果相关元素加入dest，返回函数返回值可能指向的位置的集合
    Set<String> filterInvokeResult(AndersonAnalysis nextAnderson, Map<String, Set<String>> dest)
    {
        Set<String> ret = new HashSet<String>();
        for (Map.Entry<String, Set<String>> e: nextAnderson.returnSet.entrySet()) {
            if (e.getKey().startsWith("#")) {
                // 用子程序中的结果添加/替换原有结果
                dest.put(e.getKey(), new HashSet<String>(e.getValue()));
            }
            if (e.getKey().equals("!ret")) {
                ret = new HashSet<String>(e.getValue());
            }
        }
        return ret;
    }

    Set<String> processInvokeExpr(Unit u, InvokeExpr ie, Map<String, Set<String>> in, Map<String, Set<String>> out)
    {
        SootMethod m = ie.getMethod();

        Set<String> retValSet = null;

        if (!m.getDeclaringClass().isJavaLibraryClass()) {

            if (tryEnterMethod(m)) {
                Map<String, Set<String>> nextInitSet = filterInitSetForInvoke(in);

                // 添加参数
                if (ie instanceof SpecialInvokeExpr) {
                    String baseName = ((Local) ((SpecialInvokeExpr)ie).getBase()).getName();
                    nextInitSet.put("%this", new HashSet<String>(in.get(baseName)));
                }
                List<Value> args = ie.getArgs();
                for (int i = 0; i < args.size(); i++) {
                    Value curArg = args.get(i);
                    if (curArg instanceof Local) {
                        String argName = ((Local)curArg).getName();
                        nextInitSet.put("%" + Integer.toString(i), new HashSet<String>(in.get(argName)));
                    }
                }

                AndersonAnalysis nextAnderson = new AndersonAnalysis(getGraph(m), curPrefix + "/" + m.getDeclaringClass().getName() + "." + m.getName(), nextInitSet);
                nextAnderson.printDetails();

                retValSet = filterInvokeResult(nextAnderson, out);

                leaveMethod(m);

            } else {
                // 若发现函数递归（我们无法处理）
                // 将所有堆上分析结果和返回值置为#unk

                System.out.println(">>> recursion detected, result dropped [" + u.toString() + "]");
                out.clear();
                for (Map.Entry<String, Set<String>> e: in.entrySet()) {
                    if (e.getKey().startsWith("#")) {
                        Set<String> ns = new HashSet<String>();
                        ns.add("#unk");
                        out.put(e.getKey(), ns);
                    } else {
                        out.put(e.getKey(), new HashSet<String>(e.getValue()));
                    }
                }

                retValSet = new HashSet<String>();
                retValSet.add("#unk");

            }
        }

        return retValSet;
    }

    @Override
    protected void flowThrough(Object _in, Object _node, Object _out)
    {
        Map<String, Set<String>> in, out;
        in = (Map<String, Set<String>>) _in;
        out = (Map<String, Set<String>>) _out;
        Unit u = (Unit) _node;

        // 先把in复制到out，再做操作
        copy(in, out);

        // 处理Benchmark相关的InvokeStmt
        if (u instanceof InvokeStmt) {
            InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
            if (ie.getMethod().toString().contains("Benchmark: void alloc")) {
                // 记录得到的new语句ID
                allocId = ((IntConstant) ie.getArgs().get(0)).value;
                allAllocID.add(Integer.toString(allocId));
                return;
            }
            if (ie.getMethod().toString().contains("Benchmark: void test")) {
                int targetId = ((IntConstant) ie.getArgs().get(0)).value;
                Set<String> resultSet = in.get(((Local)ie.getArgs().get(1)).getName());
                // 将答案记录下来
                if (AndersonAnalysis.answers.containsKey(Integer.toString(targetId))) {
                    // 如果已经存在则合并（因为同一test()可能被执行多次）
                    AndersonAnalysis.answers.get(Integer.toString(targetId)).addAll(resultSet);
                } else {
                    AndersonAnalysis.answers.put(Integer.toString(targetId), new TreeSet<String>(resultSet));
                }
                return;
            }
        }

        // 处理各类语句
        if (u instanceof InvokeStmt) { // 处理函数调用

            InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
            processInvokeExpr(u, ie, in, out);

        } else if (u instanceof DefinitionStmt) { // 处理赋值语句

            DefinitionStmt du = (DefinitionStmt) u;
            Value rightOp = du.getRightOp();
            Value leftOp = du.getLeftOp();

            // 处理右侧
            Set<String> rightSet = new HashSet<String>(); rightSet.add("#unk");

            if (rightOp instanceof NewExpr) {
                // 若是new语句，集合应为 { newID }
                rightSet = new HashSet<String>();
                rightSet.add(Integer.toString(allocId));
                allocId = 0; // 为了让没有标注的new分配ID为0,此处用完就给它置0

            } else if (rightOp instanceof Local) {
                // 若是变量，则复制一份该变量可能指向的集合
                String rightName = ((Local) rightOp).getName();
                rightSet = new HashSet<String>(in.get(rightName));

            } else if (rightOp instanceof InstanceFieldRef) {
                InstanceFieldRef fr = (InstanceFieldRef) rightOp;
                String baseName = ((Local) fr.getBase()).getName();
                String fieldName = fr.getField().getName();
                // 若是域，则将base可能指向的对象的.field可能指向的对象并起来
                rightSet = new HashSet<String>();
                Set<String> basePointsTo = in.get(baseName);
                for (String pto: basePointsTo) {
                    String k = "#" + pto + "." + fieldName;
                    if (in.containsKey(k)) {
                        rightSet.addAll(in.get(k));
                    } else {
                        rightSet.add("#unk");
                    }
                }

            } else if (rightOp instanceof ThisRef) {
                if (in.containsKey("%this")) {
                    rightSet = new HashSet<String>(in.get("%this"));
                } else {
                    rightSet.add("#unk");
                }

            } else if (rightOp instanceof ParameterRef) {
                String rightName = "%" + Integer.toString(((ParameterRef)rightOp).getIndex());
                if (in.containsKey(rightName)) {
                    rightSet = new HashSet<String>(in.get(rightName));
                } else {
                    rightSet.add("#unk");
                }

            } else if (rightOp instanceof InvokeExpr) {
                Set<String> retValSet = processInvokeExpr(u, (InvokeExpr)rightOp, in, out);
                if (retValSet != null) rightSet = retValSet;

            } else if (rightOp instanceof NullConstant) {
                rightSet = new HashSet<String>();
                rightSet.add("#null");

            } else {
                System.out.println("!!! " + curPrefix + " RightOp unknown: " + rightOp.getClass().getName() + " [" + u.toString() + "]");
            }

            // 处理左侧
            if (leftOp instanceof Local) {
                String leftName = ((Local) leftOp).getName();
                // 用右侧变量对应的集合替换左侧变量对应的集合
                out.put(leftName, rightSet);

            } else if (leftOp instanceof InstanceFieldRef) {
                InstanceFieldRef fr = (InstanceFieldRef) leftOp;
                String baseName = ((Local) fr.getBase()).getName();
                String fieldName = fr.getField().getName();
                // 先查询 base 可能指向谁
                Set<String> basePointsTo = in.get(baseName);
                for (String pto: basePointsTo) {
                    String k = "#" + pto + "." + fieldName;
                    if (basePointsTo.size() == 1) {
                        // 如果只可能指向一个对象，则直接替换
                        out.put(k, rightSet);
                    } else {
                        // 如果指向多个对象，则合并
                        if (!out.containsKey(k)) out.put(k, new HashSet<String>());
                        out.get(k).addAll(rightSet);
                    }
                }
            } else {
                System.out.println("!!! " + curPrefix + " LeftOp unknown: " + leftOp.getClass().getName() + " [" + u.toString() + "]");
            }

        } else if (u instanceof ReturnVoidStmt || u instanceof ReturnStmt) {
            // 处理return语句
            // 合并所有return对应的分析结果
            Map<String, Set<String>> newReturnSet = new HashMap<String, Set<String>>();
            merge(returnSet, in, newReturnSet);
            returnSet = newReturnSet;

            if (u instanceof ReturnStmt) { // 处理非void函数对应的return语句
                Value retVal = ((ReturnStmt)u).getOp();
                if (retVal instanceof Local) {

                    // 将返回值可能指向的对象加入到"!ret"中
                    if (!returnSet.containsKey("!ret")) returnSet.put("!ret", new HashSet<String>());
                    returnSet.get("!ret").addAll(in.get(((Local)retVal).getName()));
                }
            }
        } else {
            System.out.println("!!! " + curPrefix + " Unit unknown: " + u.getClass().getName() + " [" + u.toString() + "]");
        }

    }
}
