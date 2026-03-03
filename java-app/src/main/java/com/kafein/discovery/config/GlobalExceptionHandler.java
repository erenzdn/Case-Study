package com.kafein.discovery.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler — mirrors Python exceptions.py.
 * Provides consistent JSON error responses for all exception types.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle 404 - NoResourceFoundException (e.g., unknown routes like /swagger-ui.html)
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException e) {
        log.warn("Route not found: {}", e.getResourcePath());
        return buildResponse(HttpStatus.NOT_FOUND, "Route not found: " + e.getResourcePath(), null);
    }

    /**
     * Handle ResponseStatusException (explicit HTTP errors thrown by controllers)
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException e) {
        log.warn("ResponseStatusException: {} - {}", e.getStatusCode(), e.getReason());
        return ResponseEntity.status(e.getStatusCode()).body(
            buildBody(e.getReason() != null ? e.getReason() : "HTTP Error", null)
        );
    }

    /**
     * Handle Bean Validation errors (@NotBlank, @NotNull, etc.)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        var errors = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .toList();
        log.warn("Validation error: {}", errors);
        return ResponseEntity.badRequest().body(buildBody("Validation failed", errors));
    }

    /**
     * Catch-all handler — logs full stack trace, returns 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception e) {
        log.error("Unhandled exception [{}]: {}", e.getClass().getSimpleName(), e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            buildBody("An internal server error occurred", null)
        );
    }

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status, String detail, Object errors) {
        return ResponseEntity.status(status).body(buildBody(detail, errors));
    }

    private Map<String, Object> buildBody(String detail, Object errors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("detail", detail);
        body.put("timestamp", OffsetDateTime.now().toString());
        if (errors != null) {
            body.put("errors", errors);
        }
        return body;
    }
}
