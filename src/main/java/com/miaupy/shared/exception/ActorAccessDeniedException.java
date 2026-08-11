package com.miaupy.shared.exception;

public class ActorAccessDeniedException extends RuntimeException {
    public ActorAccessDeniedException(String message) {
        super(message);
    }
}
