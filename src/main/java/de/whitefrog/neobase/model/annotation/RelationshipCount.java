package de.whitefrog.neobase.model.annotation;

import de.whitefrog.neobase.model.Model;
import org.neo4j.graphdb.Direction;

import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RelationshipCount {
  String type() default "None";
  Direction direction() default Direction.OUTGOING;
  Class<? extends Model> otherModel() default Model.class;
}
