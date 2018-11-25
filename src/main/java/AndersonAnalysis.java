import soot.Local;
import soot.Unit;
import soot.jimple.*;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.*;

public class AndersonAnalysis {
    private Map resultBeforeUnit;
    private Map resultAfterUnit;
    public AndersonAnalysis(DirectedGraph g){
        Anderson anderson = new Anderson(g);
        resultBeforeUnit = new HashMap();
        resultAfterUnit = new HashMap();

        Iterator unitIt = g.iterator();
        while (unitIt.hasNext()){
            Unit s = (Unit) unitIt.next();

            FlowSet set = (FlowSet) anderson.getFlowBefore(s);
            resultBeforeUnit.put(s,
                    Collections.unmodifiableList(set.toList()));

            set = (FlowSet) anderson.getFlowAfter(s);
            resultAfterUnit.put(s,
                    Collections.unmodifiableList(set.toList()));
        }

    }

    public List getResultBeforeUnit(Unit s){
        return (List) resultBeforeUnit.get(s);
    }

    public List getResultAfterUnit(Unit s){
        return (List) resultAfterUnit.get(s);
    }

}

class Anderson extends ForwardFlowAnalysis{
    private FlowSet emptySet;
    private int allocId;

    public Anderson(DirectedGraph g){
        super(g);
        emptySet = new ArraySparseSet();
        doAnalysis();
    }

    @Override
    protected void merge(Object in1, Object in2, Object out){
        FlowSet inSet1 = (FlowSet) in1,
                inSet2 = (FlowSet) in2,
                outSet = (FlowSet) out;

        //TODO the following code is buggy, not really union

        Set<String> namesInset1 = new TreeSet<>();
        Map<String, List<Integer>> mapInset1 = new HashMap<>();
        for (Object obj: inSet1){
            Map<String, List<Integer>> ele = (Map<String, List<Integer>>) obj;
            // map only contains one key
            String name = getOnlyKey(ele);
            namesInset1.add(name);
            mapInset1.put(name, ele.get(name));

        }

        Set<String> namesInSet2 = new TreeSet<>();
        Map<String, List<Integer>> mapInset2 = new HashMap<>();
        for (Object obj: inSet2){
            Map<String, List<Integer>> ele = (Map<String, List<Integer>>) obj;
            // map only contains one key
            String name = getOnlyKey(ele);
            namesInSet2.add(name);
            mapInset2.put(name, ele.get(name));
        }

        //namesInset1.addAll(namesInSet2);
        Set<String> allNames = new TreeSet<>(namesInset1);
        allNames.addAll(namesInSet2);

        for (String name: allNames){
            if (namesInset1.contains(name) && namesInSet2.contains(name)){
                List<Integer> arr = new ArrayList<>(mapInset1.get(name));
                List<Integer> arr2 = mapInset2.get(name);
                arr.addAll(arr2);
                Map<String, List<Integer>> eleOut = new HashMap<>();
                eleOut.put(name, arr);
            }else if(namesInset1.contains(name)){
                outSet.add(mapInset1.get(name));
            }else{
                outSet.add(mapInset2.get(name));
            }
        }
    }

    @Override
    protected void copy(Object source, Object dest){
        FlowSet srcSet = (FlowSet) source,
                destSet = (FlowSet) dest;
        srcSet.copy(destSet);
    }

    @Override
    protected Object newInitialFlow() {
        return emptySet.clone();
    }

    @Override
    protected Object entryInitialFlow() {
        return emptySet.clone();
    }

    @Override
    protected void flowThrough(Object in, Object node, Object out) {
        FlowSet inSet = (FlowSet) in,
                outSet = (FlowSet) out;
        Unit u = (Unit) node;
        // out <- (in - expr containing locals defined in d) union out
        kill(inSet, u, outSet);
        // out <- out union expr used in d
        gen(outSet, u);
    }

    private void kill(FlowSet inSet, Unit u, FlowSet outSet){
        String name = "";

        if (u instanceof DefinitionStmt){
            Local var = (Local)((DefinitionStmt) u).getLeftOp();
            name = var.getName();


        }

        for(Object obj: inSet){
            Map<String, List<Integer>> ele = (Map<String, List<Integer>>) obj;
            if (!ele.containsKey(name)){
                outSet.add(ele);
            }
        }
        //Iterator inIt = inSet.iterator();
        //inSet.difference(kills, outSet);
    }

    private String getOnlyKey(Map<String, List<Integer>> bag){
        String ret = "";
        for(String key: bag.keySet()){
            ret = key;
            break;
        }
        return ret;
    }

    private void gen(FlowSet outSet, Unit u){

        if (u instanceof InvokeStmt){
            InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
            if (ie.getMethod().toString().contains("Benchmark: void alloc(int)>")){
                allocId = ((IntConstant) ie.getArgs().get(0)).value;
            }
        }

        //Iterator outIt = outSet.iterator();

        if (u instanceof DefinitionStmt) {
            Map<String, List<Integer>> ele = new HashMap<>();
            List<Integer> arr = new ArrayList<>();

            if (((DefinitionStmt) u).getRightOp() instanceof NewExpr) {
                //TODO check constructor's parameter

                Local var = (Local) ((DefinitionStmt) u).getLeftOp();
                String name = var.getName();
                arr.add(allocId);
                ele.put(name, arr);
                outSet.add(ele);
            }

            if (((DefinitionStmt) u).getLeftOp() instanceof Local && ((DefinitionStmt) u).getRightOp() instanceof Local) {
                Local varLeft = (Local) ((DefinitionStmt) u).getLeftOp();
                String left = varLeft.getName();

                Local varRight = (Local) ((DefinitionStmt) u).getRightOp();
                String right = varRight.getName();

                //Boolean leftIn = false;
                Boolean rightIn = false;

                List<Integer> rightList = new ArrayList<>();

                for (Object obj : outSet) {
                    Map<String, List<Integer>> item = (Map<String, List<Integer>>) obj;
                    if (item.containsKey(right)) {
                        rightIn = true;
                        rightList = item.get(right);
                    }
                }

                if (!rightIn) return;
                ele = new HashMap<>();
                arr = new ArrayList<>();
                arr.addAll(rightList);
                ele.put(left, arr);
                outSet.add(ele);
            }
        }

    }
}
