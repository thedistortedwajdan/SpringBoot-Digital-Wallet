package com.app.wallet.exception;

import com.app.wallet.dto.ApiErrorDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDto> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        ApiErrorDto error = new ApiErrorDto();

        error.setTimestamp(LocalDateTime.now());

        error.setStatus(HttpStatus.BAD_REQUEST.value());

        error.setError(HttpStatus.BAD_REQUEST.getReasonPhrase());

        error.setMessage("Validation failed");

        error.setPath(request.getRequestURI());

        return ResponseEntity
                .badRequest()
                .body(error);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiErrorDto> handleEmailAlreadyExistsException(
            EmailAlreadyExistsException ex,
            HttpServletRequest request) {

        ApiErrorDto error = new ApiErrorDto();

        error.setTimestamp(LocalDateTime.now());

        error.setStatus(HttpStatus.CONFLICT.value());

        error.setError(HttpStatus.CONFLICT.getReasonPhrase());

        error.setMessage(ex.getMessage());

        error.setPath(request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

}
