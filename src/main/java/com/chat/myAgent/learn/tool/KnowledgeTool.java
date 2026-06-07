package com.chat.myAgent.learn.tool;

import com.chat.myAgent.learn.dto.CreateNoteRequest;
import com.chat.myAgent.learn.dto.KnowledgeSearchRequest;
import com.chat.myAgent.learn.dto.UpdateNoteRequest;
import com.chat.myAgent.learn.model.KnowledgeNoteDocument;
import com.chat.myAgent.learn.service.LearnUserService;
import com.chat.myAgent.learn.service.NoteService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KnowledgeTool {

    private final NoteService noteService;
    private final LearnUserService learnUserService;
    private final ObjectMapper objectMapper;

    @Tool(description = "检索个人知识库中的笔记。输入自然语言查询，返回最相关的笔记片段。")
    public String searchNotes(@ToolParam(description = "检索查询") String query,
                              @ToolParam(description = "返回结果数量") int topK) {
        KnowledgeSearchRequest request = new KnowledgeSearchRequest();
        request.setQuery(query);
        request.setTopK(topK <= 0 ? 5 : topK);
        return toJson(noteService.search(learnUserService.currentUserId(), request));
    }

    @Tool(description = "在个人知识库中创建学习笔记。")
    public String createNote(@ToolParam(description = "笔记标题") String title,
                             @ToolParam(description = "Markdown 笔记内容") String content,
                             @ToolParam(description = "逗号分隔标签") String tags) {
        CreateNoteRequest request = new CreateNoteRequest();
        request.setTitle(title);
        request.setContent(content);
        request.setTags(splitTags(tags));
        KnowledgeNoteDocument note = noteService.create(learnUserService.currentUserId(), request, "agent_generated");
        return toJson(Map.of("noteId", note.getNoteId(), "title", note.getTitle(), "created", true));
    }

    @Tool(description = "更新已有学习笔记的正文。")
    public String updateNote(@ToolParam(description = "笔记ID") String noteId,
                             @ToolParam(description = "新的 Markdown 内容") String content) {
        UpdateNoteRequest request = new UpdateNoteRequest();
        request.setContent(content);
        KnowledgeNoteDocument note = noteService.update(learnUserService.currentUserId(), noteId, request);
        return toJson(Map.of("noteId", note.getNoteId(), "updated", true));
    }

    @Tool(description = "获取指定笔记详情。")
    public String getNoteDetail(@ToolParam(description = "笔记ID") String noteId) {
        return toJson(noteService.getOwned(learnUserService.currentUserId(), noteId));
    }

    private List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(",")).map(String::trim).filter(tag -> !tag.isBlank()).toList();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"工具结果序列化失败\"}";
        }
    }
}
