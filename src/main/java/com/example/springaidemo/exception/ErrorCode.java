package com.example.springaidemo.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(0, "ok"),
    SENSITIVE_DATA_ACCESS_DENIED(40001, "Sensitive data access denied"),
    SYSTEM_ERROR(50000, "System error");

    private final int code;
    private final String message;

    ErrorCode(int code, String errMessage) {
        this.code = code;
        this.message = errMessage;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
