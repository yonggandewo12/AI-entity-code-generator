package org.example.demotest.exception;

import org.springframework.http.HttpStatus;

/**
 * Server-side configuration exception.
 *
 * @author Liang.Xu
 */
public class ConfigurationException extends AppException {

    public ConfigurationException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
