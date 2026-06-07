package com.chat.myAgent.learn.controller;

import com.chat.myAgent.common.result.R;
import com.chat.myAgent.learn.dto.CreatePathRequest;
import com.chat.myAgent.learn.dto.UpdateStageRequest;
import com.chat.myAgent.learn.service.LearnUserService;
import com.chat.myAgent.learn.service.LearningPathService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/learn/paths")
@RequiredArgsConstructor
public class LearningPathController {

    private final LearningPathService learningPathService;
    private final LearnUserService learnUserService;

    @PostMapping
    public R<Map<String, Object>> create(@Valid @RequestBody CreatePathRequest request) {
        return R.ok(learningPathService.create(learnUserService.currentUserId(), request));
    }

    @GetMapping
    public R<List<Map<String, Object>>> list() {
        return R.ok(learningPathService.list(learnUserService.currentUserId()));
    }

    @GetMapping("/{pathId}")
    public R<Map<String, Object>> getProgress(@PathVariable String pathId) {
        return R.ok(learningPathService.getProgress(learnUserService.currentUserId(), pathId));
    }

    @PutMapping("/{pathId}/stages")
    public R<Map<String, Object>> updateStage(@PathVariable String pathId, @Valid @RequestBody UpdateStageRequest request) {
        return R.ok(learningPathService.updateStage(learnUserService.currentUserId(), pathId, request));
    }
}
