package com.solvex.api.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<Map<String, String>> notFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> badRequest(RuntimeException exception) {
        String message = exception.getMessage() == null || exception.getMessage().isBlank()
                ? "Solicitud invÃ¡lida" : exception.getMessage();
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<Map<String, String>> unauthorized(BadCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst().orElse("Datos inválidos");
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, String>> dataServiceUnavailable(IllegalStateException exception) {
        boolean quotaExceeded = hasQuotaError(exception);
        String message = quotaExceeded
                ? "Firestore agotó temporalmente su cuota de uso. La autenticación y los datos volverán a estar disponibles cuando se restablezca o amplíe la cuota."
                : "El servicio de datos no está disponible temporalmente. Intenta de nuevo más tarde.";
        return ResponseEntity.status(quotaExceeded ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", message));
    }

    private boolean hasQuotaError(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            String details = (cause.getClass().getName() + " " + cause.getMessage()).toLowerCase();
            if (details.contains("resource_exhausted") || details.contains("quota exceeded")) return true;
            cause = cause.getCause();
        }
        return false;
    }
}
