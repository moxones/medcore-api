package com.medical.medcore.identity.controller;

import com.medical.medcore.identity.dto.AuthResponse;
import com.medical.medcore.identity.dto.ChangePasswordRequest;
import com.medical.medcore.identity.service.AuthService;
import com.medical.medcore.shared.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class AccountController {

    private final AuthService authService;

    @PutMapping("/password")
    public ApiResponse<AuthResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return ApiResponse.ok(authService.changeOwnPassword(request), "Contraseña actualizada");
    }
}
