package com.miaupy.clinical.domain;

import java.time.Instant;
import java.util.UUID;

public record ClinicalAttachment(
    UUID id,
    Long tenantId,
    UUID tenantPetId,
    UUID consultationId,
    String originalFilename,
    String contentType,
    long sizeBytes,
    String sha256,
    byte[] content,
    String uploadedBy,
    boolean active,
    Instant deletedAt,
    Instant createdAt,
    Long version) {
  public ClinicalAttachment {
    content = content == null ? null : content.clone();
  }

  @Override
  public byte[] content() {
    return content == null ? null : content.clone();
  }
}
