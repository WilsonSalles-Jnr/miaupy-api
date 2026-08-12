package com.miaupy.consumer.api;

import com.miaupy.consumer.application.ConsumerPetUseCase;
import com.miaupy.consumer.domain.ConsumerPet;
import com.miaupy.pet.domain.PetSex;
import com.miaupy.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/consumer/me/pets")
public class ConsumerPetController {
  private final ConsumerPetUseCase useCase;

  public ConsumerPetController(ConsumerPetUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping
  @Operation(
      summary = "Cadastrar meu pet",
      description = "Cadastra um pet global pertencente ao consumidor autenticado.")
  public ResponseEntity<Response> create(@Valid @RequestBody Request r) {
    Response body = Response.from(useCase.create(r.command()));
    return ResponseEntity.created(URI.create("/api/v1/consumer/me/pets/" + body.id())).body(body);
  }

  @GetMapping
  @Operation(
      summary = "Listar meus pets",
      description = "Lista paginada de pets ativos pertencentes ao claim sub autenticado.")
  public PageResponse<Response> list(
      @Parameter(description = "Índice da página, iniciando em zero.")
          @RequestParam(defaultValue = "0")
          @PositiveOrZero
          int page,
      @Parameter(description = "Quantidade de elementos por página, entre 1 e 100.")
          @RequestParam(defaultValue = "20")
          @Min(1)
          @Max(100)
          int size) {
    return PageResponse.from(useCase.list(page, size), Response::from);
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Consultar meu pet",
      description = "Retorna o pet somente quando pertence ao consumidor autenticado.")
  public Response get(
      @Parameter(description = "UUID do pet pertencente ao consumidor autenticado.") @PathVariable
          UUID id) {
    return Response.from(useCase.get(id));
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Atualizar meu pet",
      description = "Atualiza dados do pet pertencente ao consumidor autenticado.")
  public Response update(
      @Parameter(description = "UUID do pet pertencente ao consumidor autenticado.") @PathVariable
          UUID id,
      @Valid @RequestBody Request r) {
    return Response.from(useCase.update(id, r.command()));
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Desativar meu pet",
      description = "Executa exclusão lógica do pet do consumidor autenticado.")
  public ResponseEntity<Void> delete(
      @Parameter(description = "UUID do pet pertencente ao consumidor autenticado.") @PathVariable
          UUID id) {
    useCase.delete(id);
    return ResponseEntity.noContent().build();
  }

  public record Request(
      @NotBlank @Size(max = 120) @Schema(description = "Nome do pet.") String name,
      @NotBlank @Size(max = 60) @Schema(description = "Espécie do pet, por exemplo DOG ou CAT.")
          String species,
      @Size(max = 120) @Schema(description = "Raça do pet, quando conhecida.") String breed,
      @PastOrPresent @Schema(description = "Data de nascimento no formato YYYY-MM-DD.")
          LocalDate birthDate,
      @NotNull @Schema(description = "Sexo do pet: MALE, FEMALE ou UNKNOWN.") PetSex sex,
      @Positive @Digits(integer = 6, fraction = 2) @Schema(description = "Peso positivo do pet.")
          BigDecimal weight,
      @Size(max = 80) @Schema(description = "Cor predominante do pet.") String color,
      @Size(max = 80) @Schema(description = "Identificador do microchip, quando existente.")
          String microchip,
      @Schema(description = "Indica se o pet é castrado.") boolean neutered) {
    ConsumerPetUseCase.Command command() {
      return new ConsumerPetUseCase.Command(
          name, species, breed, birthDate, sex, weight, color, microchip, neutered);
    }
  }

  public record Response(
      UUID id,
      String name,
      String species,
      String breed,
      LocalDate birthDate,
      PetSex sex,
      BigDecimal weight,
      String color,
      String microchip,
      boolean neutered) {
    static Response from(ConsumerPet p) {
      return new Response(
          p.id(),
          p.name(),
          p.species(),
          p.breed(),
          p.birthDate(),
          p.sex(),
          p.weight(),
          p.color(),
          p.microchip(),
          p.neutered());
    }
  }
}
