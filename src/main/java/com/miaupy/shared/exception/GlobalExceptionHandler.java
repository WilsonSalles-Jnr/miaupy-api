package com.miaupy.shared.exception;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Resource not found", exception.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException exception) {
        return problem(HttpStatus.CONFLICT, "CONFLICT", "Operation conflicts with current state", exception.getMessage());
    }

    @ExceptionHandler({TenantAccessDeniedException.class, ActorAccessDeniedException.class,
            org.springframework.security.access.AccessDeniedException.class})
    ProblemDetail handleAccessDenied(RuntimeException exception) {
        return problem(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access denied", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request", "One or more fields are invalid");
        List<Map<String, String>> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(this::violation)
                .toList();
        detail.setProperty("violations", violations);
        return detail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "BUSINESS_RULE_VIOLATION", "Business rule violation", exception.getMessage());
    }

    private Map<String, String> violation(FieldError error) {
        return Map.of("field", error.getField(), "message", error.getDefaultMessage() == null ? "invalid" : error.getDefaultMessage());
    }

    private ProblemDetail problem(HttpStatus status, String type, String title, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setType(URI.create("urn:miaupy:problem:" + type.toLowerCase()));
        detail.setTitle(title);
        detail.setProperty("code", type);
        detail.setProperty("traceId", MDC.get("traceId"));
        return detail;
    }
}
