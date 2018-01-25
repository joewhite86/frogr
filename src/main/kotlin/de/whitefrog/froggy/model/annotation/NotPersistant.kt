package de.whitefrog.froggy.model.annotation

/**
 * The field should not be persisted.
 */
@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class NotPersistant
