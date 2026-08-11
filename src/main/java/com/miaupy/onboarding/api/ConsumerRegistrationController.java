package com.miaupy.onboarding.api;

import com.miaupy.onboarding.application.RegisterConsumerUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/consumers")
public class ConsumerRegistrationController {
  private static final String GENERIC_MESSAGE =
      "If the registration can be accepted, verification instructions will be sent by email.";

  private final RegisterConsumerUseCase useCase;

  public ConsumerRegistrationController(RegisterConsumerUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping("/registrations")
  public ResponseEntity<Response> register(
      @Valid @RequestBody Request request, HttpServletRequest httpRequest) {
    useCase.execute(
        httpRequest.getRemoteAddr(), request.name(), request.email(), request.password());
    return ResponseEntity.accepted().body(new Response(GENERIC_MESSAGE));
  }

  public record Request(
      @NotBlank @Size(max = 160) String name,
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(min = 12, max = 128) String password,
      @AssertTrue(message = "must be accepted") boolean termsAccepted) {}

  public record Response(String message) {}
}
