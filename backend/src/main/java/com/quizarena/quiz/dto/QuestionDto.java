package com.quizarena.quiz.dto;

import java.util.List;

public record QuestionDto(
        String id,
        int number,
        String category,
        String difficulty,
        String question,
        List<ChoiceDto> choices) {
}
