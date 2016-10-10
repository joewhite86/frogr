package de.whitefrog.neobase.model.annotation;

import org.neo4j.graphdb.Direction;

import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RelatedTo {
  String type() default "None";
  Direction direction() default Direction.OUTGOING;
  boolean multiple() default false;
  boolean restrictType() default false;
}
