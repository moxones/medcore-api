package com.medical.medcore.platform.controller;

import com.medical.medcore.platform.dto.BusinessTypeResponse;
import com.medical.medcore.platform.dto.LogoResponse;
import com.medical.medcore.platform.dto.CreateTenantRequest;
import com.medical.medcore.platform.dto.PurgeTenantRequest;
import com.medical.medcore.platform.dto.TenantResponse;
import com.medical.medcore.platform.dto.UpdateTenantRequest;
import com.medical.medcore.platform.service.TenantDataService;
import com.medical.medcore.platform.service.TenantLogoService;
import com.medical.medcore.platform.service.TenantService;
import com.medical.medcore.shared.api.ApiResponse;
import com.medical.medcore.shared.security.annotation.RequireSuperAdmin;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
@RequireSuperAdmin
public class TenantController {

    private final TenantService tenantService;
    private final TenantDataService tenantDataService;
    private final TenantLogoService logoService;

    @PostMapping
    public ApiResponse<TenantResponse> create(@Valid @RequestBody CreateTenantRequest request) {
        return ApiResponse.ok(tenantService.createTenant(request), "Organización creada");
    }

    @PutMapping("/{id}")
    public ApiResponse<TenantResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateTenantRequest request) {
        return ApiResponse.ok(tenantService.updateTenant(id, request), "Organización actualizada");
    }

    @GetMapping("/business-types")
    public ApiResponse<List<BusinessTypeResponse>> businessTypes() {
        return ApiResponse.ok(tenantService.businessTypes(), "Rubros disponibles");
    }

    @GetMapping("/{id}")
    public ApiResponse<TenantResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(tenantService.getTenantById(id), "Organización encontrada");
    }

    @GetMapping
    public ApiResponse<List<TenantResponse>> findAll() {
        return ApiResponse.ok(tenantService.getAllTenants(), "Lista de organizaciones");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deactivate(@PathVariable Long id) {
        tenantService.deactivateTenant(id);
        return ApiResponse.message("Organización desactivada");
    }

    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<LogoResponse> uploadLogo(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(logoService.upload(id, file), "Logo actualizado");
    }

    @DeleteMapping("/{id}/logo")
    public ApiResponse<Void> removeLogo(@PathVariable Long id) {
        logoService.remove(id);
        return ApiResponse.message("Logo eliminado");
    }

    @GetMapping("/{id}/export")
    public void export(@PathVariable Long id, HttpServletResponse response) throws IOException {
        String subdomain = tenantDataService.requireTenant(id).getSubdomain();
        String filename = "medcore-" + subdomain + "-" + LocalDate.now() + ".json";
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename).build().toString());
        tenantDataService.export(id, response.getOutputStream());
    }

    @PostMapping("/{id}/purge")
    public ApiResponse<Void> purge(@PathVariable Long id, @Valid @RequestBody PurgeTenantRequest request) {
        tenantDataService.purge(id, request.confirmation());
        return ApiResponse.message("Organización eliminada definitivamente");
    }
}
