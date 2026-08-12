package com.miaupy.business.api;

import com.miaupy.business.application.BusinessConfigurationUseCase;
import com.miaupy.business.domain.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/business")
public class BusinessConfigurationController {
  private final BusinessConfigurationUseCase useCase;

  public BusinessConfigurationController(BusinessConfigurationUseCase u) {
    useCase = u;
  }

  @GetMapping("/settings")
  @Operation(
      summary = "Consultar configurações",
      description = "Retorna timezone, moeda e permissões de operação online do tenant.")
  public SettingsResponse settings() {
    return SettingsResponse.from(useCase.getSettings());
  }

  @PutMapping("/settings")
  @Operation(
      summary = "Atualizar configurações",
      description = "Define aprovação de agenda, timezone, moeda, vendas e agendamentos online.")
  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  public SettingsResponse settings(@Valid @RequestBody SettingsRequest r) {
    return SettingsResponse.from(
        useCase.updateSettings(
            r.appointmentApprovalMode(),
            r.timezone(),
            r.currency(),
            r.allowOnlineBooking(),
            r.allowOnlineSales()));
  }

  @GetMapping("/address")
  @Operation(
      summary = "Consultar endereço empresarial",
      description = "Retorna o endereço da empresa do tenant autenticado.")
  public AddressResponse address() {
    return AddressResponse.from(useCase.getAddress());
  }

  @PutMapping("/address")
  @Operation(
      summary = "Atualizar endereço empresarial",
      description = "Cria ou atualiza endereço e coordenadas geográficas da empresa.")
  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  public AddressResponse address(@Valid @RequestBody AddressRequest r) {
    return AddressResponse.from(
        useCase.updateAddress(
            r.street(),
            r.number(),
            r.district(),
            r.city(),
            r.state(),
            r.postalCode(),
            r.latitude(),
            r.longitude()));
  }

  public record SettingsRequest(
      @NotNull @Schema(description = "Modo MANUAL ou AUTOMATIC para solicitações B2C.")
          AppointmentApprovalMode appointmentApprovalMode,
      @NotBlank
          @Size(max = 64)
          @Schema(
              description = "Timezone IANA usado nas regras locais de agenda.",
              example = "America/Sao_Paulo")
          String timezone,
      @NotBlank
          @Pattern(regexp = "[A-Z]{3}")
          @Schema(description = "Código ISO-4217 de três letras.", example = "BRL")
          String currency,
      @Schema(description = "Habilita consulta e solicitação online de horários.")
          boolean allowOnlineBooking,
      @Schema(description = "Habilita vendas online na vitrine.") boolean allowOnlineSales) {}

  public record SettingsResponse(
      AppointmentApprovalMode appointmentApprovalMode,
      String timezone,
      String currency,
      boolean allowOnlineBooking,
      boolean allowOnlineSales) {
    static SettingsResponse from(BusinessSettings s) {
      return new SettingsResponse(
          s.appointmentApprovalMode(),
          s.timezone(),
          s.currency(),
          s.allowOnlineBooking(),
          s.allowOnlineSales());
    }
  }

  public record AddressRequest(
      @NotBlank @Size(max = 160) @Schema(description = "Logradouro do endereço.") String street,
      @Size(max = 30) @Schema(description = "Número ou complemento curto do endereço.")
          String number,
      @Size(max = 100) @Schema(description = "Bairro do endereço.") String district,
      @NotBlank @Size(max = 100) @Schema(description = "Cidade do endereço.") String city,
      @NotBlank @Size(max = 80) @Schema(description = "Estado ou unidade federativa.") String state,
      @Size(max = 20) @Schema(description = "Código postal do endereço.") String postalCode,
      @DecimalMin("-90") @DecimalMax("90") @Schema(description = "Latitude decimal entre -90 e 90.")
          BigDecimal latitude,
      @DecimalMin("-180")
          @DecimalMax("180")
          @Schema(description = "Longitude decimal entre -180 e 180.")
          BigDecimal longitude) {}

  public record AddressResponse(
      String street,
      String number,
      String district,
      String city,
      String state,
      String postalCode,
      BigDecimal latitude,
      BigDecimal longitude) {
    static AddressResponse from(BusinessAddress a) {
      return new AddressResponse(
          a.street(),
          a.number(),
          a.district(),
          a.city(),
          a.state(),
          a.postalCode(),
          a.latitude(),
          a.longitude());
    }
  }
}
