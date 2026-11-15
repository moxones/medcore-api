package com.medical.medcore.shared.exception;

import org.springframework.http.HttpStatus;

public class PaymentRequiredException extends ApiException {

    public PaymentRequiredException(String code, String message) {
        super(HttpStatus.PAYMENT_REQUIRED, code, message);
    }
}
