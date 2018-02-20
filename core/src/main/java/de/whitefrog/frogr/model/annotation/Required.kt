package de.whitefrog.frogr.model.annotation

/**
 * The field is required upon storing to database. If it is missing an exception will be thrown.
 */
@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Required
