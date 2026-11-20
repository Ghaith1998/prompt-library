package dev.learn.promptlibrary.common;

/** Signals a duplicate or otherwise invalid state; mapped to HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
