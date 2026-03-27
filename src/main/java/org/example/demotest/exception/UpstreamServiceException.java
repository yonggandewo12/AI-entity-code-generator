package org.example.demotest.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception used when the upstream LLM API fails.
 *
 * @author Liang.Xu
 */
public class UpstreamServiceException extends AppException {

    public UpstreamServiceException(String message) {
        super(HttpStatus.BAD_GATEWAY, message);
    }
}
