package com.medical.medcore.platform.dto;

import com.medical.medcore.platform.domain.BusinessType;
import com.medical.medcore.platform.domain.ClinicModule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateTenantRequest(
        @NotBlank(message = "El subdominio es obligatorio")
        @Pattern(regexp = "^[a-z0-9](?:[a-z0-9-]{1,48}[a-z0-9])$",
                message = "El subdominio solo admite minúsculas, números y guiones (3 a 50 caracteres)") String subdomain,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String name,
        @Size(max = 253)
        @Pattern(regexp = "^$|^(?i)(?=.{4,253}$)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}$",
                message = "El dominio propio debe ser un nombre de host válido, por ejemplo citas.miclinica.com") String customDomain,
        @Size(max = 150) String subtitle,
        @Email(message = "Email de contacto inválido") @Size(max = 150) String contactEmail,
        @Size(max = 50) String contactPhone,
        @Size(max = 255) String address,
        @Size(max = 20) String taxId,
        @Size(max = 60) String timezone,
        @Pattern(regexp = "^$|^[A-Z]{3}$", message = "La moneda debe ser un código ISO de 3 letras") String currency,
        BusinessType businessType,
        @Valid ThemeRequest theme,
        Set<ClinicModule> modules
) {
}
