package de.whitefrog.neobase.exception;

import de.whitefrog.neobase.model.Model;

import java.lang.reflect.Field;

public class DuplicateEntryException extends NeobaseRuntimeException {
  private String message;
  private Model bean;
  private Field field;
  
  public DuplicateEntryException(String message, Model bean, Field field) {
    this.message = message;
    this.bean = bean;
    this.field = field;
  }

  public String getMessage() {
    return message;
  }
  
  public Model getBean() {
    return bean;
  }
  
  public Field getField() {
    return field;
  }
}
