package com.medical.medcore.shared.web;

import com.medical.medcore.shared.security.OnboardingInterceptor;
import com.medical.medcore.shared.security.recaptcha.RecaptchaInterceptor;
import com.medical.medcore.platform.service.TenantLogoService;
import com.medical.medcore.shared.tenancy.ModuleInterceptor;
import com.medical.medcore.shared.tenancy.TenantInterceptor;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantInterceptor tenantInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final OnboardingInterceptor onboardingInterceptor;
    private final ModuleInterceptor moduleInterceptor;
    private final RecaptchaInterceptor recaptchaInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/**");
        registry.addInterceptor(tenantInterceptor).addPathPatterns("/**")
                .excludePathPatterns(TenantLogoService.PUBLIC_PATH + "**");
        registry.addInterceptor(onboardingInterceptor).addPathPatterns("/**")
                .excludePathPatterns(TenantLogoService.PUBLIC_PATH + "**");
        registry.addInterceptor(moduleInterceptor).addPathPatterns("/**")
                .excludePathPatterns("/super-admin/**");
        registry.addInterceptor(recaptchaInterceptor).addPathPatterns("/**");
    }
}
