package com.medical.medcore.identity.controller;

import com.medical.medcore.identity.dto.AssignRolesRequest;
import com.medical.medcore.identity.dto.CreateUserRequest;
import com.medical.medcore.identity.dto.RoleResponse;
import com.medical.medcore.identity.dto.SetPasswordRequest;
import com.medical.medcore.identity.dto.UpdateUserRequest;
import com.medical.medcore.identity.dto.UpdateUserStatusRequest;
import com.medical.medcore.identity.dto.UserResponse;
import com.medical.medcore.identity.service.UserService;
import com.medical.medcore.shared.api.ApiResponse;
import com.medical.medcore.shared.security.annotation.RequireAdminOrSuperAdmin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@RequireAdminOrSuperAdmin
public class UserController {

    private final UserService userService;

    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok(userService.create(request), "Usuario creado");
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> findAll() {
        return ApiResponse.ok(userService.findAll(), "Lista de usuarios");
    }

    @GetMapping("/roles")
    public ApiResponse<List<RoleResponse>> assignableRoles() {
        return ApiResponse.ok(userService.listAssignableRoles(), "Roles disponibles");
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(userService.findById(id), "Usuario encontrado");
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok(userService.update(id, request), "Usuario actualizado");
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateUserStatusRequest request) {
        userService.updateStatus(id, request.isActive());
        return ApiResponse.message("Estado actualizado");
    }

    @PostMapping("/{id}/roles")
    public ApiResponse<UserResponse> assignRoles(@PathVariable Long id, @Valid @RequestBody AssignRolesRequest request) {
        return ApiResponse.ok(userService.assignRoles(id, request.roleIds()), "Roles asignados");
    }

    @PutMapping("/{id}/password")
    public ApiResponse<Void> setPassword(@PathVariable Long id, @Valid @RequestBody SetPasswordRequest request) {
        userService.setPassword(id, request.newPassword());
        return ApiResponse.message("Contraseña restablecida");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deactivate(@PathVariable Long id) {
        userService.updateStatus(id, false);
        return ApiResponse.message("Usuario desactivado");
    }
}
