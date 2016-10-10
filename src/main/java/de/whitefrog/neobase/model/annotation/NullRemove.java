package de.whitefrog.neobase.model.annotation;

import java.lang.annotation.*;

/**
 *  Remove a property if set to null.
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NullRemove {}
