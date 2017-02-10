package de.whitefrog.neobase.exception;

import de.whitefrog.neobase.model.Base;

import java.lang.reflect.Field;

public class DuplicateEntryException extends NeobaseRuntimeException {
  private String message;
  private Base bean;
  private Field field;
  
  public DuplicateEntryException(String message, Base bean, Field field) {
    this.message = message;
    this.bean = bean;
    this.field = field;
  }

  public String getMessage() {
    return message;
  }
  
  public Base getBean() {
    return bean;
  }
  
  public Field getField() {
    return field;
  }
}
