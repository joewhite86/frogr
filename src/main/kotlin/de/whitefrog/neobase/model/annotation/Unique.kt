package de.whitefrog.neobase.model.annotation

/**
 * Indicates a property must be and will be unique.
 */
@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Unique
