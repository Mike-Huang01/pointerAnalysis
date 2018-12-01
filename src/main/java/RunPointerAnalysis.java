import soot.PackManager;
import soot.Transform;

import java.util.Arrays;
import java.util.stream.Stream;

public class RunPointerAnalysis {
    public static String mainClass;
    public static void main(String[] argv)
    {
        // 命令行中最后一个类名认为是我们要分析的main()函数所在的类
        mainClass = argv[argv.length - 1];
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.mypta", new WholeProgramTransformer()));
        String newArgv[] = Stream.of(new String[] { "-w" }, argv).flatMap(Stream::of).toArray(String[]::new);
        System.out.println(Arrays.toString(newArgv));
        soot.Main.main(newArgv);
    }
}
