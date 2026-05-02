package com.medical.medcore.controller;

import com.medical.medcore.dto.request.AssignRolesRequest;
import com.medical.medcore.dto.request.CreateUserRequest;
import com.medical.medcore.dto.request.UpdateUserRequest;
import com.medical.medcore.dto.request.UpdateUserStatusRequest;
import com.medical.medcore.dto.response.UserResponse;
import com.medical.medcore.security.authorization.annotation.RequireAdminOrSuperAdmin;
import com.medical.medcore.service.user.UserService;
import com.medical.medcore.types.ApiResponse;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @RequireAdminOrSuperAdmin
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, userService.create(request), "Usuario creado")
        );
    }

    @RequireAdminOrSuperAdmin
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> findAll() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, userService.findAll(), "Lista de usuarios")
        );
    }

    @RequireAdminOrSuperAdmin
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, userService.findById(id), "Usuario encontrado")
        );
    }

    @RequireAdminOrSuperAdmin
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {

        return ResponseEntity.ok(
                new ApiResponse<>(true, userService.update(id, request), "Usuario actualizado")
        );
    }

    @RequireAdminOrSuperAdmin
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request) {

        userService.updateStatus(id, request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, null, "Estado actualizado")
        );
    }

    @RequireAdminOrSuperAdmin
    @PostMapping("/{id}/roles")
    public ResponseEntity<ApiResponse<UserResponse>> assignRoles(
            @PathVariable Long id,
            @Valid @RequestBody AssignRolesRequest request) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, userService.assignRoles(id, request.getRoleIds()), "Roles asignados")
        );
    }
}