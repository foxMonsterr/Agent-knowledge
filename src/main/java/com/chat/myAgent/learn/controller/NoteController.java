package com.chat.myAgent.learn.controller;

import com.chat.myAgent.common.result.R;
import com.chat.myAgent.learn.dto.CreateNoteRequest;
import com.chat.myAgent.learn.dto.KnowledgeSearchRequest;
import com.chat.myAgent.learn.dto.UpdateNoteRequest;
import com.chat.myAgent.learn.model.KnowledgeNoteDocument;
import com.chat.myAgent.learn.service.LearnUserService;
import com.chat.myAgent.learn.service.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/learn/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;
    private final LearnUserService learnUserService;

    @PostMapping
    public R<KnowledgeNoteDocument> create(@Valid @RequestBody CreateNoteRequest request) {
        return R.ok("笔记创建成功", noteService.create(learnUserService.currentUserId(), request, "manual"));
    }

    @GetMapping
    public R<List<KnowledgeNoteDocument>> list(@RequestParam(required = false) Boolean archived) {
        return R.ok(noteService.list(learnUserService.currentUserId(), archived == null ? false : archived));
    }

    @GetMapping("/{noteId}")
    public R<KnowledgeNoteDocument> detail(@PathVariable String noteId) {
        return R.ok(noteService.getOwned(learnUserService.currentUserId(), noteId));
    }

    @PutMapping("/{noteId}")
    public R<KnowledgeNoteDocument> update(@PathVariable String noteId, @RequestBody UpdateNoteRequest request) {
        return R.ok(noteService.update(learnUserService.currentUserId(), noteId, request));
    }

    @DeleteMapping("/{noteId}")
    public R<KnowledgeNoteDocument> archive(@PathVariable String noteId) {
        return R.ok("笔记已归档", noteService.archive(learnUserService.currentUserId(), noteId));
    }

    @PostMapping("/search")
    public R<List<Map<String, Object>>> search(@Valid @RequestBody KnowledgeSearchRequest request) {
        return R.ok(noteService.search(learnUserService.currentUserId(), request));
    }

    @PostMapping("/import")
    public R<KnowledgeNoteDocument> importDocument(@RequestParam("file") MultipartFile file,
                                                  @RequestParam(required = false) String category,
                                                  @RequestParam(required = false) String tags) throws IOException {
        return R.ok("导入成功", noteService.importDocument(learnUserService.currentUserId(), file, category, parseTags(tags)));
    }

    private List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .toList();
    }
}
