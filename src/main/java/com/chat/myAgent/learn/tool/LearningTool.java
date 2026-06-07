package com.chat.myAgent.learn.tool;

import com.chat.myAgent.learn.service.LearnUserService;
import com.chat.myAgent.learn.service.StudyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LearningTool {

    private final StudyService studyService;
    private final LearnUserService learnUserService;
    private final ObjectMapper objectMapper;

    @Tool(description = "查询当前用户学习进度总览。")
    public String getLearningProgress() {
        return toJson(studyService.overview(learnUserService.currentUserId()));
    }

    @Tool(description = "分析当前用户的薄弱知识点。")
    public String analyzeWeakness(@ToolParam(description = "返回数量") int limit) {
        return toJson(studyService.weakness(learnUserService.currentUserId(), limit <= 0 ? 10 : limit));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"工具结果序列化失败\"}";
        }
    }
}
