package com.medical.medcore.identity.controller;

import com.medical.medcore.identity.dto.EmailVerificationRequest;
import com.medical.medcore.identity.dto.EmailVerificationResponse;
import com.medical.medcore.identity.service.EmailVerificationService;
import com.medical.medcore.shared.api.ApiResponse;
import com.medical.medcore.shared.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService verificationService;

    @PostMapping("/public/email-verification")
    public ApiResponse<EmailVerificationResponse> send(@Valid @RequestBody EmailVerificationRequest request,
                                                       HttpServletRequest httpRequest) {
        EmailVerificationResponse response =
                verificationService.send(request.email(), ClientIpResolver.resolve(httpRequest));
        String message = response.delivered()
                ? "Te enviamos un código a " + request.email().trim()
                : "No pudimos enviar el correo de verificación. Puedes continuar sin el código";
        return ApiResponse.ok(response, message);
    }
}
