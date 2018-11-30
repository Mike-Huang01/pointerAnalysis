package test;

import benchmark.internal.Benchmark;
import benchmark.objects.A;

public class MyTest8 {
    public static void main(String[] args) {

        Benchmark.alloc(1);
        A a = new A();
        Benchmark.alloc(2);
        A b = new A();
        Benchmark.alloc(3);
        A c = new A();
        Benchmark.alloc(4);
        A d = new A();


        A x[] = new A[3];
        x[0] = a;
        x[1] = b;
        x[2] = c;

        int s = args.length % 3;
        x[s] = d;
        int t = (args.length * 17 + 19) % 3;


        Benchmark.test(1, x[0]);
        Benchmark.test(2, x[1]);
        Benchmark.test(3, x[2]);
        Benchmark.test(4, x[t]);

    }
}
/*
1: 1 4
2: 2 4
3: 3 4
4: 1 2 3 4
 */