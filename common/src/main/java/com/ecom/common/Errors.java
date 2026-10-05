package com.ecom.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.*;

@RestControllerAdvice
public class Errors {
    public record ErrorBody(String code, String message, Map<String,String> fields, String traceId) {}
    private ResponseEntity<ErrorBody> response(HttpStatus status, String code, String message, Map<String,String> fields) {
        return ResponseEntity.status(status).body(new ErrorBody(code, message, fields, MDC.get("traceId")));
    }
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorBody> api(ApiException e) { return response(e.status,e.code,e.getMessage(),Map.of()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorBody> validation(MethodArgumentNotValidException e) {
        Map<String,String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.put(f.getField(),f.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST,"VALIDATION_FAILED","Check the highlighted fields",fields);
    }
    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ErrorBody> malformed(Exception e) { return response(HttpStatus.BAD_REQUEST,"INVALID_REQUEST","Invalid request values",Map.of()); }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorBody> denied(Exception e) { return response(HttpStatus.FORBIDDEN,"FORBIDDEN","Insufficient permission",Map.of()); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorBody> duplicate(Exception e) { return response(HttpStatus.CONFLICT,"CONFLICT","A value is duplicated or violates a data constraint",Map.of()); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorBody> unexpected(Exception e) {
        org.slf4j.LoggerFactory.getLogger(Errors.class).error("Unhandled failure type={} traceId={}",e.getClass().getSimpleName(),MDC.get("traceId"));
        return response(HttpStatus.INTERNAL_SERVER_ERROR,"INTERNAL_ERROR","An unexpected error occurred",Map.of());
    }
}
