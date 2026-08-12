package com.miaupy.business.api;

import com.miaupy.business.application.CreateBusinessUseCase;
import com.miaupy.business.application.GetBusinessProfileUseCase;
import com.miaupy.business.application.UpdateBusinessUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/business/profile")
public class BusinessController {

  private final CreateBusinessUseCase createBusiness;
  private final GetBusinessProfileUseCase getBusinessProfile;
  private final UpdateBusinessUseCase updateBusiness;

  public BusinessController(
      CreateBusinessUseCase createBusiness,
      GetBusinessProfileUseCase getBusinessProfile,
      UpdateBusinessUseCase updateBusiness) {
    this.createBusiness = createBusiness;
    this.getBusinessProfile = getBusinessProfile;
    this.updateBusiness = updateBusiness;
  }

  @PostMapping
  @Operation(
      summary = "Criar perfil empresarial",
      description =
          "Cria o perfil do tenant autenticado. O tenant_id é obtido exclusivamente do JWT.")
  @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
  public ResponseEntity<BusinessResponse> create(@Valid @RequestBody BusinessRequest request) {
    BusinessResponse response = BusinessResponse.from(createBusiness.execute(request.toCommand()));
    return ResponseEntity.created(URI.create("/api/v1/business/profile")).body(response);
  }

  @GetMapping
  @Operation(
      summary = "Consultar perfil empresarial",
      description = "Retorna o perfil da empresa pertencente ao tenant autenticado.")
  public BusinessResponse get() {
    return BusinessResponse.from(getBusinessProfile.execute());
  }

  @PutMapping
  @Operation(
      summary = "Atualizar perfil empresarial",
      description = "Atualiza dados comerciais e a visibilidade pública da empresa autenticada.")
  @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
  public BusinessResponse update(@Valid @RequestBody BusinessRequest request) {
    return BusinessResponse.from(updateBusiness.execute(request.toCommand()));
  }
}
