package info.search.exception;

public class UnknownDocumentException extends RuntimeException {
    public UnknownDocumentException(String message) {
        super(message);
    }
}
