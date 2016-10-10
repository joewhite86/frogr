package de.whitefrog.neobase.exception;

public class QueryParseException extends RuntimeException {
  public QueryParseException(String message, Throwable cause) {
    super(message, cause);
  }
}
