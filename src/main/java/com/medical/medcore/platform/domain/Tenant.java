package com.medical.medcore.platform.domain;

import com.medical.medcore.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subdomain", nullable = false, unique = true, length = 100)
    private String subdomain;

    @Column(name = "custom_domain", unique = true, length = 253)
    private String customDomain;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TenantStatus status;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "primary_color", length = 20)
    private String primaryColor;

    @Column(name = "subtitle", length = 150)
    private String subtitle;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "address")
    private String address;

    @Column(name = "tax_id", length = 20)
    private String taxId;

    @Builder.Default
    @Column(name = "timezone", nullable = false, length = 60)
    private String timezone = "America/Lima";

    @Builder.Default
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "PEN";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false, length = 30)
    private BusinessType businessType = BusinessType.MEDICAL;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "theme_preset", nullable = false, length = 30)
    private ThemePreset themePreset = ThemePreset.CLASSIC;

    @Column(name = "accent_color", length = 20)
    private String accentColor;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "font_family", nullable = false, length = 30)
    private FontFamily fontFamily = FontFamily.ROBOTO;
}
