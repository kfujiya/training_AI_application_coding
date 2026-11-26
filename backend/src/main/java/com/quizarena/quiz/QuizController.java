package com.quizarena.quiz;

import com.quizarena.quiz.dto.AnswerRequest;
import com.quizarena.quiz.dto.AnswerResponse;
import com.quizarena.quiz.dto.CategoryDto;
import com.quizarena.quiz.dto.QuizStartRequest;
import com.quizarena.quiz.dto.QuizStartResponse;
import com.quizarena.quiz.service.CategoryService;
import com.quizarena.quiz.service.QuizService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class QuizController {
    private final CategoryService categoryService;
    private final QuizService quizService;

    public QuizController(CategoryService categoryService, QuizService quizService) {
        this.categoryService = categoryService;
        this.quizService = quizService;
    }

    @GetMapping("/categories")
    public List<CategoryDto> categories() {
        return categoryService.getCategories();
    }

    @PostMapping("/quizzes")
    public QuizStartResponse start(@RequestBody QuizStartRequest request) {
        return quizService.start(request);
    }

    @PostMapping("/quizzes/{quizId}/answers")
    public AnswerResponse answer(@PathVariable UUID quizId, @RequestBody AnswerRequest request) {
        return quizService.answer(quizId, request);
    }
}
