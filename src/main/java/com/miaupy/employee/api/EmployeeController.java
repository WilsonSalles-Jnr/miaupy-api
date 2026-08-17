package com.miaupy.employee.api;

import com.miaupy.employee.application.EmployeeUseCase;
import com.miaupy.employee.domain.Employee;
import com.miaupy.employee.domain.EmployeeRole;
import com.miaupy.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/business/employees")
@Tag(name = "Funcionários", description = "Equipe e acessos vinculados ao tenant autenticado.")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class EmployeeController {
  private final EmployeeUseCase useCase;

  public EmployeeController(EmployeeUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping
  @Operation(
      summary = "Listar funcionários",
      description = "Lista funcionários paginados exclusivamente do tenant autenticado.")
  public PageResponse<EmployeeResponse> list(
      @Parameter(description = "Índice da página, iniciando em zero.")
          @RequestParam(defaultValue = "0")
          @PositiveOrZero
          int page,
      @Parameter(description = "Quantidade de elementos por página, entre 1 e 100.")
          @RequestParam(defaultValue = "20")
          @Min(1)
          @Max(100)
          int size) {
    return PageResponse.from(useCase.list(page, size), EmployeeResponse::from);
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Consultar funcionário",
      description = "Consulta o funcionário utilizando id e tenant_id.")
  public EmployeeResponse get(
      @Parameter(description = "UUID do funcionário no tenant autenticado.") @PathVariable
          UUID id) {
    return EmployeeResponse.from(useCase.get(id));
  }

  @PostMapping
  @Operation(
      summary = "Cadastrar funcionário com login",
      description =
          "Cria o funcionário e sua identidade empresarial. A senha é temporária, não é persistida pela API e deve ser alterada no primeiro login.")
  public ResponseEntity<CreateEmployeeResponse> create(
      @Valid @RequestBody CreateEmployeeRequest request) {
    Employee employee = useCase.create(request.command());
    CreateEmployeeResponse response =
        new CreateEmployeeResponse(EmployeeResponse.from(employee), employee.email(), true);
    return ResponseEntity.created(URI.create("/api/v1/business/employees/" + employee.id()))
        .body(response);
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Desativar funcionário",
      description =
          "Desativa o cadastro e bloqueia o acesso do funcionário no provedor de identidade.")
  public ResponseEntity<Void> deactivate(
      @Parameter(description = "UUID do funcionário no tenant autenticado.") @PathVariable
          UUID id) {
    useCase.deactivate(id);
    return ResponseEntity.noContent().build();
  }

  public record CreateEmployeeRequest(
      @NotBlank @Size(max = 160) @Schema(description = "Nome completo do funcionário.") String name,
      @NotBlank @Email @Size(max = 254) @Schema(description = "E-mail usado como login.")
          String email,
      @Size(max = 32) @Schema(description = "Telefone de contato.") String phone,
      @NotBlank
          @Size(min = 12, max = 128)
          @Schema(description = "Senha temporária, alterada obrigatoriamente no primeiro acesso.")
          String temporaryPassword,
      @NotNull @Schema(description = "Função operacional, sem permissão de proprietário.")
          EmployeeRole role) {
    EmployeeUseCase.Command command() {
      return new EmployeeUseCase.Command(name, email, phone, temporaryPassword, role);
    }
  }

  public record EmployeeResponse(
      UUID id, String name, String email, String phone, EmployeeRole role, boolean active) {
    static EmployeeResponse from(Employee employee) {
      return new EmployeeResponse(
          employee.id(),
          employee.name(),
          employee.email(),
          employee.phone(),
          employee.role(),
          employee.active());
    }
  }

  public record CreateEmployeeResponse(
      EmployeeResponse employee, String login, boolean passwordChangeRequired) {}
}
