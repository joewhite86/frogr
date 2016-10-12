package de.whitefrog.neobase.exception;

public class RelationshipExistsException extends NeobaseRuntimeException {
  public RelationshipExistsException(String message) {
    super(message);
  }
}
