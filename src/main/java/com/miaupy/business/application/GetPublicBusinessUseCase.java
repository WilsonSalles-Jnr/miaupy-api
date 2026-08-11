package com.miaupy.business.application;

import com.miaupy.business.domain.Business;
import com.miaupy.business.domain.BusinessRepository;
import com.miaupy.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetPublicBusinessUseCase {

  private final BusinessRepository repository;

  public GetPublicBusinessUseCase(BusinessRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public Business execute(String slug) {
    return repository
        .findPublicBySlug(CreateBusinessUseCase.normalizeSlug(slug))
        .orElseThrow(() -> new ResourceNotFoundException("Public store not found"));
  }
}
