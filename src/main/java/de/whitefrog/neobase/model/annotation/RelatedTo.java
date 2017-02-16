package de.whitefrog.neobase.model.annotation;

import org.neo4j.graphdb.Direction;

import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RelatedTo {
  /**
   * The relationship type name to use.
   */
  String type() default "None";

  /**
   * The relationships direction.
   */
  Direction direction() default Direction.OUTGOING;

  /**
   * Allows multiple relationships with the same nodes.
   * So Node A can have two relationships to Node B with the same name.
   */
  boolean multiple() default false;

  /**
   * Restricts the type to the annotated one.
   */
  boolean restrictType() default false;
}
