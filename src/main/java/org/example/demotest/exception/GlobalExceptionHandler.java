package org.example.demotest.exception;

import org.example.demotest.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;

/**
 * Centralized exception-to-JSON mapping for REST APIs.
 *
 * @author Liang.Xu
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles application-specific exceptions (AppException subclasses).
     * Uses the exception's predefined HTTP status and message to build response.
     *
     * @param ex Caught application exception
     * @param request Incoming HTTP request
     * @return ResponseEntity with error details and appropriate HTTP status
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex, HttpServletRequest request) {
        return buildResponse(ex.getHttpStatus(), ex.getMessage(), request.getRequestURI());
    }

    /**
     * Handles request validation exceptions.
     * Catches missing parameters, constraint violations, and validation errors.
     *
     * @param ex Caught validation exception
     * @param request Incoming HTTP request
     * @return ResponseEntity with error details and 400 BAD REQUEST status
     */
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            ConstraintViolationException.class,
            MethodArgumentNotValidException.class
    })
    public ResponseEntity<ErrorResponse> handleValidationException(Exception ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    /**
     * Handles file upload size limit exceeded exceptions.
     *
     * @param ex Caught file size exception
     * @param request Incoming HTTP request
     * @return ResponseEntity with error details and 400 BAD REQUEST status
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleFileSizeException(MaxUploadSizeExceededException ex,
                                                                 HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Uploaded file exceeds multipart size limit", request.getRequestURI());
    }

    /**
     * Fallback handler for all uncaught exceptions.
     * Returns generic internal server error response.
     *
     * @param ex Caught unknown exception
     * @param request Incoming HTTP request
     * @return ResponseEntity with error details and 500 INTERNAL SERVER ERROR status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknownException(Exception ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI());
    }

    /**
     * Builds standardized ErrorResponse JSON payload.
     *
     * @param status HTTP status for the response
     * @param message Human-readable error message
     * @param path Request path where the error occurred
     * @return ResponseEntity containing the ErrorResponse payload
     */
    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, String path) {
        ErrorResponse payload = new ErrorResponse(
                status.getReasonPhrase(),
                message,
                path,
                System.currentTimeMillis()
        );
        return ResponseEntity.status(status).body(payload);
    }
}
