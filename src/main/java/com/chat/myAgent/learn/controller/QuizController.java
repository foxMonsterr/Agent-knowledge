package com.chat.myAgent.learn.controller;

import com.chat.myAgent.common.result.R;
import com.chat.myAgent.learn.dto.FeynmanEvaluateRequest;
import com.chat.myAgent.learn.dto.QuizEvaluateRequest;
import com.chat.myAgent.learn.dto.QuizGenerateRequest;
import com.chat.myAgent.learn.model.QuizDocument;
import com.chat.myAgent.learn.service.LearnUserService;
import com.chat.myAgent.learn.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/learn")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final LearnUserService learnUserService;

    @PostMapping("/quizzes/generate")
    public R<Map<String, Object>> generate(@RequestBody QuizGenerateRequest request) {
        return R.ok(quizService.generate(learnUserService.currentUserId(), request));
    }

    @PostMapping("/quizzes/evaluate")
    public R<Map<String, Object>> evaluate(@Valid @RequestBody QuizEvaluateRequest request) {
        return R.ok(quizService.evaluate(learnUserService.currentUserId(), request));
    }

    @GetMapping("/quizzes")
    public R<List<QuizDocument>> list(@RequestParam(required = false) String noteId) {
        return R.ok(quizService.list(learnUserService.currentUserId(), noteId));
    }

    @PostMapping("/feynman/evaluate")
    public R<Map<String, Object>> feynman(@Valid @RequestBody FeynmanEvaluateRequest request) {
        return R.ok(quizService.evaluateFeynman(learnUserService.currentUserId(), request));
    }
}
