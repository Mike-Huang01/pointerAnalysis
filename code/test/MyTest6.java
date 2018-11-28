package test;

import benchmark.internal.Benchmark;
import benchmark.objects.A;
import benchmark.objects.B;


public class MyTest6 {

    static void test1(A a) {
        Benchmark.alloc(1001);
        a.f = new B();
    }

    static A test2(A a) {
        Benchmark.alloc(2001);
        a.f = new B();
        return a;
    }

    public static void main(String[] args) {
        A a, a1;

        Benchmark.alloc(1000);
        a = new A();
        test1(a);
        Benchmark.test(1000, a);
        Benchmark.test(1001, a.f);

        Benchmark.alloc(2000);
        a = new A();
        a1 = test2(a);
        Benchmark.test(2000, a);
        Benchmark.test(2001, a.f);
        Benchmark.test(2002, a1);
    }
}
/*
1000: 1000
1001: 1001
2000: 2000
2001: 2001
2002: 2000
 */
