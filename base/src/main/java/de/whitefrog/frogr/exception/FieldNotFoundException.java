package de.whitefrog.frogr.exception;

import de.whitefrog.frogr.model.Base;

public class FieldNotFoundException extends FrogrException {
  public FieldNotFoundException(String field, Base model) {
    super(field + " on " + model.getClass().getSimpleName());
  }
}
