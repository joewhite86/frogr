package de.whitefrog.frogr.model.annotation

/**
 * The field is unique across all models of the same type. Will be indexed as well.
 * If a duplicate value is passed an exception will be thrown.
 */
@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Unique
