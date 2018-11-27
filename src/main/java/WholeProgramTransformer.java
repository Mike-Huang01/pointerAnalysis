import soot.*;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WholeProgramTransformer extends SceneTransformer {
    protected  void internalTransform(String arg0, Map<String, String> arg1){
        //Chain<SootClass> clazz = Scene.v().getApplicationClasses();
        SootClass mainClass =  Scene.v().getSootClass("Hello");
        SootMethod m = mainClass.getMethodByName("main");
        Body b = m.retrieveActiveBody();

        UnitGraph graph = new ExceptionalUnitGraph(b);

        AndersonAnalysis pointerAnalysis = new AndersonAnalysis(graph);

        Iterator gIt = graph.iterator();
        while (gIt.hasNext()){
            Unit u = (Unit) gIt.next();
            Object before = pointerAnalysis.getResultBeforeUnit(u);
            Object after = pointerAnalysis.getResultAfterUnit(u);
            System.out.println(u.toString());
            System.out.println(before);
            System.out.println(after);
            System.out.println("-------------");
            System.out.println("-------------");
        }

        pointerAnalysis.printAnswer();
    }
}
