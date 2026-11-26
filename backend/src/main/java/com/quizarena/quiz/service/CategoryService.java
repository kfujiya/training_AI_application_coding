package com.quizarena.quiz.service;

import com.quizarena.quiz.client.OpenTriviaDbClient;
import com.quizarena.quiz.dto.CategoryDto;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private final OpenTriviaDbClient client;
    private final AtomicReference<Cache> cache = new AtomicReference<>();

    public CategoryService(OpenTriviaDbClient client) {
        this.client = client;
    }

    public List<CategoryDto> getCategories() {
        Cache current = cache.get();
        if (current != null && current.expiresAt().isAfter(Instant.now())) return current.categories();
        try {
            var response = client.fetchCategories();
            if (response == null || response.triviaCategories() == null) {
                throw new QuizException(502, "カテゴリ情報を取得できませんでした。");
            }
            var categories = response.triviaCategories().stream()
                    .map(item -> new CategoryDto(item.id(), item.name()))
                    .toList();
            cache.set(new Cache(categories, Instant.now().plus(CACHE_TTL)));
            return categories;
        } catch (QuizException e) {
            throw e;
        } catch (Exception e) {
            throw new QuizException(503, "カテゴリ情報の取得に失敗しました。時間をおいて再試行してください。", e);
        }
    }

    private record Cache(List<CategoryDto> categories, Instant expiresAt) {
    }
}
