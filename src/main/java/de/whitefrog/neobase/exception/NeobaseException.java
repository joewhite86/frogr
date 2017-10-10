package de.whitefrog.neobase.exception;

public abstract class NeobaseException extends Exception {
    public NeobaseException() {
        super();
    }

    public NeobaseException(String message) {
        super(message);
    }

    public NeobaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public NeobaseException(Throwable cause) {
        super(cause);
    }

    protected NeobaseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
