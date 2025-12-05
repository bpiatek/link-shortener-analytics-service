package pl.bpiatek.linkshorteneranalyticsservice.exception;

public class LinkNotFoundRetryableException extends RuntimeException {
    public LinkNotFoundRetryableException(String message) {
        super(message);
    }
}