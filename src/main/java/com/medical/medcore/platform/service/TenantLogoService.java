package com.medical.medcore.platform.service;

import com.medical.medcore.platform.domain.Tenant;
import com.medical.medcore.platform.domain.TenantLogo;
import com.medical.medcore.platform.dto.LogoResponse;
import com.medical.medcore.platform.repository.TenantLogoRepository;
import com.medical.medcore.platform.repository.TenantRepository;
import com.medical.medcore.shared.exception.BadRequestException;
import com.medical.medcore.shared.exception.NotFoundException;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantLogoService {

    public static final int MAX_BYTES = 512 * 1024;
    public static final String PUBLIC_PATH = "/public/logos/";

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP = "WEBP".getBytes(StandardCharsets.US_ASCII);
    private static final int WEBP_OFFSET = 8;

    private final TenantLogoRepository logoRepository;
    private final TenantRepository tenantRepository;

    @Transactional
    public LogoResponse upload(Long tenantId, MultipartFile file) {
        byte[] content = read(file);
        String contentType = detectContentType(content);
        Tenant tenant = loadTenant(tenantId);
        TenantLogo logo = logoRepository.findById(tenantId)
                .orElseGet(() -> TenantLogo.builder().tenantId(tenantId).build());
        logo.setLogoKey(UUID.randomUUID().toString());
        logo.setContentType(contentType);
        logo.setContent(content);
        logo.setSizeBytes(content.length);
        logo.setCreatedAt(LocalDateTime.now());
        logo.setCreatedBy(TenantContext.getCurrentUserId());
        logoRepository.save(logo);
        tenant.setLogoUrl(PUBLIC_PATH + logo.getLogoKey());
        tenantRepository.save(tenant);
        return new LogoResponse(tenant.getLogoUrl());
    }

    @Transactional
    public void remove(Long tenantId) {
        Tenant tenant = loadTenant(tenantId);
        logoRepository.findById(tenantId).ifPresent(logoRepository::delete);
        tenant.setLogoUrl(null);
        tenantRepository.save(tenant);
    }

    public Optional<TenantLogo> findByKey(String key) {
        return TenantContext.runAsSystem(() -> logoRepository.findByLogoKey(key));
    }

    private byte[] read(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("LOGO_EMPTY", "Selecciona una imagen para el logo");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("LOGO_TOO_LARGE", "El logo no puede superar los 512 KB");
        }
        try {
            return file.getBytes();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private String detectContentType(byte[] content) {
        if (startsWith(content, PNG_SIGNATURE, 0)) {
            return "image/png";
        }
        if (startsWith(content, JPEG_SIGNATURE, 0)) {
            return "image/jpeg";
        }
        if (startsWith(content, RIFF, 0) && startsWith(content, WEBP, WEBP_OFFSET)) {
            return "image/webp";
        }
        throw new BadRequestException("LOGO_INVALID_FORMAT", "El logo debe ser una imagen PNG, JPG o WebP");
    }

    private boolean startsWith(byte[] content, byte[] signature, int offset) {
        return content.length >= offset + signature.length
                && Arrays.equals(content, offset, offset + signature.length, signature, 0, signature.length);
    }

    private Tenant loadTenant(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Organización no encontrada"));
    }
}
