package org.example.demotest.exception;

import org.springframework.http.HttpStatus;

/**
 * Bad request exception for input validation failures.
 *
 * @author Liang.Xu
 */
public class BadRequestException extends AppException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
