package de.whitefrog.frogr.model.annotation

/**
 * Indicator for lists to fetch them lazily on demand, not every list item at once.
 */
@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Lazy