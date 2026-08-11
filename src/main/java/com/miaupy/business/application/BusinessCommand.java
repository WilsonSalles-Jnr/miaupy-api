package com.miaupy.business.application;

public record BusinessCommand(
    String slug,
    String name,
    String tradeName,
    String document,
    String description,
    String phone,
    String email,
    String website,
    boolean publicVisible) {}
