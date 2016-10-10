package de.whitefrog.neobase.model.annotation;

import java.lang.annotation.*;

/**
 * Indicates that a field should be handled by index.
 */
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Indexed {
}
