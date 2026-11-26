package com.quizarena;

import com.quizarena.quiz.service.QuizException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(QuizException.class)
    public ResponseEntity<Map<String, Object>> handleQuizException(QuizException exception) {
        return ResponseEntity.status(exception.status()).body(Map.of(
                "message", exception.getMessage(),
                "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(Exception exception) {
        return ResponseEntity.internalServerError().body(Map.of(
                "message", "予期しないエラーが発生しました。",
                "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, Object>> handleInvalidRequest(Exception exception) {
        return ResponseEntity.badRequest().body(Map.of(
                "message", "リクエストの形式が不正です。",
                "timestamp", Instant.now().toString()));
    }
}
