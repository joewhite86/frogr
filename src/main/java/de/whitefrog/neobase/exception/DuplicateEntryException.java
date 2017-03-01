package de.whitefrog.neobase.exception;

import de.whitefrog.neobase.model.Base;

import java.lang.reflect.Field;

public class DuplicateEntryException extends NeobaseRuntimeException {
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
