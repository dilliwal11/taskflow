package com.taskflow.common;

/**
 * Domain exceptions thrown by services. GlobalExceptionHandler turns each one into an HTTP status,
 * so services never need to know about HTTP.
 */
public final class Exceptions {

    private Exceptions() {}

    /** 404 */
    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    /** 403: we know who you are, and you may not do this. */
    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) {
            super(message);
        }
    }

    /** 409: the request clashes with current state (duplicate, stale version, ...). */
    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) {
            super(message);
        }
    }

    /** 400: well-formed JSON that breaks a business rule. */
    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) {
            super(message);
        }
    }
}
