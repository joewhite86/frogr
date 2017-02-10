package de.whitefrog.neobase.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Benchmark {
  String text() default "";
  long expectation() default -1;
  int count() default 10000;
  TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
}