package com.medical.medcore.controller;

import com.medical.medcore.dto.request.CreateSubscriptionRequest;
import com.medical.medcore.dto.request.UpdateSubscriptionRequest;
import com.medical.medcore.dto.response.SubscriptionResponse;
import com.medical.medcore.security.authorization.annotation.RequireAdminOrSuperAdmin;
import com.medical.medcore.service.subscription.SubscriptionService;
import com.medical.medcore.types.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@RequireAdminOrSuperAdmin
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionResponse>> createSubscription(@Valid @RequestBody CreateSubscriptionRequest request) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, subscriptionService.createSubscription(request), "Suscripción creada exitosamente")
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> updateSubscription(
            @PathVariable Long id, 
            @Valid @RequestBody UpdateSubscriptionRequest request) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, subscriptionService.updateSubscription(id, request), "Suscripción actualizada exitosamente")
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getSubscriptionById(@PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, subscriptionService.getSubscriptionById(id), "Suscripción encontrada")
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> getAllSubscriptions() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, subscriptionService.getAllSubscriptions(), "Lista de suscripciones")
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSubscription(@PathVariable Long id) {
        subscriptionService.deleteSubscription(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, null, "Suscripción eliminada exitosamente")
        );
    }
}
