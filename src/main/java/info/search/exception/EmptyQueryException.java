package info.search.exception;

public class EmptyQueryException extends RuntimeException {
    public EmptyQueryException(String message) {
        super(message);
    }
}
