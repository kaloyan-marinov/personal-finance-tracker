package com.kaloyan_marinov.personal_finance_tracker;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    /** 
     * This is thrown when the request body cannot be deserialized
     * (such as malformed JSON or a value of the wrong type).
     * The exception's message is deliberately not echoed,
     * because doing so would expose internal details (such as class names).
    */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(
        HttpMessageNotReadableException e
    ) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                Map.of(
                    "error", "Malformed request body"
                )
            );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameter(
        MissingServletRequestParameterException e
    ) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                Map.of(
                    "error", "Validation failed",
                    "fieldErrors", Map.of(e.getParameterName(), "must be provided")
                )
            );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
        MethodArgumentTypeMismatchException e
    ) {
        String expectedType = e.getRequiredType() == null
            ? "a valid value"
            : "of type " + e.getRequiredType().getSimpleName();

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                Map.of(
                    "error", "Validation failed",
                    "fieldErrors", Map.of(e.getName(), "must be " + expectedType)
                )
            );
    }

    // This is thrown when a constraint (such as `@Pattern`) on a `@RequestParam` or `@PathVariable` is violated.
    // Once such a constraint is present, Spring validates every argument of that method this way,
    // so an invalid `@Valid @RequestBody` of the same method ends up here as well (rather than in `handleInvalidBody`);
    // its errors are reported per field, as `handleInvalidBody` does.
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidParameter(
        HandlerMethodValidationException e
    ) {
        Stream<Map.Entry<String, String>> valueErrors = e
            .getValueResults()
            .stream()
            .map(
                pvr -> Map.entry(
                    pvr.getMethodParameter().getParameterName(),
                    pvr.getResolvableErrors().get(0).getDefaultMessage()
                )
            );

        Stream<Map.Entry<String, String>> beanErrors = e
            .getBeanResults()
            .stream()
            .flatMap(pe -> pe.getFieldErrors().stream())
            .map(
                fe -> Map.entry(
                    fe.getField(),
                    fe.getDefaultMessage()
                )
            );

        Map<String, String> fieldErrors = Stream
            .concat(valueErrors, beanErrors)
            .collect(
                Collectors.toMap(
                    entry -> entry.getKey(),
                    entry -> entry.getValue(),
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
