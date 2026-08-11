package com.miaupy.consumer.application;

import com.miaupy.consumer.domain.ConsumerProfile;
import com.miaupy.consumer.domain.ConsumerProfileRepository;
import com.miaupy.shared.exception.ResourceNotFoundException;
import com.miaupy.shared.security.ActorContext;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsumerProfileUseCase {
    private final ActorContext actorContext;
    private final ConsumerProfileRepository repository;

    public ConsumerProfileUseCase(ActorContext actorContext, ConsumerProfileRepository repository) {
        this.actorContext = actorContext; this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ConsumerProfile getMe() {
        return repository.findByAuthSubject(actorContext.getRequiredConsumerSubject())
                .orElseThrow(() -> new ResourceNotFoundException("Consumer profile not found"));
    }

    @Transactional
    public ConsumerProfile upsert(String name, String email, String phone, String document, LocalDate birthDate) {
        String subject = actorContext.getRequiredConsumerSubject();
        ConsumerProfile profile = repository.findByAuthSubject(subject)
                .map(current -> current.update(name, email, phone, document, birthDate))
                .orElseGet(() -> ConsumerProfile.create(subject, name, email, phone, document, birthDate));
        return repository.save(profile);
    }
}
