package de.whitefrog.froggy.model.annotation

/**
 * Blob object, not persisted to database.
 * Must be handled explicitly.
 */
@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Blob
