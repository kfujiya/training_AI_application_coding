package com.quizarena.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.quizarena.quiz.client.OpenTriviaDbClient;
import com.quizarena.quiz.dto.AnswerRequest;
import com.quizarena.quiz.dto.QuizStartRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {
    @Mock OpenTriviaDbClient client;
    private QuizService service;

    @BeforeEach
    void setUp() { service = new QuizService(client); }

    @Test
    void startsQuizWithoutExposingCorrectAnswer() {
        when(client.fetchQuestions(5, null, null)).thenReturn(response(5));
        var started = service.start(new QuizStartRequest(5, null, null));

        assertThat(started.totalQuestions()).isEqualTo(5);
        assertThat(started.question().choices()).hasSize(4);
        assertThat(started.question().toString()).doesNotContain("correct");
    }

    @Test
    void scoresCorrectAndRejectsDuplicateAnswer() {
        when(client.fetchQuestions(5, null, null)).thenReturn(response(5));
        var started = service.start(new QuizStartRequest(5, null, null));
        var correctChoice = response(5).results().getFirst().correctAnswer();
        var choice = started.question().choices().stream().filter(c -> c.text().equals(correctChoice)).findFirst().orElseThrow();

        var answered = service.answer(started.quizId(), new AnswerRequest(started.question().id(), choice.id()));
        assertThat(answered.correct()).isTrue();
        assertThat(answered.correctCount()).isEqualTo(1);
        assertThatThrownBy(() -> service.answer(started.quizId(), new AnswerRequest(started.question().id(), choice.id())))
                .isInstanceOf(QuizException.class)
                .hasMessageContaining("現在の問題");
    }

    @Test
    void mapsExternalApiProblemCode() {
        when(client.fetchQuestions(5, null, null)).thenReturn(new OpenTriviaDbClient.QuestionApiResponse(1, List.of()));
        assertThatThrownBy(() -> service.start(new QuizStartRequest(5, null, null)))
                .isInstanceOf(QuizException.class)
                .hasMessageContaining("不足");
    }

    private static OpenTriviaDbClient.QuestionApiResponse response(int amount) {
        var items = java.util.stream.IntStream.range(0, amount)
                .mapToObj(i -> new OpenTriviaDbClient.QuestionItem("General Knowledge", "multiple", "easy", "Question &quot;" + i, "Correct", List.of("Wrong A", "Wrong B", "Wrong C")))
                .toList();
        return new OpenTriviaDbClient.QuestionApiResponse(0, items);
    }
}
