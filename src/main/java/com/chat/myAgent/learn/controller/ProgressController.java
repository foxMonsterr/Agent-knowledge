package com.chat.myAgent.learn.controller;

import com.chat.myAgent.common.result.R;
import com.chat.myAgent.learn.service.LearnUserService;
import com.chat.myAgent.learn.service.StudyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/learn/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final StudyService studyService;
    private final LearnUserService learnUserService;

    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        return R.ok(studyService.overview(learnUserService.currentUserId()));
    }

    @GetMapping("/weakness")
    public R<List<Map<String, Object>>> weakness(@RequestParam(defaultValue = "10") Integer limit) {
        return R.ok(studyService.weakness(learnUserService.currentUserId(), limit));
    }

    @GetMapping("/radar")
    public R<Map<String, Object>> radar() {
        return R.ok(studyService.radar(learnUserService.currentUserId()));
    }

    @GetMapping("/intervention")
    public R<Map<String, Object>> intervention() {
        return R.ok(studyService.intervention(learnUserService.currentUserId()));
    }

    @GetMapping("/recommendations")
    public R<Map<String, Object>> recommendations() {
        return R.ok(studyService.recommendations(learnUserService.currentUserId()));
    }
}
