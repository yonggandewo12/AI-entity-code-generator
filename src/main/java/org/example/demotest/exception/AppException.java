package org.example.demotest.exception;

import org.springframework.http.HttpStatus;

/**
 * Base application exception carrying HTTP status.
 *
 * @author Liang.Xu
 */
public class AppException extends RuntimeException {

    private final HttpStatus httpStatus;

    public AppException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
