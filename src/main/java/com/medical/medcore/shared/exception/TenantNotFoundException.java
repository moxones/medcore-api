package com.medical.medcore.shared.exception;

import org.springframework.http.HttpStatus;

public class TenantNotFoundException extends ApiException {

    public TenantNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, null, message);
    }

    public TenantNotFoundException(String code, String message) {
        super(HttpStatus.NOT_FOUND, code, message);
    }
}
