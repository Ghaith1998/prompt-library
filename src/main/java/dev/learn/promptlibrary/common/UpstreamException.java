package dev.learn.promptlibrary.common;

/** Something went wrong while talking to Keycloak. */
public class UpstreamException extends RuntimeException {
    public UpstreamException(String message) {
        super(message);
    }

    public UpstreamException(String message, Throwable cause) {
        super(message, cause);
    }
}
