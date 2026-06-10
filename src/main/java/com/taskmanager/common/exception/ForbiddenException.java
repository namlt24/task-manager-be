package com.taskmanager.common.exception;

/** Thrown when the current user lacks permission for the requested action (HTTP 403). */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
