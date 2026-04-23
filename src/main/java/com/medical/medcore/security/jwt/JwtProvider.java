package com.medical.medcore.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;

@Component
public class JwtProvider {

    private final Key key = Keys.hmacShaKeyFor("my-super-secret-key-my-super-secret-key".getBytes());

    private final long EXPIRATION = 900000;

    public String generateToken(Long userId, Long tenantId, List<String> roles) {

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("tenantId", tenantId)
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}