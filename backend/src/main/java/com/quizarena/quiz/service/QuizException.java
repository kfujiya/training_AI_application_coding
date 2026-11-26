package com.quizarena.quiz.service;

public class QuizException extends RuntimeException {
    private final int status;

    public QuizException(int status, String message) {
        super(message);
        this.status = status;
    }

    public QuizException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
