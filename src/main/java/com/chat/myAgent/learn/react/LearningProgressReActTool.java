package com.chat.myAgent.learn.react;

import com.chat.myAgent.learn.tool.LearningTool;
import com.chat.myAgent.react.core.ReActRunRequest;
import com.chat.myAgent.react.core.ReActTool;
import com.chat.myAgent.react.core.ReActToolResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LearningProgressReActTool implements ReActTool {

    private final LearningTool learningTool;

    @Override
    public String name() {
        return "learning_progress";
    }

    @Override
    public String displayName() {
        return "学习进度与薄弱点分析";
    }

    @Override
    public String category() {
        return "analysis";
    }

    @Override
    public boolean supports(ReActRunRequest request) {
        return true;
    }

    @Override
    public Map<String, Object> input(ReActRunRequest request) {
        return new LinkedHashMap<>();
    }

    @Override
    public ReActToolResult execute(ReActRunRequest request, Map<String, Object> input) {
        try {
            String progress = learningTool.getLearningProgress();
            String weakness = learningTool.analyzeWeakness(5);

            StringBuilder context = new StringBuilder();
            context.append("学习进度总览：\n").append(progress).append("\n\n");
            context.append("薄弱知识点：\n").append(weakness);

            String observation = "已获取学习进度与薄弱点分析数据。";
            return ReActToolResult.builder()
                    .observation(observation)
                    .context(context.toString())
                    .build();
        } catch (Exception e) {
            log.warn("获取学习进度失败: {}", e.getMessage());
            return ReActToolResult.builder()
                    .observation("获取学习进度失败: " + e.getMessage())
                    .build();
        }
    }
}
