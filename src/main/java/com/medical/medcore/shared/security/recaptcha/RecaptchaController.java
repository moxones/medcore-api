package com.medical.medcore.shared.security.recaptcha;

import com.medical.medcore.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RecaptchaController {

    private final RecaptchaVerifier verifier;

    @GetMapping("/public/recaptcha-config")
    public ApiResponse<RecaptchaConfigResponse> config() {
        return ApiResponse.ok(new RecaptchaConfigResponse(verifier.isEnabled(), verifier.siteKey()));
    }

    public record RecaptchaConfigResponse(boolean enabled, String siteKey) {
    }
}
