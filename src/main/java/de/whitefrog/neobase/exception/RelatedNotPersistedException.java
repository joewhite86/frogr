package de.whitefrog.neobase.exception;

public class RelatedNotPersistedException extends RuntimeException {
  public RelatedNotPersistedException() {
    super();
  }

  public RelatedNotPersistedException(String message) {
    super(message);
  }

  public RelatedNotPersistedException(String message, Throwable cause) {
    super(message, cause);
  }
}
