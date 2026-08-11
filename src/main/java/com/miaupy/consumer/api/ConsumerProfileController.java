package com.miaupy.consumer.api;

import com.miaupy.consumer.application.ConsumerProfileUseCase;
import com.miaupy.consumer.domain.ConsumerProfile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/consumer/me")
public class ConsumerProfileController {
  private final ConsumerProfileUseCase useCase;

  public ConsumerProfileController(ConsumerProfileUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping
  public Response get() {
    return Response.from(useCase.getMe());
  }

  @PutMapping
  public Response upsert(@Valid @RequestBody Request request) {
    return Response.from(
        useCase.upsert(
            request.name(),
            request.email(),
            request.phone(),
            request.document(),
            request.birthDate()));
  }

  public record Request(
      @NotBlank @Size(max = 160) String name,
      @NotBlank @Email @Size(max = 254) String email,
      @Size(max = 32) String phone,
      @Size(max = 32) String document,
      @Past LocalDate birthDate) {}

  public record Response(
      UUID id, String name, String email, String phone, String document, LocalDate birthDate) {
    static Response from(ConsumerProfile p) {
      return new Response(p.id(), p.name(), p.email(), p.phone(), p.document(), p.birthDate());
    }
  }
}
