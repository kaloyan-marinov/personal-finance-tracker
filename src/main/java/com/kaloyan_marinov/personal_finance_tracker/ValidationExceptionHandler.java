package com.kaloyan_marinov.personal_finance_tracker;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidBody(
        MethodArgumentNotValidException e
    ) {
        Map<String, String> fieldErrors = e
            .getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(
                Collectors.toMap(
                    fe -> fe.getField(),
                    fe -> fe.getDefaultMessage(),
                    (first, second) -> first
                )
            );
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                Map.of(
                    "error", "Validation failed",
                    "fieldErrors", fieldErrors
                )
            );
    }
}
