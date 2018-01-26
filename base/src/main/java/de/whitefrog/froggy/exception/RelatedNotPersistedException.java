package de.whitefrog.froggy.exception;

/**
 * Thrown when a relationship should get persisted when a related model is not persisted yet.
 */
public class RelatedNotPersistedException extends PersistException {
  public RelatedNotPersistedException(String message) {
    super(message);
  }
  public RelatedNotPersistedException(String message, Throwable cause) {
    super(message, cause);
  }
}
