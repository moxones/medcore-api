package com.medical.medcore.identity.controller;

import com.medical.medcore.identity.dto.AuthResponse;
import com.medical.medcore.identity.dto.ForgotPasswordRequest;
import com.medical.medcore.identity.dto.ForgotPasswordResponse;
import com.medical.medcore.identity.dto.LoginRequest;
import com.medical.medcore.identity.dto.LogoutRequest;
import com.medical.medcore.identity.dto.RefreshRequest;
import com.medical.medcore.identity.dto.ResetPasswordRequest;
import com.medical.medcore.identity.dto.UserMeResponse;
import com.medical.medcore.identity.service.AuthService;
import com.medical.medcore.identity.service.PasswordRecoveryService;
import com.medical.medcore.shared.api.ApiResponse;
import com.medical.medcore.shared.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordRecoveryService passwordRecoveryService;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request), "Sesión iniciada");
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody(required = false) LogoutRequest request) {
        authService.logout(request != null ? request.refreshToken() : null);
        return ApiResponse.message("Sesión cerrada");
    }

    @GetMapping("/me")
    public ApiResponse<UserMeResponse> me() {
        return ApiResponse.ok(authService.me());
    }

    @PostMapping("/forgot-password")
    public ApiResponse<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                              HttpServletRequest httpRequest) {
        ForgotPasswordResponse response =
                passwordRecoveryService.requestReset(request.email(), ClientIpResolver.resolve(httpRequest));
        return ApiResponse.ok(response,
                "Si el correo está registrado, recibirás las instrucciones para restablecer tu contraseña");
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordRecoveryService.resetPassword(request);
        return ApiResponse.message("Contraseña actualizada. Ya puedes iniciar sesión");
    }
}
