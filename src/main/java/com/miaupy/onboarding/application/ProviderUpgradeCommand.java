package com.miaupy.onboarding.application;

public record ProviderUpgradeCommand(
    String slug,
    String name,
    String tradeName,
    String document,
    String description,
    String phone,
    String email,
    String website) {}
