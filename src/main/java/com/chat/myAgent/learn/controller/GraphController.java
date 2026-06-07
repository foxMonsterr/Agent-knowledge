package com.chat.myAgent.learn.controller;

import com.chat.myAgent.common.result.R;
import com.chat.myAgent.learn.service.GraphService;
import com.chat.myAgent.learn.service.LearnUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/learn/graph")
@RequiredArgsConstructor
public class GraphController {

    private final GraphService graphService;
    private final LearnUserService learnUserService;

    @GetMapping
    public R<Map<String, Object>> graph(@RequestParam(required = false) String category,
                                        @RequestParam(defaultValue = "200") Integer maxNodes) {
        return R.ok(graphService.graph(learnUserService.currentUserId(), category, maxNodes));
    }

    @GetMapping("/around/{noteId}")
    public R<Map<String, Object>> around(@PathVariable String noteId,
                                         @RequestParam(defaultValue = "2") Integer depth) {
        return R.ok(graphService.around(learnUserService.currentUserId(), noteId, depth));
    }
}
