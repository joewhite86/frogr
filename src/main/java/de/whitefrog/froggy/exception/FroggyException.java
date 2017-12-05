package de.whitefrog.froggy.exception;

public class FroggyException extends RuntimeException {
    public FroggyException() {
        super();
    }

    public FroggyException(String message) {
        super(message);
    }

    public FroggyException(String message, Throwable cause) {
        super(message, cause);
    }

    public FroggyException(Throwable cause) {
        super(cause);
    }

    protected FroggyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
