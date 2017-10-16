package de.whitefrog.froggy.exception;

public class ModelValidationException extends NeobaseRuntimeException {
  private final String field;
  public ModelValidationException(String message, String field) {
    super(message);
    this.field = field;
  }

  public String getField() {
    return field;
  }
}
