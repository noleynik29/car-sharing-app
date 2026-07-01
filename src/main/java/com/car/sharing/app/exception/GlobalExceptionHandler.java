package com.car.sharing.app.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import com.car.sharing.app.exception.car.CarNotAvailableException;
import com.car.sharing.app.exception.payment.PaymentProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Object> handleEntityNotFound(EntityNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND,
                ex.getMessage());
    }

    @ExceptionHandler(RegistrationException.class)
    public ResponseEntity<Object> handleRegistration(RegistrationException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT,
                ex.getMessage());
    }

    @ExceptionHandler(CarNotAvailableException.class)
    public ResponseEntity<Object> handleCarNotAvailableException(CarNotAvailableException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST,
                ex.getMessage());
    }

    @ExceptionHandler(PaymentProcessingException.class)
    public ResponseEntity<Object> handlePaymentProcessingException(PaymentProcessingException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST,
                ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleBadCredentialsException(AuthenticationException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED,
                ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationException(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors()
                .forEach(error ->
                        errors.put(error.getField(), error.getDefaultMessage())
                );

        return buildErrorResponse(HttpStatus.BAD_REQUEST, errors.toString());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN,
                ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
    }

    private ResponseEntity<Object> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }
}
