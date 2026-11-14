package com.medical.medcore.platform.controller;

import com.medical.medcore.platform.dto.CreateSubscriptionRequest;
import com.medical.medcore.platform.dto.SubscriptionResponse;
import com.medical.medcore.platform.dto.UpdateSubscriptionRequest;
import com.medical.medcore.platform.service.SubscriptionService;
import com.medical.medcore.shared.api.ApiResponse;
import com.medical.medcore.shared.security.annotation.RequireAdminOrSuperAdmin;
import com.medical.medcore.shared.security.annotation.RequireSuperAdmin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @RequireSuperAdmin
    @PostMapping
    public ApiResponse<SubscriptionResponse> create(@Valid @RequestBody CreateSubscriptionRequest request) {
        return ApiResponse.ok(subscriptionService.create(request), "Suscripción creada");
    }

    @RequireSuperAdmin
    @PutMapping("/{id}")
    public ApiResponse<SubscriptionResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody UpdateSubscriptionRequest request) {
        return ApiResponse.ok(subscriptionService.update(id, request), "Suscripción actualizada");
    }

    @RequireAdminOrSuperAdmin
    @GetMapping("/{id}")
    public ApiResponse<SubscriptionResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(subscriptionService.findById(id), "Suscripción encontrada");
    }

    @RequireAdminOrSuperAdmin
    @GetMapping
    public ApiResponse<List<SubscriptionResponse>> findAll(@RequestParam(required = false) Long tenantId) {
        return ApiResponse.ok(subscriptionService.findAll(tenantId), "Lista de suscripciones");
    }

    @RequireSuperAdmin
    @DeleteMapping("/{id}")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        subscriptionService.cancel(id);
        return ApiResponse.message("Suscripción cancelada");
    }
}
