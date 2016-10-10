package de.whitefrog.neobase.exception;

public class RepositoryInstantiationException extends NeobaseRuntimeException {
    public RepositoryInstantiationException(Throwable cause) {
        super(cause);
    }

    public RepositoryInstantiationException(String s, Throwable cause) {
        super(s, cause);
    }
}
