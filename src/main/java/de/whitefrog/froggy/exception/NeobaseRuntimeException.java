package de.whitefrog.froggy.exception;

public class NeobaseRuntimeException extends RuntimeException {
    public NeobaseRuntimeException() {
        super();
    }

    public NeobaseRuntimeException(String message) {
        super(message);
    }

    public NeobaseRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public NeobaseRuntimeException(Throwable cause) {
        super(cause);
    }

    protected NeobaseRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
