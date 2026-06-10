package com.medical.medcore.security.jwt;

import com.medical.medcore.types.ApiResponse;
import com.medical.medcore.util.TenantContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtFilter extends OncePerRequestFilter {

    /**
     * Endpoints públicos de autenticación donde NO debe evaluarse el header
     * Authorization: el cliente suele adjuntar su access token vencido al
     * llamar /auth/refresh y, si el filtro lo procesa, responde 401 antes de
     * que el refresh llegue al controller. /auth/me sí requiere el JWT.
     */
    private static final Set<String> PUBLIC_AUTH_PATHS = Set.of(
            "/auth/login", "/auth/register", "/auth/refresh", "/auth/logout");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PUBLIC_AUTH_PATHS.contains(request.getServletPath());
    }

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    public JwtFilter(JwtProvider jwtProvider, ObjectMapper objectMapper) {
        this.jwtProvider = jwtProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String header = request.getHeader("Authorization");

            if (header != null && header.startsWith("Bearer ")) {

                String token = header.substring(7);

                Claims claims;
                try {
                    claims = jwtProvider.extractClaims(token);
                } catch (RuntimeException e) {
                    // Token expirado/inválido: responder 401 JSON aquí mismo.
                    // Si se propaga la excepción desde un Filter, el ControllerAdvice
                    // no aplica y el cliente recibe un 500 en lugar de 401.
                    response.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"");
                    writeUnauthorized(response, e.getMessage());
                    return;
                }

                Long userId = Long.valueOf(claims.getSubject());
                Number tenantClaim = (Number) claims.get("tenantId");
                if (tenantClaim == null) {
                    writeUnauthorized(response, "El token de acceso es inválido");
                    return;
                }
                Long tenantId = tenantClaim.longValue();

                List<String> roles = extractRoles(claims.get("roles"));
                List<Long> branchIds = extractBranchIds(claims.get("branchIds"));

                TenantContext.set(tenantId, userId, branchIds);
                MDC.put("tenantId", String.valueOf(tenantId));
                MDC.put("userId", String.valueOf(userId));

                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            MDC.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                new ApiResponse<>(false, null, message != null ? message : "No autenticado"));
    }

    private List<String> extractRoles(Object rolesClaim) {
        if (!(rolesClaim instanceof Collection<?> rawRoles)) {
            return Collections.emptyList();
        }

        return rawRoles.stream()
                .filter(role -> role != null && !role.toString().isBlank())
                .map(Object::toString)
                .toList();
    }

    private List<Long> extractBranchIds(Object branchIdsClaim) {
        if (!(branchIdsClaim instanceof Collection<?> rawIds)) {
            return Collections.emptyList();
        }

        return rawIds.stream()
                .filter(id -> id instanceof Number)
                .map(id -> ((Number) id).longValue())
                .toList();
    }
}
