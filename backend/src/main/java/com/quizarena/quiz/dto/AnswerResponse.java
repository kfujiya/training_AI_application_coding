package com.quizarena.quiz.dto;

public record AnswerResponse(
        boolean correct,
        ChoiceDto correctChoice,
        int correctCount,
        int incorrectCount,
        boolean completed,
        QuestionDto nextQuestion,
        QuizResultDto result) {
}
