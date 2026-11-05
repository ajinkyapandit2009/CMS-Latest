package com.cmsbackend.registration;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.cmsbackend.controller.UserController;

@RestControllerAdvice(assignableTypes = UserController.class)
public class RegistrationErrorHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalidFields(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(Map.of("message", "Please correct the registration fields.",
                "fieldErrors", fields));
    }

    @ExceptionHandler(RegistrationException.class)
    public ResponseEntity<Map<String, Object>> registrationError(RegistrationException exception) {
        return ResponseEntity.status(exception.status()).cacheControl(CacheControl.noStore()).body(Map.of(
                "message", exception.getMessage(), "fieldErrors", Map.of(exception.field(), exception.getMessage())));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> invalidBody() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).cacheControl(CacheControl.noStore()).body(Map.of(
                "message", "Enter valid registration details.", "fieldErrors", Map.of("request", "Invalid request body")));
    }
}
