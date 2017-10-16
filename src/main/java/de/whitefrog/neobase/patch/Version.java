package de.whitefrog.neobase.patch;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

/**
 * Version tag for patches.
 */
@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Version {
  String value();
}
