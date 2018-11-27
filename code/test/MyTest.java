package test;

import benchmark.internal.Benchmark;
import benchmark.objects.A;

public class MyTest {

    static A sa;

    private void dummy(A x) {
        Benchmark.test(10000, x);

    }

    private int add(int a, int b) {
        return a + b;
    }

    private A haha(int x) {
        if (x == 100) {
            Benchmark.alloc(100);
            return new A();
        }
        if (x == 200) {
            Benchmark.alloc(200);
            return new A();
        }
        Benchmark.alloc(300);
        return new A();
    }

    public static void main(String[] args) {
        Benchmark.alloc(9999);
        MyTest t = new MyTest();

        Benchmark.alloc(1);
        A a = new A();
        Benchmark.alloc(2);
        A b = new A();
        Benchmark.alloc(3);
        A c = new A();

        System.out.println(t.add(100,200));

        if (args.length > 1) a = b;

        //if (args.length > 1) c = a;
        Benchmark.test(1, a);
        Benchmark.test(2, b);
        Benchmark.test(3, c);

        Benchmark.test(300, t.haha(10));

        sa = c;
        t.dummy(c);

        int x = 1000;
    }
}
