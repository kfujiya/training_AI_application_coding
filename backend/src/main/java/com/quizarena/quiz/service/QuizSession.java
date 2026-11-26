package com.quizarena.quiz.service;

import com.quizarena.quiz.dto.ChoiceDto;
import com.quizarena.quiz.dto.QuestionDto;
import java.util.List;
import java.util.UUID;

final class QuizSession {
    private final UUID id;
    private final List<StoredQuestion> questions;
    private int currentIndex;
    private int correctCount;
    private int incorrectCount;
    private boolean completed;

    QuizSession(UUID id, List<StoredQuestion> questions) {
        this.id = id;
        this.questions = questions;
    }

    UUID id() { return id; }
    List<StoredQuestion> questions() { return questions; }
    int currentIndex() { return currentIndex; }
    int correctCount() { return correctCount; }
    int incorrectCount() { return incorrectCount; }
    boolean completed() { return completed; }

    void answer() {
        currentIndex++;
        if (currentIndex >= questions.size()) completed = true;
    }

    void incrementCorrect() { correctCount++; }
    void incrementIncorrect() { incorrectCount++; }

    record StoredQuestion(String id, String category, String difficulty, String question, List<Choice> choices) {
        QuestionDto publicView(int number) {
            return new QuestionDto(id, number, category, difficulty, question,
                    choices.stream().map(choice -> new ChoiceDto(choice.id(), choice.text())).toList());
        }
    }

    record Choice(String id, String text, boolean correct) {
        ChoiceDto publicView() { return new ChoiceDto(id, text); }
    }
}
