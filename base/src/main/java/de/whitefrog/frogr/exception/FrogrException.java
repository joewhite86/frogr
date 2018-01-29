package de.whitefrog.frogr.exception;

public class FrogrException extends RuntimeException {
    public FrogrException() {
        super();
    }

    public FrogrException(String message) {
        super(message);
    }

    public FrogrException(String message, Throwable cause) {
        super(message, cause);
    }

    public FrogrException(Throwable cause) {
        super(cause);
    }

    protected FrogrException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
