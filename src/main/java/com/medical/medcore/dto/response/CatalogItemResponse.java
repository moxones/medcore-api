package com.medical.medcore.dto.response;

/**
 * Respuesta genérica de un ítem de catálogo.
 * Sirve tanto para el maestro (vista SUPER_ADMIN) como para la vista por clínica
 * (activados / combo disponible).
 *
 * @param id            id del ítem MAESTRO
 * @param code          código único global
 * @param name          nombre
 * @param description    descripción (especialidades; null en otros)
 * @param durationMinutes duración efectiva (tipos de cita: override del tenant o default maestro; null en otros)
 * @param masterActive  is_active del maestro (plataforma)
 * @param activated     true si la clínica ya lo tiene activado (junction)
 * @param tenantActive  is_active dentro de la junction del tenant (null si no activado)
 * @param tenantLinkId  id de la fila junction (para desactivar; null si no activado)
 */
public record CatalogItemResponse(
        Long id,
        String code,
        String name,
        String description,
        Integer durationMinutes,
        Boolean masterActive,
        boolean activated,
        Boolean tenantActive,
        Long tenantLinkId
) {}
