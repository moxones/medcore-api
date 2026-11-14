package com.medical.medcore.identity.controller;

import com.medical.medcore.identity.dto.CreateSuperAdminUserRequest;
import com.medical.medcore.identity.dto.SetPasswordRequest;
import com.medical.medcore.identity.dto.UpdateUserRequest;
import com.medical.medcore.identity.dto.UserResponse;
import com.medical.medcore.identity.service.UserService;
import com.medical.medcore.shared.api.ApiResponse;
import com.medical.medcore.shared.security.annotation.RequireSuperAdmin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/super-admin/users")
@RequiredArgsConstructor
@RequireSuperAdmin
public class SuperAdminUserController {

    private final UserService userService;

    @PostMapping
    public ApiResponse<UserResponse> createForTenant(@Valid @RequestBody CreateSuperAdminUserRequest request) {
        return ApiResponse.ok(userService.createForTenant(request), "Usuario creado");
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> findAll(@RequestParam(required = false) Long tenantId) {
        return ApiResponse.ok(userService.findAllForPlatform(tenantId), "Lista de usuarios");
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok(userService.update(id, request), "Usuario actualizado");
    }

    @PutMapping("/{id}/password")
    public ApiResponse<Void> setPassword(@PathVariable Long id, @Valid @RequestBody SetPasswordRequest request) {
        userService.setPassword(id, request.newPassword());
        return ApiResponse.message("Contraseña actualizada");
    }
}
