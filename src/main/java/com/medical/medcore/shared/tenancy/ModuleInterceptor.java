package com.medical.medcore.shared.tenancy;

import com.medical.medcore.platform.service.TenantModuleService;
import com.medical.medcore.shared.security.annotation.RequireModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class ModuleInterceptor implements HandlerInterceptor {

    private final TenantModuleService moduleService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequireModule required = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequireModule.class);
        if (required == null) {
            required = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequireModule.class);
        }
        if (required != null) {
            moduleService.require(required.value());
        }
        return true;
    }
}
