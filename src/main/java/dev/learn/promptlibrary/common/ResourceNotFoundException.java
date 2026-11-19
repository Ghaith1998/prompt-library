package dev.learn.promptlibrary.common;

/** Signals an inaccessible or nonexistent resource; mapped to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
