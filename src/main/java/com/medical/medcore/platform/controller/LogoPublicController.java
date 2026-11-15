package com.medical.medcore.platform.controller;

import com.medical.medcore.platform.domain.TenantLogo;
import com.medical.medcore.platform.service.TenantLogoService;
import com.medical.medcore.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class LogoPublicController {

    private static final Duration CACHE_DURATION = Duration.ofDays(365);

    private final TenantLogoService logoService;

    @GetMapping(TenantLogoService.PUBLIC_PATH + "{key}")
    public ResponseEntity<byte[]> logo(@PathVariable String key) {
        TenantLogo logo = logoService.findByKey(key)
                .orElseThrow(() -> new NotFoundException("LOGO_NOT_FOUND", "Logo no encontrado"));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(logo.getContentType()))
                .cacheControl(CacheControl.maxAge(CACHE_DURATION).cachePublic().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .body(logo.getContent());
    }
}
