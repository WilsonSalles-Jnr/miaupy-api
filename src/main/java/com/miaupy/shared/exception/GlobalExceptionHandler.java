package com.miaupy.shared.exception;

import com.miaupy.onboarding.domain.IdentityProviderUnavailableException;
import com.miaupy.onboarding.domain.RegistrationRateLimitExceededException;
import com.miaupy.order.domain.InvalidOrderTransitionException;
import com.miaupy.scheduling.domain.AppointmentConflictException;
import com.miaupy.scheduling.domain.InvalidAppointmentTransitionException;
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

  @ExceptionHandler(RegistrationRateLimitExceededException.class)
  ProblemDetail handleRateLimit(RegistrationRateLimitExceededException exception) {
    return problem(
        HttpStatus.TOO_MANY_REQUESTS,
        "RATE_LIMIT_EXCEEDED",
        "Too many requests",
        exception.getMessage());
  }

  @ExceptionHandler(IdentityProviderUnavailableException.class)
  ProblemDetail handleIdentityProvider(IdentityProviderUnavailableException exception) {
    return problem(
        HttpStatus.SERVICE_UNAVAILABLE,
        "IDENTITY_PROVIDER_UNAVAILABLE",
        "Identity provider unavailable",
        "The identity operation could not be completed safely. Retry later");
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  ProblemDetail handleNotFound(ResourceNotFoundException exception) {
    return problem(
        HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Resource not found", exception.getMessage());
  }

  @ExceptionHandler(ConflictException.class)
  ProblemDetail handleConflict(ConflictException exception) {
    return problem(
        HttpStatus.CONFLICT,
        "CONFLICT",
        "Operation conflicts with current state",
        exception.getMessage());
  }

  @ExceptionHandler({
    AppointmentConflictException.class,
    InvalidAppointmentTransitionException.class
  })
  ProblemDetail handleAppointmentConflict(RuntimeException exception) {
    return problem(
        HttpStatus.CONFLICT,
        "APPOINTMENT_CONFLICT",
        "Appointment conflict",
        exception.getMessage());
  }

  @ExceptionHandler(InvalidOrderTransitionException.class)
  ProblemDetail handleOrderConflict(InvalidOrderTransitionException exception) {
    return problem(
        HttpStatus.CONFLICT, "ORDER_CONFLICT", "Order transition conflict", exception.getMessage());
  }

  @ExceptionHandler({
    TenantAccessDeniedException.class,
    ActorAccessDeniedException.class,
    org.springframework.security.access.AccessDeniedException.class
  })
  ProblemDetail handleAccessDenied(RuntimeException exception) {
    return problem(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access denied", exception.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
    ProblemDetail detail =
        problem(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "Invalid request",
            "One or more fields are invalid");
    List<Map<String, String>> violations =
        exception.getBindingResult().getFieldErrors().stream().map(this::violation).toList();
    detail.setProperty("violations", violations);
    return detail;
  }

  @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
  ProblemDetail handleAttachmentTooLarge(
      org.springframework.web.multipart.MaxUploadSizeExceededException exception) {
    return problem(
        HttpStatus.PAYLOAD_TOO_LARGE,
        "ATTACHMENT_TOO_LARGE",
        "Attachment too large",
        "The clinical attachment exceeds the configured upload limit");
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
    return problem(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "BUSINESS_RULE_VIOLATION",
        "Business rule violation",
        exception.getMessage());
  }

  private Map<String, String> violation(FieldError error) {
    return Map.of(
        "field",
        error.getField(),
        "message",
        error.getDefaultMessage() == null ? "invalid" : error.getDefaultMessage());
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
