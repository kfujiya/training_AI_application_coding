package com.quizarena.quiz.dto;

import java.util.UUID;

public record QuizStartResponse(UUID quizId, int totalQuestions, QuestionDto question) {
}
