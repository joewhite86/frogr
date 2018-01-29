package de.whitefrog.frogr.model.annotation

/**
 * Indicates that a field should be handled by index.
 */
@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Indexed
