package com.medical.medcore.config.exception;

/** 422: parámetros requeridos faltantes o no procesables para la operación pedida. */
public class UnprocessableEntityException extends RuntimeException {
    public UnprocessableEntityException(String message) {
        super(message);
    }
}
