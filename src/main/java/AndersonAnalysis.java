import soot.Local;
import soot.Unit;
import soot.jimple.*;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.*;

public class AndersonAnalysis {

    static Map<String, Set<String>> answers = new HashMap<String, Set<String>>();

    private Map resultBeforeUnit;
    private Map resultAfterUnit;

    public AndersonAnalysis(DirectedGraph g){
        Anderson anderson = new Anderson(g);
        resultBeforeUnit = new HashMap();
        resultAfterUnit = new HashMap();

        Iterator unitIt = g.iterator();
        while (unitIt.hasNext()){
            Unit s = (Unit) unitIt.next();

            Map<String, Set<String>> r;
            r = (Map<String, Set<String>>) anderson.getFlowBefore(s);
            resultBeforeUnit.put(s, r.toString());

            r = (Map<String, Set<String>>) anderson.getFlowAfter(s);
            resultAfterUnit.put(s, r.toString());
        }

    }

    public Object getResultBeforeUnit(Unit s){
        return resultBeforeUnit.get(s);
    }

    public Object getResultAfterUnit(Unit s){
        return resultAfterUnit.get(s);
    }

    public void printAnswer()
    {
        String answer = "";
        for (Map.Entry<String, Set<String>> e : answers.entrySet()) {
            answer += e.getKey() + ":";
            for (String i : e.getValue()) {
                answer += " " + i;
            }
            answer += "\n";
        }
        AnswerPrinter.printAnswer(answer);
    }
}




class Anderson extends ForwardFlowAnalysis{
    private int allocId; // 用于记录new语句对应的ID

    public Anderson(DirectedGraph g)
    {
        super(g);
        doAnalysis();
    }

    // 数据流分析的元素是 Map<变量名, Set<对应变量可能指向的位置>>
    @Override
    protected Object newInitialFlow() {
        return new HashMap<String, Set<String>>();
    }
    @Override
    protected Object entryInitialFlow() {
        return new HashMap<String, Set<String>>();
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



    @Override
    protected void flowThrough(Object _in, Object _node, Object _out)
    {
        Map<String, Set<String>> in, out;
        in = (Map<String, Set<String>>) _in;
        out = (Map<String, Set<String>>) _out;
        Unit u = (Unit) _node;

        if (u instanceof InvokeStmt) {
            InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
            if (ie.getMethod().toString().contains("Benchmark: void alloc")) {
                // 记录得到的new语句ID
                allocId = ((IntConstant) ie.getArgs().get(0)).value;
            }
            if (ie.getMethod().toString().contains("Benchmark: void test")) {
                int targetId = ((IntConstant) ie.getArgs().get(0)).value;
                String targetName = ((Local)ie.getArgs().get(1)).getName();
                AndersonAnalysis.answers.put(Integer.toString(targetId), new HashSet<String>(in.get(targetName)));
            }
        }

        // 先把in复制到out，再做操作
        copy(in, out);

        if (u instanceof DefinitionStmt) {
            DefinitionStmt du = (DefinitionStmt) u;

            String leftName;
            if (du.getLeftOp() instanceof Local) {
                leftName = ((Local) du.getLeftOp()).getName();

                // 从集合中删去左侧变量
                out.remove(leftName);

                if (du.getRightOp() instanceof NewExpr) {
                    // 若是new语句，添加 (leftOp, { new ID })
                    //TODO check constructor's parameter
                    Set<String> ts = new HashSet<String>();
                    ts.add(Integer.toString(allocId));
                    out.put(leftName, ts);
                    allocId = 0; // 为了让没有标注的new分配ID为0,此处用完就给它置0
                }

                if (du.getRightOp() instanceof Local) {
                    // 若是赋值语句，则用右侧变量对应的集合替换左侧变量对应的集合
                    String rightName = ((Local) du.getRightOp()).getName();
                    out.put(leftName, new HashSet<String>(in.get(rightName)));
                }
            }

            // FIXME
        }

    }
}
