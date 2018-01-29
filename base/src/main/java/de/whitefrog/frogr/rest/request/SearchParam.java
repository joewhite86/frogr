package de.whitefrog.frogr.rest.request;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

@java.lang.annotation.Target({ElementType.PARAMETER})
@java.lang.annotation.Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Documented
public @interface SearchParam {
  String value() default "params";
}
