package de.whitefrog.froggy.exception;

import de.whitefrog.froggy.model.Base;
import de.whitefrog.froggy.model.relationship.Relationship;

import java.lang.reflect.Field;

/**
 * Thrown when a required field is missing on a model during persistence.
 */
public class MissingRequiredException extends PersistException {
  private Base model;
  private Relationship relationship;
  private final Field field;

  public MissingRequiredException(String msg) {
    super(msg);
    this.model = null;
    this.field = null;
  }
  public MissingRequiredException(Base model, Field field) {
    super("The value for the field \"" + field.getName() + "\" is missing on " + model);
    this.model = model;
    this.field = field;
  }
  public MissingRequiredException(Relationship model, Field field) {
    super("The value for the field \"" + field.getName() + "\" is missing on " + model);
    this.relationship = model;
    this.field = field;
  }

  public MissingRequiredException(Base model, String field) {
    super("The value for the field \"" + field + "\" is missing on " + model);
    this.model = model;
    this.field = null;
  }

  public Base getModel() {
    return model;
  }

  public Field getField() {
    return field;
  }
}
