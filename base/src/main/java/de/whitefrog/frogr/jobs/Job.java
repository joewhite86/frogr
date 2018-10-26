package de.whitefrog.frogr.jobs;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Job {
  String value() default "---"; 
}
