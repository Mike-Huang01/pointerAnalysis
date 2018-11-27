import soot.*;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.util.queue.QueueReader;

import java.util.*;

public class WholeProgramTransformer extends SceneTransformer {
    protected  void internalTransform(String arg0, Map<String, String> arg1){
        /*ReachableMethods reachableMethods = Scene.v().getReachableMethods();
        QueueReader<MethodOrMethodContext> qr = reachableMethods.listener();
        while (qr.hasNext()) {
            SootMethod sm = qr.next().method();
            if (sm.getDeclaringClass().isApplicationClass()) {
                System.out.println(sm.getDeclaringClass().getName() + "/" + sm.getName());
            }
        */


        SootClass mainClass =  Scene.v().getSootClass(RunPointerAnalysis.mainClass);
        SootMethod m = mainClass.getMethodByName("main");
        Body b = m.retrieveActiveBody();

        UnitGraph graph = new ExceptionalUnitGraph(b);

        AndersonAnalysis pointerAnalysis = new AndersonAnalysis(graph, "/main", new HashMap<String, Set<String>>());
        pointerAnalysis.printDetails();

        AndersonAnalysis.printAnswer();

    }
}
