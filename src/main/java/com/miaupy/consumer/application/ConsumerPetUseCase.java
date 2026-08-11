package com.miaupy.consumer.application;

import com.miaupy.consumer.domain.*;
import com.miaupy.pet.domain.PetSex;
import com.miaupy.shared.exception.ResourceNotFoundException;
import com.miaupy.shared.security.ActorContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsumerPetUseCase {
    private final ActorContext actorContext;
    private final ConsumerProfileRepository profiles;
    private final ConsumerPetRepository pets;
    public ConsumerPetUseCase(ActorContext actorContext, ConsumerProfileRepository profiles, ConsumerPetRepository pets) {
        this.actorContext=actorContext; this.profiles=profiles; this.pets=pets;
    }
    @Transactional public ConsumerPet create(Command c) { return pets.save(ConsumerPet.create(ownerId(), c.name(),c.species(),c.breed(),c.birthDate(),c.sex(),c.weight(),c.color(),c.microchip(),c.neutered())); }
    @Transactional(readOnly=true) public ConsumerPet get(UUID id) { return required(id, ownerId()); }
    @Transactional(readOnly=true) public Page<ConsumerPet> list(int page,int size) { return pets.findAllByOwnerId(ownerId(), PageRequest.of(page, Math.min(size,100))); }
    @Transactional public ConsumerPet update(UUID id,Command c) { UUID owner=ownerId(); return pets.save(required(id,owner).update(c.name(),c.species(),c.breed(),c.birthDate(),c.sex(),c.weight(),c.color(),c.microchip(),c.neutered())); }
    @Transactional public void delete(UUID id) { UUID owner=ownerId(); pets.save(required(id,owner).deactivate()); }
    private UUID ownerId() { String sub=actorContext.getRequiredConsumerSubject(); return profiles.findByAuthSubject(sub).orElseThrow(() -> new ResourceNotFoundException("Consumer profile not found")).id(); }
    private ConsumerPet required(UUID id,UUID owner) { return pets.findByIdAndOwnerId(id,owner).orElseThrow(() -> new ResourceNotFoundException("Consumer pet not found")); }
    public record Command(String name,String species,String breed,LocalDate birthDate,PetSex sex,BigDecimal weight,String color,String microchip,boolean neutered) {}
}
