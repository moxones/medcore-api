package com.medical.medcore.identity.controller;

import com.medical.medcore.identity.dto.AccountProfileResponse;
import com.medical.medcore.identity.dto.AuthResponse;
import com.medical.medcore.identity.dto.ChangePasswordRequest;
import com.medical.medcore.identity.dto.CompleteProfileRequest;
import com.medical.medcore.identity.service.AccountProfileService;
import com.medical.medcore.identity.service.AuthService;
import com.medical.medcore.shared.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class AccountController {

    private final AuthService authService;
    private final RefreshTokenTransport refreshTokenTransport;
    private final AccountProfileService accountProfileService;

    @GetMapping
    public ApiResponse<AccountProfileResponse> profile() {
        return ApiResponse.ok(accountProfileService.current());
    }

    @PutMapping
    public ApiResponse<AccountProfileResponse> completeProfile(@Valid @RequestBody CompleteProfileRequest request) {
        return ApiResponse.ok(accountProfileService.complete(request), "Perfil actualizado");
    }

    @PutMapping("/password")
    public ApiResponse<AuthResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                    HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        AuthResponse session = authService.changeOwnPassword(request);
        return ApiResponse.ok(refreshTokenTransport.deliver(session, httpRequest, httpResponse), "Contraseña actualizada");
    }
}
