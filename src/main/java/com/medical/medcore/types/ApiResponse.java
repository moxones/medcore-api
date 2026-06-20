package com.medical.medcore.types;

import com.fasterxml.jackson.annotation.JsonInclude;

public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String message;
    private String code;

    public ApiResponse(boolean success, T data, String message) {
        this(success, data, message, null);
    }

    public ApiResponse(boolean success, T data, String message, String code) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.code = code;
    }

    public boolean isSuccess() { return success; }
    public T getData() { return data; }
    public String getMessage() { return message; }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getCode() { return code; }
}