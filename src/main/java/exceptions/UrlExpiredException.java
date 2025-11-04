package exceptions;

public class UrlExpiredException extends Exception {
    public UrlExpiredException(String message) {
        super(message);
    }
}