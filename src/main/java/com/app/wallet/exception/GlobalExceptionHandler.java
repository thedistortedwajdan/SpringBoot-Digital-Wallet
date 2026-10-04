package com.app.wallet.exception;

import com.app.wallet.dto.ApiErrorDto;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDto> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage()));

        ApiErrorDto error = build(HttpStatus.BAD_REQUEST, "Validation failed", request);
        error.setFieldErrors(fieldErrors);

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MissingRequestHeaderException.class
    })
    public ResponseEntity<ApiErrorDto> handleMalformedRequest(
            Exception ex,
            HttpServletRequest request) {

        return respond(HttpStatus.BAD_REQUEST, "Malformed or invalid request", request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorDto> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request) {

        return respond(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler({EmailAlreadyExistsException.class, ConflictException.class})
    public ResponseEntity<ApiErrorDto> handleConflict(
            RuntimeException ex,
            HttpServletRequest request) {

        return respond(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorDto> handleInvalidCredentialsException(
            InvalidCredentialsException ex,
            HttpServletRequest request) {

        return respond(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorDto> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {

        String message = ex instanceof InvalidTokenException
                ? ex.getMessage()
                : "Authentication is required to access this resource";

        return respond(HttpStatus.UNAUTHORIZED, message, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorDto> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        return respond(HttpStatus.FORBIDDEN, "You do not have permission to access this resource", request);
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ApiErrorDto> handleAccountDisabled(
            AccountDisabledException ex,
            HttpServletRequest request) {

        return respond(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler({UserDoesNotExistException.class, ResourceNotFoundException.class})
    public ResponseEntity<ApiErrorDto> handleNotFound(
            RuntimeException ex,
            HttpServletRequest request) {

        return respond(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /**
     * Fallback. Framework exceptions that already carry a status (405, 404 for unknown paths,
     * 415, ...) keep it; anything else is an unexpected 500 and its details are not leaked.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDto> handleUnexpected(
            Exception ex,
            HttpServletRequest request) {

        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            HttpStatus resolved = HttpStatus.resolve(status.value());
            String message = status.is4xxClientError() && resolved != null
                    ? resolved.getReasonPhrase()
                    : "Request failed";
            return respond(status, message, request);
        }

        log.error("unhandled exception on [{}]", request.getRequestURI(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred", request);
    }

    private ResponseEntity<ApiErrorDto> respond(
            HttpStatusCode status,
            String message,
            HttpServletRequest request) {

        return ResponseEntity
                .status(status)
                .body(build(status, message, request));
    }

    private ApiErrorDto build(HttpStatusCode status, String message, HttpServletRequest request) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        String reason = resolved != null ? resolved.getReasonPhrase() : "Error";
        return ApiErrorDto.of(status.value(), reason, message, request.getRequestURI());
    }
}
