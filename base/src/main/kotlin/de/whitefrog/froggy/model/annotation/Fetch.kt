package de.whitefrog.froggy.model.annotation

/**
 * Indicates that a field should be automatically fetched.
 */
@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Fetch
