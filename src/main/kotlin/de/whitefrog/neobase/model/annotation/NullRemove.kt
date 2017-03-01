package de.whitefrog.neobase.model.annotation

/**
 * Remove a property if set to null.
 */
@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class NullRemove
