package de.whitefrog.froggy.model.annotation

/**
 * Indicates a property must be and will be unique.
 * This includes that the property will be indexed as well.
 */
@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Unique
