package de.whitefrog.froggy.exception;

public class ModelValidationException extends PersistException {
  private final String field;
  public ModelValidationException(String message, String field) {
    super(message);
    this.field = field;
  }

  public String getField() {
    return field;
  }
}
