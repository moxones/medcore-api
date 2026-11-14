package com.medical.medcore.catalog.dto;

public record Cie10Response(
        Long id,
        String code,
        String description,
        String category
) {}
