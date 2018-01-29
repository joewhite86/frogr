package de.whitefrog.frogr.exception;

/**
 * Thrown when something went wrong during persistence.
 * Should be overwritten.
 */
public class PersistException extends FrogrException {
  public PersistException(String message) {
      super(message);
  }
  public PersistException(Throwable cause) {
      super(cause);
  }
  public PersistException(String message, Throwable cause) {
        super(message, cause);
    }
}
