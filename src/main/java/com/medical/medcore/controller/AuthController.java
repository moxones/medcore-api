package com.medical.medcore.controller;

import com.medical.medcore.dto.auth.RefreshRequest;
import com.medical.medcore.dto.auth.RegisterRequest;
import com.medical.medcore.dto.request.LoginRequest;
import com.medical.medcore.dto.response.AuthResponse;
import com.medical.medcore.dto.response.UserMeResponse;
import com.medical.medcore.service.auth.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody RefreshRequest request) {
        return authService.refresh(request.getRefreshToken());
    }

    @PostMapping("/logout")
    public void logout(@RequestBody RefreshRequest request) {
        authService.logout(request.getRefreshToken());
    }

    @GetMapping("/me")
    public UserMeResponse me() {
        return authService.me();
    }
}