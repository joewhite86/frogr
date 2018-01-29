package de.whitefrog.frogr.model.annotation

import org.neo4j.graphdb.Direction

/**
 * Indicates a property is a set of related entities.
 */
@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class RelatedTo(
  /**
   * The relationship type name to use.
   */
  val type: String = "None",
  /**
   * The relationships direction.
   */
  val direction: Direction = Direction.OUTGOING,
  /**
   * Allows multiple relationships with the same nodes.
   * So Node A can have two relationships to Node B with the same name.
   */
  val multiple: Boolean = false,
  /**
   * Restricts the type to the annotated one.
   */
  val restrictType: Boolean = false)
