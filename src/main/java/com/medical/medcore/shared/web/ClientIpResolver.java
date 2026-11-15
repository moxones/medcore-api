package com.medical.medcore.shared.web;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
