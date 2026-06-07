package com.chat.myAgent.learn.tool;

import com.chat.myAgent.learn.dto.FeynmanEvaluateRequest;
import com.chat.myAgent.learn.dto.FlashcardGenerateRequest;
import com.chat.myAgent.learn.dto.QuizEvaluateRequest;
import com.chat.myAgent.learn.dto.QuizGenerateRequest;
import com.chat.myAgent.learn.service.FlashcardService;
import com.chat.myAgent.learn.service.LearnUserService;
import com.chat.myAgent.learn.service.QuizService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuizTool {

    private final QuizService quizService;
    private final FlashcardService flashcardService;
    private final LearnUserService learnUserService;
    private final ObjectMapper objectMapper;

    @Tool(description = "根据指定笔记生成测验题。")
    public String generateQuiz(@ToolParam(description = "笔记ID") String noteId,
                               @ToolParam(description = "题目数量") int count,
                               @ToolParam(description = "难度 easy/medium/hard") String difficulty) {
        QuizGenerateRequest request = new QuizGenerateRequest();
        request.setNoteId(noteId);
        request.setCount(count);
        request.setDifficulty(difficulty);
        return toJson(quizService.generate(learnUserService.currentUserId(), request));
    }

    @Tool(description = "评估用户对测验题的回答。")
    public String evaluateAnswer(@ToolParam(description = "测验题ID") String quizId,
                                 @ToolParam(description = "用户答案") String userAnswer) {
        QuizEvaluateRequest request = new QuizEvaluateRequest();
        request.setQuizId(quizId);
        request.setUserAnswer(userAnswer);
        return toJson(quizService.evaluate(learnUserService.currentUserId(), request));
    }

    @Tool(description = "根据笔记内容生成闪卡。")
    public String generateFlashcards(@ToolParam(description = "笔记ID") String noteId,
                                     @ToolParam(description = "生成数量") int count) {
        FlashcardGenerateRequest request = new FlashcardGenerateRequest();
        request.setNoteId(noteId);
        request.setCount(count);
        return toJson(flashcardService.generate(learnUserService.currentUserId(), request));
    }

    @Tool(description = "费曼检验：评估用户用自己的话解释某个概念的质量。")
    public String evaluateFeynman(@ToolParam(description = "笔记ID") String noteId,
                                  @ToolParam(description = "用户解释") String explanation) {
        FeynmanEvaluateRequest request = new FeynmanEvaluateRequest();
        request.setNoteId(noteId);
        request.setExplanation(explanation);
        return toJson(quizService.evaluateFeynman(learnUserService.currentUserId(), request));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"工具结果序列化失败\"}";
        }
    }
}
