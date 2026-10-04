package com.app.wallet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorDto {

    public static ApiErrorDto of(int status, String error, String message, String path) {
        ApiErrorDto dto = new ApiErrorDto();
        dto.setTimestamp(LocalDateTime.now());
        dto.setStatus(status);
        dto.setError(error);
        dto.setMessage(message);
        dto.setPath(path);
        return dto;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(Map<String, String> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }

    private Map<String, String> fieldErrors;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    private LocalDateTime timestamp;

    private int status;

    private String error;

    private String message;

    private String path;

    // getters/setters
}
