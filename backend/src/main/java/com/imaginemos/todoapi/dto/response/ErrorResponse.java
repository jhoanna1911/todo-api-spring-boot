package com.imaginemos.todoapi.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> errors
) {
    public record FieldError(String field, String message) {}

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, null);
    }

    public static ErrorResponse of(int status, String error, String message, String path,
                                   List<FieldError> errors) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, errors);
    }
}
