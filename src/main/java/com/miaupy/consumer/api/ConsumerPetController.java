package com.miaupy.consumer.api;

import com.miaupy.consumer.application.ConsumerPetUseCase;
import com.miaupy.consumer.domain.ConsumerPet;
import com.miaupy.pet.domain.PetSex;
import com.miaupy.shared.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/consumer/me/pets")
public class ConsumerPetController {
    private final ConsumerPetUseCase useCase;
    public ConsumerPetController(ConsumerPetUseCase useCase){this.useCase=useCase;}
    @PostMapping public ResponseEntity<Response> create(@Valid @RequestBody Request r){ Response body=Response.from(useCase.create(r.command())); return ResponseEntity.created(URI.create("/api/v1/consumer/me/pets/"+body.id())).body(body); }
    @GetMapping public PageResponse<Response> list(@RequestParam(defaultValue="0") @PositiveOrZero int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return PageResponse.from(useCase.list(page,size),Response::from);}
    @GetMapping("/{id}") public Response get(@PathVariable UUID id){return Response.from(useCase.get(id));}
    @PutMapping("/{id}") public Response update(@PathVariable UUID id,@Valid @RequestBody Request r){return Response.from(useCase.update(id,r.command()));}
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable UUID id){useCase.delete(id);return ResponseEntity.noContent().build();}

    public record Request(@NotBlank @Size(max=120) String name,@NotBlank @Size(max=60) String species,@Size(max=120) String breed,@PastOrPresent LocalDate birthDate,@NotNull PetSex sex,@Positive @Digits(integer=6,fraction=2) BigDecimal weight,@Size(max=80) String color,@Size(max=80) String microchip,boolean neutered){ConsumerPetUseCase.Command command(){return new ConsumerPetUseCase.Command(name,species,breed,birthDate,sex,weight,color,microchip,neutered);}}
    public record Response(UUID id,String name,String species,String breed,LocalDate birthDate,PetSex sex,BigDecimal weight,String color,String microchip,boolean neutered){static Response from(ConsumerPet p){return new Response(p.id(),p.name(),p.species(),p.breed(),p.birthDate(),p.sex(),p.weight(),p.color(),p.microchip(),p.neutered());}}
}
