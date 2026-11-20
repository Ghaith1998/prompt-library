package dev.learn.promptlibrary.common;

/** Signals rejected user credentials; mapped to HTTP 401. */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
