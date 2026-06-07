package com.chat.myAgent.learn.react;

import com.chat.myAgent.learn.tool.QuizTool;
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
public class QuizReActTool implements ReActTool {

    private final QuizTool quizTool;

    @Override
    public String name() {
        return "quiz";
    }

    @Override
    public String displayName() {
        return "测验与闪卡工具";
    }

    @Override
    public String category() {
        return "practice";
    }

    @Override
    public boolean supports(ReActRunRequest request) {
        return true;
    }

    @Override
    public Map<String, Object> input(ReActRunRequest request) {
        Map<String, Object> input = new LinkedHashMap<>();
        String message = request.getMessage();
        if (message != null) {
            String lower = message.toLowerCase();
            if (containsAny(lower, "闪卡", "flashcard")) {
                input.put("action", "generate_flashcards");
            } else if (containsAny(lower, "费曼", "feynman")) {
                input.put("action", "evaluate_feynman");
            } else {
                input.put("action", "generate_quiz");
            }
        } else {
            input.put("action", "generate_quiz");
        }
        input.put("count", 5);
        input.put("difficulty", "medium");
        if (request.getNoteIds() != null && !request.getNoteIds().isEmpty()) {
            input.put("noteId", request.getNoteIds().get(0));
        }
        if (message != null && !message.isBlank()) {
            input.put("explanation", message);
        }
        return input;
    }

    @Override
    public ReActToolResult execute(ReActRunRequest request, Map<String, Object> input) {
        String action = String.valueOf(input.getOrDefault("action", "generate_quiz"));
        String noteId = input.get("noteId") instanceof String s ? s : "";
        String explanation = input.get("explanation") instanceof String s ? s : "";
        int count = input.get("count") instanceof Number n ? n.intValue() : 5;
        String difficulty = input.get("difficulty") instanceof String s ? s : "medium";

        try {
            String result = switch (action) {
                case "generate_quiz" -> quizTool.generateQuiz(noteId, count, difficulty);
                case "generate_flashcards" -> quizTool.generateFlashcards(noteId, count);
                case "evaluate_feynman" -> quizTool.evaluateFeynman(noteId, explanation);
                default -> quizTool.generateQuiz(noteId, count, difficulty);
            };

            String observation = switch (action) {
                case "generate_quiz" -> "已生成测验题。";
                case "generate_flashcards" -> "已生成闪卡。";
                case "evaluate_feynman" -> "已完成费曼评估。";
                default -> "测验工具已执行。";
            };

            return ReActToolResult.builder()
                    .observation(observation)
                    .context(action + " 结果：\n" + result)
                    .build();
        } catch (Exception e) {
            log.warn("测验工具执行失败 [{}]: {}", action, e.getMessage());
            return ReActToolResult.builder()
                    .observation("测验工具执行失败: " + e.getMessage())
                    .build();
        }
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
