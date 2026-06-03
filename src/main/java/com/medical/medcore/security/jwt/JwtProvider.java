package com.medical.medcore.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;

@Component
public class JwtProvider {

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.expiration:900000}")
    private long expiration;

    private Key key;

    @PostConstruct
    void init() {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "security.jwt.secret debe tener al menos 256 bits (32+ caracteres). " +
                    "Configura la variable de entorno JWT_SECRET.");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    public String generateToken(Long userId, Long tenantId, List<String> roles, List<Long> branchIds) {

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("tenantId", tenantId)
                .claim("roles", roles)
                .claim("branchIds", branchIds)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new com.medical.medcore.config.exception.BadRequestException("El token de acceso ha expirado");
        } catch (JwtException e) {
            throw new com.medical.medcore.config.exception.BadRequestException("El token de acceso es inválido");
        }
    }
}