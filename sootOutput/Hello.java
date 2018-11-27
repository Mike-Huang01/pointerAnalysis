public class Hello {

  public static void main(String[] args) {
    Benchmark.alloc(1); 
    A a = new A();
    Benchmark.alloc(2);
    A b = new A();
    Benchmark.alloc(3);
    A c = new A();
    while (args.length > 1) a = b;

    a.next = b;
    b.next = c;
    //if (args.length > 1) c = a;
    Benchmark.test(1, a); 
    Benchmark.test(2, b);
    Benchmark.test(3, c);

    Benchmark.test(4, a.next);
    Benchmark.test(5, b.next);
    Benchmark.test(6, c.next);
  }
}
