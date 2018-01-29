package de.whitefrog.frogr.exception;

import de.whitefrog.frogr.model.Base;

import java.lang.reflect.Field;

/**
 * Thrown when a duplicate entry would be generated during persistence.
 */
public class DuplicateEntryException extends PersistException {
  private Base bean;
  private Field field;
  
  public DuplicateEntryException(String message, Base bean) {
    super(message);
    this.bean = bean;
  }

  public DuplicateEntryException(String message, Base bean, Field field) {
    super(message);
    this.bean = bean;
    this.field = field;
  }

  public Base getBean() {
    return bean;
  }
  
  public Field getField() {
    return field;
  }
}
