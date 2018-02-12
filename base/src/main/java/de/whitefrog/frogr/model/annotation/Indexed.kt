package de.whitefrog.frogr.model.annotation

/**
 * Indicates that a field should be handled by index.
 */

enum class IndexType {
  Default, LowerCase
}

@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Indexed(val type: IndexType = IndexType.Default)
