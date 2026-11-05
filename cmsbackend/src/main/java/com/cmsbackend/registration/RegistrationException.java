package com.cmsbackend.registration;

import org.springframework.http.HttpStatus;

public class RegistrationException extends RuntimeException {
    private final HttpStatus status;
    private final String field;

    public RegistrationException(HttpStatus status, String field, String message) {
        super(message);
        this.status = status;
        this.field = field;
    }

    public HttpStatus status() { return status; }
    public String field() { return field; }
}
