package com.chat.myAgent.learn.service;

import com.chat.myAgent.learn.dto.CreatePathRequest;
import com.chat.myAgent.learn.dto.UpdateStageRequest;
import com.chat.myAgent.learn.model.KnowledgeNoteDocument;
import com.chat.myAgent.learn.model.LearningPathDocument;
import com.chat.myAgent.learn.model.LearningPathDocument.LearningStageDocument;
import com.chat.myAgent.learn.repository.mongo.KnowledgeNoteRepository;
import com.chat.myAgent.learn.repository.mongo.LearningPathRepository;
import com.chat.myAgent.rag.RetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningPathService {

    private final LearningPathRepository pathRepository;
    private final KnowledgeNoteRepository noteRepository;
    private final RetrievalService retrievalService;

    @Qualifier("baseChatClient")
    private final ChatClient baseChatClient;

    public Map<String, Object> create(String userId, CreatePathRequest request) {
        List<KnowledgeNoteDocument> notes = noteRepository.findByUserIdAndArchivedOrderByUpdatedAtDesc(userId, false);
        String context = buildKnowledgeContext(notes, request.getTopic());
        String prompt = "根据以下知识库上下文，为学习主题「" + request.getTopic()
                + "」生成一个包含 3-5 个阶段的学习路径。每个阶段包含 title、description、action(review/quiz/create/flashcard)。\n" + context;
        String llmOutput = baseChatClient.prompt().user(prompt).call().content();

        List<LearningStageDocument> stages = parseStages(llmOutput, request.getTopic());
        String pathId = "path-" + UUID.randomUUID().toString().replace("-", "");
        LearningPathDocument path = LearningPathDocument.builder()
                .pathId(pathId)
                .userId(userId)
                .topic(request.getTopic())
                .targetNoteCount(request.getTargetNoteCount())
                .preferredDepth(request.getPreferredDepth())
                .status("active")
                .completedStages(0)
                .totalStages(stages.size())
                .stages(stages)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        pathRepository.save(path);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pathId", path.getPathId());
        result.put("topic", path.getTopic());
        result.put("stages", stages);
        result.put("totalStages", stages.size());
        result.put("created", true);
        return result;
    }

    public Map<String, Object> updateStage(String userId, String pathId, UpdateStageRequest request) {
        LearningPathDocument path = pathRepository.findByPathIdAndUserId(pathId, userId)
                .orElseThrow(() -> new IllegalArgumentException("学习路径不存在或无权访问"));
        path.getStages().stream()
                .filter(stage -> stage.getStageId().equals(request.getStageId()))
                .findFirst()
                .ifPresent(stage -> {
                    stage.setStatus(request.getStatus() != null ? request.getStatus() : "completed");
                    stage.setScore(request.getScore() != null ? request.getScore() : stage.getScore());
                    stage.setCompletedAt(LocalDateTime.now());
                });
        long completed = path.getStages().stream()
                .filter(stage -> "completed".equals(stage.getStatus()))
                .count();
        path.setCompletedStages((int) completed);
        if (completed >= path.getTotalStages()) {
            path.setStatus("completed");
        }
        path.setUpdatedAt(LocalDateTime.now());
        pathRepository.save(path);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pathId", path.getPathId());
        result.put("completedStages", path.getCompletedStages());
        result.put("totalStages", path.getTotalStages());
        result.put("status", path.getStatus());
        return result;
    }

    public Map<String, Object> getProgress(String userId, String pathId) {
        LearningPathDocument path = pathRepository.findByPathIdAndUserId(pathId, userId)
                .orElseThrow(() -> new IllegalArgumentException("学习路径不存在或无权访问"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pathId", path.getPathId());
        result.put("topic", path.getTopic());
        result.put("status", path.getStatus());
        result.put("completedStages", path.getCompletedStages());
        result.put("totalStages", path.getTotalStages());
        result.put("progress", path.getTotalStages() > 0
                ? Math.round(path.getCompletedStages() * 100.0 / path.getTotalStages()) : 0);
        result.put("stages", path.getStages());
        return result;
    }

    public List<Map<String, Object>> list(String userId) {
        List<LearningPathDocument> paths = pathRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (LearningPathDocument path : paths) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("pathId", path.getPathId());
            item.put("topic", path.getTopic());
            item.put("status", path.getStatus());
            item.put("completedStages", path.getCompletedStages());
            item.put("totalStages", path.getTotalStages());
            item.put("createdAt", path.getCreatedAt());
            result.add(item);
        }
        return result;
    }

    @EventListener
    public void onStageCompleted(StageCompletedEvent event) {
        List<LearningPathDocument> activePaths = pathRepository.findByUserIdAndStatusOrderByCreatedAtDesc(event.userId(), "active");
        for (LearningPathDocument path : activePaths) {
            for (LearningStageDocument stage : path.getStages()) {
                if ("pending".equals(stage.getStatus()) && "quiz".equals(stage.getAction())) {
                    stage.setStatus("completed");
                    stage.setScore(event.score());
                    stage.setResourceNoteId(event.noteId());
                    stage.setCompletedAt(LocalDateTime.now());
                    long completed = path.getStages().stream()
                            .filter(s -> "completed".equals(s.getStatus()))
                            .count();
                    path.setCompletedStages((int) completed);
                    if (completed >= path.getTotalStages()) {
                        path.setStatus("completed");
                    }
                    path.setUpdatedAt(LocalDateTime.now());
                    pathRepository.save(path);
                    log.info("学习路径阶段自动完成: pathId={}, stageId={}, noteId={}",
                            path.getPathId(), stage.getStageId(), event.noteId());
                    return;
                }
            }
        }
    }

    private String buildKnowledgeContext(List<KnowledgeNoteDocument> notes, String topic) {
        StringBuilder sb = new StringBuilder();
        sb.append("已有笔记主题：");
        for (KnowledgeNoteDocument note : notes) {
            sb.append(note.getTitle()).append("（掌握度: ").append(value(note.getMasteryLevel())).append("）, ");
        }
        sb.append("\n\n");
        try {
            var docs = retrievalService.retrieve(topic);
            if (!docs.isEmpty()) {
                sb.append("知识库相关内容：\n");
                for (int i = 0; i < Math.min(docs.size(), 3); i++) {
                    sb.append(docs.get(i).getText()).append("\n\n");
                }
            }
        } catch (Exception e) {
            sb.append("(知识库检索暂时不可用)\n");
        }
        return sb.toString();
    }

    private List<LearningStageDocument> parseStages(String llmOutput, String topic) {
        List<LearningStageDocument> stages = new ArrayList<>();
        String[] lines = llmOutput.split("\n");
        int order = 0;
        String currentTitle = null;
        StringBuilder currentDesc = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.matches("^\\d+[.)\\-].*") || trimmed.startsWith("阶段") || trimmed.startsWith("Step")) {
                if (currentTitle != null) {
                    stages.add(buildStage(order++, currentTitle, currentDesc.toString()));
                }
                currentTitle = trimmed.replaceAll("^\\d+[.)\\-]\\s*", "").replaceAll("^阶段\\d+[：:]\\s*", "");
                if (currentTitle.length() > 60) {
                    currentTitle = currentTitle.substring(0, 60) + "...";
                }
                currentDesc = new StringBuilder();
            } else if (!trimmed.isBlank() && currentTitle != null) {
                currentDesc.append(trimmed).append(" ");
            }
        }
        if (currentTitle != null) {
            stages.add(buildStage(order, currentTitle, currentDesc.toString()));
        }
        if (stages.isEmpty()) {
            stages.add(buildStage(0, "复习笔记: " + topic, "回顾与主题相关的已有笔记内容"));
            stages.add(buildStage(1, "生成测验: " + topic, "基于已有笔记生成练习题巩固知识点"));
            stages.add(buildStage(2, "创建闪卡: " + topic, "将关键概念制作成复习闪卡"));
        }
        return stages;
    }

    private LearningStageDocument buildStage(int order, String title, String description) {
        String action = "review";
        if (containsAny(title, "测验", "quiz")) action = "quiz";
        else if (containsAny(title, "闪卡", "flashcard")) action = "flashcard";
        else if (containsAny(title, "创建", "笔记", "create", "note")) action = "create";
        return LearningStageDocument.builder()
                .stageId("stage-" + UUID.randomUUID().toString().replace("-", ""))
                .order(order)
                .title(title)
                .description(description.isBlank() ? title : description.trim())
                .action(action)
                .status("pending")
                .score(0)
                .build();
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        for (String kw : keywords) {
            if (lower.contains(kw)) return true;
        }
        return false;
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    public record StageCompletedEvent(String userId, String noteId, int score) {}
}
