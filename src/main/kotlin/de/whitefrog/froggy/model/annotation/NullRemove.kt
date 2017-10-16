package de.whitefrog.froggy.model.annotation

/**
 * Remove a property if set to null.
 */
@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class NullRemove
