package com.ecom.common;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    public final HttpStatus status;
    public final String code;
    public ApiException(HttpStatus status, String code, String message) {
        super(message); this.status = status; this.code = code;
    }
    public static ApiException missing(String resource) { return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", resource + " not found"); }
    public static ApiException conflict(String message) { return new ApiException(HttpStatus.CONFLICT, "CONFLICT", message); }
    public static ApiException forbidden() { return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You cannot access this resource"); }
    public static ApiException bad(String message) { return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message); }
}
