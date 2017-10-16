package de.whitefrog.froggy.model.annotation

/**
 * Indicates that a property is required upon saving to database.
 * If it is missing an exception will be thrown.
 */
@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Required
