package test;

import benchmark.internal.Benchmark;
import benchmark.objects.A;
import benchmark.objects.B;

class MyTreeNode {
    int l, r;
    MyTreeNode lch, rch;
};

public class MyTest5 {

    static MyTreeNode buildTree(int l, int r)
    {
        MyTreeNode ret;
        if (l % 2 == 1) {
            Benchmark.alloc(1);
            ret = new MyTreeNode();
        } else {
            Benchmark.alloc(2);
            ret = new MyTreeNode();
        }

        ret.l = l;
        ret.r = r;

        if (r - l > 1) {
            int m = (l + r) / 2;
            ret.lch = buildTree(l, m);
            ret.rch = buildTree(m, r);
        }

        return ret;
    }

    public static void main(String[] args) {
        Benchmark.alloc(101);
        B b = new B();
        Benchmark.alloc(100);
        A a = new A(b);
        Benchmark.test(100, a);
        Benchmark.test(101, a.f);

        MyTreeNode myRoot = buildTree(0, 4);

        Benchmark.test(1, myRoot);
        Benchmark.test(2, myRoot.lch);
        Benchmark.test(3, myRoot.rch);
        Benchmark.test(4, myRoot.lch.lch);
        Benchmark.test(5, myRoot.lch.rch);
        Benchmark.test(6, myRoot.rch.lch);
        Benchmark.test(7, myRoot.rch.rch);

        Benchmark.test(200, a);
        Benchmark.test(201, a.f);
    }
}
/*
我也不知道结果是什么
 */