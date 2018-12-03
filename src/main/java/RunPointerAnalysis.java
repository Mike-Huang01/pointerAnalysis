import soot.PackManager;
import soot.Transform;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Stream;

public class RunPointerAnalysis {
    public static String mainClass;
    public static void main(String[] argv)
    {

        // 命令行中最后一个类名认为是我们要分析的main()函数所在的类
        mainClass = argv[argv.length - 1];
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.mypta", new WholeProgramTransformer()));
        String newArgv[] = Stream.of(new String[] { "-w", "-pp", "-cp" }, argv).flatMap(Stream::of).toArray(String[]::new);
        System.out.println(Arrays.toString(newArgv));
        soot.Main.main(newArgv);


		/*String classpath = argv[0]
				+ File.pathSeparator + argv[0] + File.separator + "rt.jar"
				+ File.pathSeparator + argv[0] + File.separator + "jce.jar";
		System.out.println(classpath);
		PackManager.v().getPack("wjtp").add(new Transform("wjtp.mypta", new WholeProgramTransformer()));
		soot.Main.main(new String[] {
			"-w",
			"-p", "cg.spark", "enabled:true",
			"-p", "wjtp.mypta", "enabled:true",
			"-soot-class-path", classpath,
			argv[1]
		});*/


    }
}
