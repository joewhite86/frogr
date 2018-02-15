package de.whitefrog.frogr.test;

import de.whitefrog.frogr.Service;
import de.whitefrog.frogr.helper.TimeUtils;
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
  private Task currentTask;

  private static final Map<String, Task> results = new HashMap<>();
  private static long benchmarkStart;
  private static Service service;


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
    System.out.println("starting");
    benchmarkStart = System.nanoTime();
    service = new TemporaryService();
    service.connect();
  }

  @AfterClass
  public static void printStatistics() {
    service.shutdown();
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
      currentTask = task;
    }
  }

  @After
  public void after() {
    if(results.containsKey(test.getMethodName())) {
      currentTask.result = (System.nanoTime() - currentTask.start) / currentTask.count;
      printStatistics(currentTask);
      if(currentTask.expectation > 0) {
        TestCase.assertTrue("expected " + currentTask.result + " to be less than " + currentTask.expectation,
          currentTask.result < currentTask.expectation);
      }
    }
  }
  
  public static Service service() {
    return service;
  }
  
  public Task task() {
    return currentTask;
  }
}
