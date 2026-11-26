package com.quizarena.quiz.dto;

public record QuizResultDto(int totalQuestions, int correctCount, int incorrectCount, int accuracyPercent) {
}
