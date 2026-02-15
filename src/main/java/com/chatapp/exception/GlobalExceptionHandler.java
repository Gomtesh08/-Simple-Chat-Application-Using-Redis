package com.chatapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateRoomException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateRoom(DuplicateRoomException exception) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), null);
    }

    @ExceptionHandler(RoomNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRoomNotFound(RoomNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(fieldError -> errors.put(fieldError.getField(), fieldError.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception exception) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, Map<String, String> validationErrors) {
        ErrorResponse error = new ErrorResponse("error", message, Instant.now(), validationErrors);
        return ResponseEntity.status(status).body(error);
    }
}
