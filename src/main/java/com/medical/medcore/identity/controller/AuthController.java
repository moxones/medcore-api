package com.medical.medcore.identity.controller;

import com.medical.medcore.identity.dto.ActivateAccountRequest;
import com.medical.medcore.identity.dto.ActivationInfoResponse;
import com.medical.medcore.identity.dto.ActivationTokenRequest;
import com.medical.medcore.identity.dto.AuthResponse;
import com.medical.medcore.identity.dto.ForgotPasswordRequest;
import com.medical.medcore.identity.dto.ForgotPasswordResponse;
import com.medical.medcore.identity.dto.LoginRequest;
import com.medical.medcore.identity.dto.LogoutRequest;
import com.medical.medcore.identity.dto.RefreshRequest;
import com.medical.medcore.identity.dto.ResetPasswordRequest;
import com.medical.medcore.identity.dto.UserMeResponse;
import com.medical.medcore.identity.service.AccountActivationService;
import com.medical.medcore.identity.service.AuthService;
import com.medical.medcore.identity.service.PasswordRecoveryService;
import com.medical.medcore.shared.api.ApiResponse;
import com.medical.medcore.shared.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    private final RefreshTokenTransport refreshTokenTransport;
    private final AccountActivationService accountActivationService;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                           HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        AuthResponse session = authService.login(request);
        return ApiResponse.ok(refreshTokenTransport.deliver(session, httpRequest, httpResponse), "Sesión iniciada");
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@RequestBody(required = false) RefreshRequest request,
                                             HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String rawToken = refreshTokenTransport.read(request != null ? request.refreshToken() : null, httpRequest);
        AuthResponse session = authService.refresh(rawToken);
        return ApiResponse.ok(refreshTokenTransport.deliver(session, httpRequest, httpResponse));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody(required = false) LogoutRequest request,
                                    HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.logout(refreshTokenTransport.read(request != null ? request.refreshToken() : null, httpRequest));
        refreshTokenTransport.clear(httpRequest, httpResponse);
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
                passwordRecoveryService.requestReset(request.identifier(), ClientIpResolver.resolve(httpRequest));
        return ApiResponse.ok(response,
                "Si los datos corresponden a una cuenta, enviamos un código al correo registrado");
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordRecoveryService.resetPassword(request);
        return ApiResponse.message("Contraseña actualizada. Ya puedes iniciar sesión con tu nueva contraseña");
    }

    @PostMapping("/activation/check")
    public ApiResponse<ActivationInfoResponse> checkActivation(@Valid @RequestBody ActivationTokenRequest request) {
        return ApiResponse.ok(accountActivationService.inspect(request.token()));
    }

    @PostMapping("/activate")
    public ApiResponse<AuthResponse> activate(@Valid @RequestBody ActivateAccountRequest request,
                                              HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        AuthResponse session = accountActivationService.activate(request);
        return ApiResponse.ok(refreshTokenTransport.deliver(session, httpRequest, httpResponse), "Cuenta activada");
    }
}
