import com.sun.org.apache.xpath.internal.operations.And;
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


        AndersonAnalysis.tryEnterMethod(m);
        AndersonAnalysis pointerAnalysis = new AndersonAnalysis(AndersonAnalysis.getGraph(m),"/main", new HashMap<String, Set<String>>());
        pointerAnalysis.printDetails();
        AndersonAnalysis.leaveMethod(m);

        System.out.println("== converted ==\n" + AndersonAnalysis.getAnswer(true));
        System.out.println("== internal ==\n" + AndersonAnalysis.getAnswer(false));
        AnswerPrinter.printAnswer(AndersonAnalysis.getAnswer(true));

    }
}
