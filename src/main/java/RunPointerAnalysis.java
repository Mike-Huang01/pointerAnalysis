import soot.PackManager;
import soot.Transform;

public class RunPointerAnalysis {
    public static String mainClass;
    public static void main(String[] argv)
    {
        // 注意要把所有要分析的类都写进命令行（否则不会进入分析）
        // 命令行中最后一个类名认为是我们要分析的main()函数所在的类
        mainClass = argv[argv.length - 1];
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.mypta", new WholeProgramTransformer()));
        soot.Main.main(argv);
    }
}
