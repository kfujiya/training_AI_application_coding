package com.quizarena.quiz.service;

import com.quizarena.quiz.client.OpenTriviaDbClient;
import com.quizarena.quiz.dto.AnswerRequest;
import com.quizarena.quiz.dto.AnswerResponse;
import com.quizarena.quiz.dto.ChoiceDto;
import com.quizarena.quiz.dto.QuestionDto;
import com.quizarena.quiz.dto.QuizResultDto;
import com.quizarena.quiz.dto.QuizStartRequest;
import com.quizarena.quiz.dto.QuizStartResponse;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriUtils;

@Service
public class QuizService {
    private static final Logger log = LoggerFactory.getLogger(QuizService.class);
    private final OpenTriviaDbClient client;
    private final ConcurrentHashMap<UUID, QuizSession> sessions = new ConcurrentHashMap<>();

    public QuizService(OpenTriviaDbClient client) {
        this.client = client;
    }

    public QuizStartResponse start(QuizStartRequest request) {
        validateRequest(request);
        try {
            var response = client.fetchQuestions(request.amount(), request.category(), normalizeDifficulty(request.difficulty()));
            if (response == null || response.results() == null) throw new QuizException(502, "問題データを取得できませんでした。");
            if (response.responseCode() != 0) throw responseCodeException(response.responseCode());
            if (response.results().size() != request.amount()) {
                throw new QuizException(502, "指定された問題数を取得できませんでした。条件を変更して再試行してください。");
            }
            UUID quizId = UUID.randomUUID();
            var questions = response.results().stream().map(item -> toStoredQuestion(item)).toList();
            var session = new QuizSession(quizId, questions);
            sessions.put(quizId, session);
            return new QuizStartResponse(quizId, questions.size(), questions.getFirst().publicView(1));
        } catch (QuizException e) {
            throw e;
        } catch (RestClientException e) {
            log.warn("Open Trivia DB request failed", e);
            throw new QuizException(503, "クイズ問題の取得に失敗しました。時間をおいて再試行してください。", e);
        } catch (Exception e) {
            log.error("Unexpected quiz start failure", e);
            throw new QuizException(502, "クイズ問題の処理に失敗しました。", e);
        }
    }

    public AnswerResponse answer(UUID quizId, AnswerRequest request) {
        QuizSession session = sessions.get(quizId);
        if (session == null) throw new QuizException(404, "指定されたクイズが見つかりません。");
        synchronized (session) {
            if (session.completed()) throw new QuizException(409, "このクイズはすでに完了しています。");
            if (request == null || request.questionId() == null || request.selectedChoiceId() == null) {
                throw new QuizException(400, "問題IDと選択肢IDは必須です。");
            }
            var question = session.questions().get(session.currentIndex());
            if (!question.id().equals(request.questionId())) throw new QuizException(409, "現在の問題に対する回答ではありません。");
            var selected = question.choices().stream().filter(c -> c.id().equals(request.selectedChoiceId())).findFirst()
                    .orElseThrow(() -> new QuizException(400, "指定された選択肢が見つかりません。"));
            var correctChoice = question.choices().stream().filter(QuizSession.Choice::correct).findFirst().orElseThrow();
            if (selected.correct()) session.incrementCorrect(); else session.incrementIncorrect();
            session.answer();
            if (session.completed()) {
                return new AnswerResponse(selected.correct(), correctChoice.publicView(), session.correctCount(), session.incorrectCount(), true, null, result(session));
            }
            QuestionDto next = session.questions().get(session.currentIndex()).publicView(session.currentIndex() + 1);
            return new AnswerResponse(selected.correct(), correctChoice.publicView(), session.correctCount(), session.incorrectCount(), false, next, null);
        }
    }

    private QuizSession.StoredQuestion toStoredQuestion(OpenTriviaDbClient.QuestionItem item) {
        String question = decode(item.question());
        var choices = new ArrayList<QuizSession.Choice>();
        choices.add(new QuizSession.Choice(UUID.randomUUID().toString(), decode(item.correctAnswer()), true));
        item.incorrectAnswers().forEach(answer -> choices.add(new QuizSession.Choice(UUID.randomUUID().toString(), decode(answer), false)));
        if (choices.size() != 4) throw new QuizException(502, "取得した問題の選択肢数が不正です。");
        Collections.shuffle(choices);
        return new QuizSession.StoredQuestion(UUID.randomUUID().toString(), decode(item.category()), item.difficulty().toUpperCase(), question, List.copyOf(choices));
    }

    private static String decode(String value) {
        return org.springframework.web.util.HtmlUtils.htmlUnescape(value == null ? "" : value);
    }

    private static QuizResultDto result(QuizSession session) {
        int total = session.questions().size();
        int accuracy = Math.round((float) session.correctCount() / total * 100);
        return new QuizResultDto(total, session.correctCount(), session.incorrectCount(), accuracy);
    }

    private static void validateRequest(QuizStartRequest request) {
        if (request == null || request.amount() == null || !List.of(5, 10, 20).contains(request.amount())) {
            throw new QuizException(400, "問題数は5問、10問、20問のいずれかを指定してください。");
        }
        if (request.category() != null && request.category() <= 0) throw new QuizException(400, "カテゴリ指定が不正です。");
        if (request.difficulty() != null && !List.of("easy", "medium", "hard", "").contains(request.difficulty().toLowerCase())) {
            throw new QuizException(400, "難易度指定が不正です。");
        }
    }

    private static String normalizeDifficulty(String difficulty) {
        return difficulty == null || difficulty.isBlank() ? null : difficulty.toLowerCase();
    }

    private static QuizException responseCodeException(int code) {
        return switch (code) {
            case 1 -> new QuizException(502, "条件に合う問題が不足しています。カテゴリや難易度を変更してください。");
            case 2 -> new QuizException(400, "クイズ設定が不正です。入力内容を確認してください。");
            case 3, 4 -> new QuizException(502, "クイズセッションを準備できませんでした。再試行してください。");
            case 5 -> new QuizException(429, "リクエストが多すぎます。5秒以上待ってから再試行してください。");
            default -> new QuizException(502, "クイズ問題を取得できませんでした。");
        };
    }
}
