package com.medical.medcore.controller;

import com.medical.medcore.dto.request.CreateSuperAdminUserRequest;
import com.medical.medcore.dto.request.UpdateUserRequest;
import com.medical.medcore.dto.response.UserResponse;
import com.medical.medcore.security.authorization.annotation.RequireSuperAdmin;
import com.medical.medcore.service.user.UserService;
import com.medical.medcore.types.ApiResponse;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/super-admin/users")
public class SuperAdminUserController {

    private final UserService userService;

    public SuperAdminUserController(UserService userService) {
        this.userService = userService;
    }

    @RequireSuperAdmin
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createForTenant(
            @Valid @RequestBody CreateSuperAdminUserRequest request) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, userService.createForTenant(request), "Usuario creado")
        );
    }

    @RequireSuperAdmin
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> findAllByTenant(
            @RequestParam(required = false) Long tenantId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, userService.findAllByTenant(tenantId), "Lista de usuarios")
        );
    }

    @RequireSuperAdmin
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, userService.updateForSuperAdmin(id, request), "Usuario actualizado")
        );
    }
}
