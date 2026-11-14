package com.medical.medcore.shared.web;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpResolver {

    private static final String FORWARDED_HEADER = "X-Forwarded-For";

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader(FORWARDED_HEADER);
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
