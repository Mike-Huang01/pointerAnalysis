import soot.PackManager;
import soot.Transform;

public class RunPointerAnalysis {
    public static void main(String[] argv){
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.mypta", new WholeProgramTransformer()));
        soot.Main.main(argv);
    }
}
