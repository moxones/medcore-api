package com.medical.medcore.dto.response;

public record Cie10Response(
        Long id,
        String code,
        String description,
        String category
) {}
