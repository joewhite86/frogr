package de.whitefrog.frogr.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Describes a benchmark method. Can have a text, 
 * a expectation how long the operation will take,
 * a count how often the operation should repeat and
 * the time unit used when milliseconds is not appropriate.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Benchmark {
  String text() default "";
  long expectation() default -1;
  int count() default 10000;
  TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
}