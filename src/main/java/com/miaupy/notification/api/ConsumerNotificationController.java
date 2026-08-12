package com.miaupy.notification.api;

import com.miaupy.notification.application.NotificationQueryUseCase;
import com.miaupy.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/consumer/me/notifications")
public class ConsumerNotificationController {
  private final NotificationQueryUseCase useCase;

  public ConsumerNotificationController(NotificationQueryUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping
  @Operation(
      summary = "Listar minhas notificações",
      description =
          "Lista somente notificações associadas ao ConsumerProfile autenticado, sem expor o endereço de entrega.")
  public PageResponse<NotificationResponse> list(
      @Parameter(description = "Índice da página, iniciando em zero.")
          @RequestParam(defaultValue = "0")
          @PositiveOrZero
          int page,
      @Parameter(description = "Quantidade de elementos por página, entre 1 e 100.")
          @RequestParam(defaultValue = "20")
          @Min(1)
          @Max(100)
          int size) {
    return PageResponse.from(useCase.listConsumer(page, size), NotificationResponse::from);
  }
}
