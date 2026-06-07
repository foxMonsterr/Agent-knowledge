package com.chat.myAgent.learn.controller;

import com.chat.myAgent.common.result.R;
import com.chat.myAgent.learn.dto.FlashcardGenerateRequest;
import com.chat.myAgent.learn.dto.FlashcardReviewRequest;
import com.chat.myAgent.learn.model.FlashcardDocument;
import com.chat.myAgent.learn.service.FlashcardService;
import com.chat.myAgent.learn.service.LearnUserService;
import com.chat.myAgent.learn.service.ReviewSchedulerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/learn/flashcards")
@RequiredArgsConstructor
public class FlashcardController {

    private final FlashcardService flashcardService;
    private final ReviewSchedulerService reviewSchedulerService;
    private final LearnUserService learnUserService;

    @PostMapping("/generate")
    public R<Map<String, Object>> generate(@Valid @RequestBody FlashcardGenerateRequest request) {
        return R.ok(flashcardService.generate(learnUserService.currentUserId(), request));
    }

    @GetMapping("/due")
    public R<List<FlashcardDocument>> due() {
        return R.ok(flashcardService.due(learnUserService.currentUserId()));
    }

    @GetMapping
    public R<List<FlashcardDocument>> list() {
        return R.ok(flashcardService.list(learnUserService.currentUserId()));
    }

    @PostMapping("/{cardId}/review")
    public R<Map<String, Object>> review(@PathVariable String cardId, @Valid @RequestBody FlashcardReviewRequest request) {
        return R.ok(flashcardService.review(learnUserService.currentUserId(), cardId, request));
    }

    @GetMapping("/priority-queue")
    public R<Map<String, Object>> priorityQueue() {
        return R.ok(reviewSchedulerService.priorityQueueStats(learnUserService.currentUserId()));
    }
}
