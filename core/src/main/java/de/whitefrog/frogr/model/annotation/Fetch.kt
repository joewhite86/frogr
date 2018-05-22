package de.whitefrog.frogr.model.annotation

/**
 * Indicates that a field should be automatically fetched.
 */
@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Fetch(
  /**
   * The fetch group, fields in the "auto" group will get fetched everytime.
   */
  val group: String = "auto"
)
