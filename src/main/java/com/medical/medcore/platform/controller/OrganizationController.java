package com.medical.medcore.platform.controller;

import com.medical.medcore.platform.dto.LogoResponse;
import com.medical.medcore.platform.dto.OrganizationProfileResponse;
import com.medical.medcore.platform.dto.ThemeOptionsResponse;
import com.medical.medcore.platform.dto.ThemeRequest;
import com.medical.medcore.platform.dto.UpdateOrganizationProfileRequest;
import com.medical.medcore.platform.service.TenantBrandingService;
import com.medical.medcore.platform.service.TenantLogoService;
import com.medical.medcore.shared.api.ApiResponse;
import com.medical.medcore.shared.security.annotation.RequireAdmin;
import com.medical.medcore.shared.tenancy.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/organization")
@RequiredArgsConstructor
@RequireAdmin
public class OrganizationController {

    private final TenantBrandingService brandingService;
    private final TenantLogoService logoService;

    @GetMapping
    public ApiResponse<OrganizationProfileResponse> profile() {
        return ApiResponse.ok(brandingService.profile(TenantContext.requireTenantId()));
    }

    @PutMapping("/profile")
    public ApiResponse<OrganizationProfileResponse> updateProfile(
            @Valid @RequestBody UpdateOrganizationProfileRequest request) {
        return ApiResponse.ok(brandingService.updateProfile(TenantContext.requireTenantId(), request),
                "Datos de la organización actualizados");
    }

    @GetMapping("/theme-options")
    public ApiResponse<ThemeOptionsResponse> themeOptions() {
        return ApiResponse.ok(brandingService.themeOptions());
    }

    @PutMapping("/theme")
    public ApiResponse<OrganizationProfileResponse> updateTheme(@Valid @RequestBody ThemeRequest request) {
        return ApiResponse.ok(brandingService.updateTheme(TenantContext.requireTenantId(), request),
                "Apariencia actualizada");
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<LogoResponse> uploadLogo(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(logoService.upload(TenantContext.requireTenantId(), file), "Logo actualizado");
    }

    @DeleteMapping("/logo")
    public ApiResponse<Void> removeLogo() {
        logoService.remove(TenantContext.requireTenantId());
        return ApiResponse.message("Logo eliminado");
    }
}
