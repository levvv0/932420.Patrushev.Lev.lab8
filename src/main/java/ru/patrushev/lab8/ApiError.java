package ru.patrushev.lab8;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiError {
    public static class Problem extends RuntimeException {
        private final HttpStatus status;

        public Problem(HttpStatus status, String message) {
            super(message);
            this.status = status;
        }
    }

    @ExceptionHandler(Problem.class)
    public ResponseEntity<Map<String, String>> handle(Problem error) {
        return ResponseEntity.status(error.status).body(Map.of("error", error.getMessage()));
    }
}
