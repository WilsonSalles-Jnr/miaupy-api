package com.miaupy.shared.exception;

public class TenantAccessDeniedException extends RuntimeException {
    public TenantAccessDeniedException(String message) {
        super(message);
    }
}
