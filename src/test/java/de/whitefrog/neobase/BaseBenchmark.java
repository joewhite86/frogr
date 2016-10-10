package de.whitefrog.neobase;

import de.whitefrog.neobase.helper.TimeUtils;
import junit.framework.TestCase;
import org.junit.*;
import org.junit.rules.TestName;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BaseBenchmark {
  @Rule
  public TestName test = new TestName();

  private static final Map<String, Task> results = new HashMap<>();
  private static long benchmarkStart;


  public static class Task {
    public String text;
    public long expectation;
    public int count;
    public long start;
    public long result;

    public Task(Benchmark benchmark) {
      text = benchmark.text();
      count = benchmark.count();
      expectation = benchmark.timeUnit().toNanos(benchmark.expectation());
      start = System.nanoTime();
    }
  }

  @BeforeClass
  public static void startBenchmark() {
    benchmarkStart = System.nanoTime();
  }

  @AfterClass
  public static void printStatistics() {
    PrintStream stream = System.out;

    stream.println("");
    stream.println("Benchmark took: " +
      TimeUtils.formatInterval(System.nanoTime() - benchmarkStart, TimeUnit.NANOSECONDS));
    stream.println("");
    stream.println("\tDescription: (actual) => (expectation)");
    stream.println("\t======================================");

    for(Task task: results.values()) {
      printStatistics(task);
    }
  }

  private static void printStatistics(Task task) {
    String notPassed = " (- NOT PASSED -)";
    String name = task.text;
    if(name.isEmpty()) {
      for(String method: results.keySet()) {
        if(results.get(method).equals(task)) {
          name = method;
          break;
        }
      }
    }
    System.out.println(String.format("\t%s in: %s%s%s\tThroughput: %s",
      name,
      TimeUtils.formatInterval(task.result, TimeUnit.NANOSECONDS),
      task.expectation > 0? " => " + TimeUtils.formatInterval(task.expectation, TimeUnit.NANOSECONDS): "",
      task.expectation > 0? task.result <= task.expectation ? " (passed)" : notPassed: "",
      TimeUtils.perSecond(task.result)));
  }

  @Before
  public void before() throws Exception {
    Method method = getClass().getMethod(test.getMethodName());
    if(method.isAnnotationPresent(Benchmark.class)) {
      Benchmark benchmark = getClass().getMethod(test.getMethodName()).getAnnotation(Benchmark.class);
      Task task = new Task(benchmark);
      results.put(test.getMethodName(), task);
    }
  }

  @After
  public void after() throws Exception {
    if(results.containsKey(test.getMethodName())) {
      Task task = results.get(test.getMethodName());
      task.result = (System.nanoTime() - task.start) / task.count;
      printStatistics(task);
      if(task.expectation > 0) {
        TestCase.assertTrue("expected " + task.result + " to be less than " + task.expectation,
          task.result < task.expectation);
      }
    }
  }
}
