package de.whitefrog.frogr.patch

/**
 * Version tag for patches.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
annotation class Version(val value: String, val priority: Int = 0)
