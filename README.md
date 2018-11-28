# Soot-based Pointer Analysis

* please use IntelliJ to open this project
* mark `code` and `src/main/java` as source folder in module settings
* `code/` test files
* `src/main/java/RunPointerAnalysis.java` entry of analysis

## command line options:

* last class in command line should contain `main()` method
* all user class should be provided in commandline
* `-w -cp out/production/pointerAnalysis -pp benchmark.internal.Benchmark benchmark.objects.A benchmark.objects.B test.Hello`