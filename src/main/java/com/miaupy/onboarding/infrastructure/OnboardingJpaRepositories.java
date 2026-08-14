package com.miaupy.onboarding.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

interface TenantJpaRepository extends JpaRepository<TenantJpaEntity, Long> {}
