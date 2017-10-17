package de.whitefrog.froggy.model.annotation

/**
 * Indicates that a field should not be persistated.
 */
@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class NotPersistant
