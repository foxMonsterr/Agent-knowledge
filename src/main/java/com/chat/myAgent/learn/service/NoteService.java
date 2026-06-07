package com.chat.myAgent.learn.service;

import com.chat.myAgent.learn.dto.CreateNoteRequest;
import com.chat.myAgent.learn.dto.KnowledgeSearchRequest;
import com.chat.myAgent.learn.dto.UpdateNoteRequest;
import com.chat.myAgent.learn.model.KnowledgeNoteDocument;
import com.chat.myAgent.learn.repository.mongo.KnowledgeNoteRepository;
import com.chat.myAgent.rag.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final KnowledgeNoteRepository noteRepository;
    private final DocumentService documentService;
    private final StudyService studyService;

    public KnowledgeNoteDocument create(String userId, CreateNoteRequest request, String sourceType) {
        LocalDateTime now = LocalDateTime.now();
        KnowledgeNoteDocument note = KnowledgeNoteDocument.builder()
                .noteId("note-" + UUID.randomUUID().toString().replace("-", ""))
                .userId(userId)
                .title(request.getTitle().trim())
                .content(request.getContent())
                .summary(summarize(request.getContent()))
                .tags(cleanTags(request.getTags()))
                .category(request.getCategory())
                .sourceType(sourceType == null ? "manual" : sourceType)
                .sourceTraceId(request.getSourceTraceId())
                .relatedNoteIds(new ArrayList<>())
                .masteryLevel(0)
                .reviewCount(0)
                .vectorIndexed(false)
                .archived(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
        KnowledgeNoteDocument saved = noteRepository.save(note);
        if (request.getVectorIndex() == null || Boolean.TRUE.equals(request.getVectorIndex())) {
            saved = syncNoteVectors(saved, false);
        }
        studyService.record(userId, "note_create", saved.getTitle(), saved.getTags(), saved.getCategory(),
                null, saved.getSourceTraceId(), saved.getNoteId(), null, null, 0, null, 0,
                Map.of("sourceType", saved.getSourceType()));
        return saved;
    }

    public KnowledgeNoteDocument update(String userId, String noteId, UpdateNoteRequest request) {
        KnowledgeNoteDocument note = getOwned(userId, noteId);
        boolean reindex = Boolean.TRUE.equals(request.getVectorReindex());
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            note.setTitle(request.getTitle().trim());
            reindex = true;
        }
        if (request.getContent() != null && !request.getContent().isBlank()) {
            note.setContent(request.getContent());
            note.setSummary(request.getSummary() == null ? summarize(request.getContent()) : request.getSummary());
            reindex = true;
        }
        if (request.getTags() != null) {
            note.setTags(cleanTags(request.getTags()));
            reindex = true;
        }
        if (request.getCategory() != null) {
            note.setCategory(request.getCategory());
            reindex = true;
        }
        if (request.getSummary() != null) {
            note.setSummary(request.getSummary());
            reindex = true;
        }
        if (request.getMasteryLevel() != null) {
            note.setMasteryLevel(Math.max(0, Math.min(100, request.getMasteryLevel())));
        }
        if (request.getArchived() != null) {
            note.setArchived(request.getArchived());
            reindex = true;
        }
        note.setUpdatedAt(LocalDateTime.now());
        KnowledgeNoteDocument saved = noteRepository.save(note);
        if (reindex) {
            saved = syncNoteVectors(saved, true);
        }
        studyService.record(userId, "note_update", saved.getTitle(), saved.getTags(), saved.getCategory(),
                null, null, saved.getNoteId(), null, null, 0, null, 0,
                Map.of("archived", saved.getArchived()));
        return saved;
    }

    public KnowledgeNoteDocument archive(String userId, String noteId) {
        UpdateNoteRequest request = new UpdateNoteRequest();
        request.setArchived(true);
        return update(userId, noteId, request);
    }

    public KnowledgeNoteDocument getOwned(String userId, String noteId) {
        return noteRepository.findByNoteIdAndUserId(noteId, userId)
                .orElseThrow(() -> new IllegalArgumentException("笔记不存在或无权访问"));
    }

    public List<KnowledgeNoteDocument> list(String userId, Boolean archived) {
        if (archived == null) {
            return noteRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        }
        return noteRepository.findByUserIdAndArchivedOrderByUpdatedAtDesc(userId, archived);
    }

    public List<Map<String, Object>> search(String userId, KnowledgeSearchRequest request) {
        String query = request.getQuery().toLowerCase(Locale.ROOT);
        int limit = request.getTopK() == null ? 5 : Math.max(1, request.getTopK());
        return noteRepository.findByUserIdAndArchivedOrderByUpdatedAtDesc(userId, false).stream()
                .filter(note -> matches(note, query, request))
                .limit(limit)
                .map(note -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("sourceId", note.getNoteId());
                    item.put("sourceType", "note");
                    item.put("title", note.getTitle());
                    item.put("content", note.getContent());
                    item.put("snippet", snippet(note.getContent(), query));
                    item.put("score", score(note, query));
                    item.put("noteId", note.getNoteId());
                    item.put("tags", note.getTags());
                    item.put("category", note.getCategory());
                    return item;
                })
                .toList();
    }

    public KnowledgeNoteDocument importDocument(String userId, MultipartFile file, String category, List<String> tags) throws IOException {
        DocumentService.DocumentMeta meta = documentService.uploadAndIndex(file, userId, "document", cleanTags(tags), category);
        CreateNoteRequest request = new CreateNoteRequest();
        request.setTitle(meta.getFileName());
        request.setContent(meta.getTextContent() == null || meta.getTextContent().isBlank()
                ? "Imported document: " + meta.getFileName()
                : meta.getTextContent());
        request.setCategory(category);
        request.setTags(tags);
        request.setVectorIndex(false);
        KnowledgeNoteDocument note = create(userId, request, "imported");
        note.setSourceDocId(meta.getDocId());
        note.setVectorIds(new ArrayList<>(meta.getDocumentIds() == null ? List.of() : meta.getDocumentIds()));
        note.setVectorIndexed(!note.getVectorIds().isEmpty());
        note.setUpdatedAt(LocalDateTime.now());
        KnowledgeNoteDocument saved = noteRepository.save(note);
        studyService.record(userId, "document_import", saved.getTitle(), saved.getTags(), saved.getCategory(),
                null, null, saved.getNoteId(), null, null, 0, null, 0,
                Map.of("fileName", meta.getFileName(), "chunkCount", meta.getChunkCount()));
        return saved;
    }

    public String summarize(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String compact = content.replaceAll("\\s+", " ").trim();
        return compact.length() <= 180 ? compact : compact.substring(0, 180) + "...";
    }

    private boolean matches(KnowledgeNoteDocument note, String query, KnowledgeSearchRequest request) {
        if (request.getCategory() != null && !request.getCategory().isBlank()
                && !request.getCategory().equalsIgnoreCase(note.getCategory())) {
            return false;
        }
        if (request.getTags() != null && !request.getTags().isEmpty()
                && (note.getTags() == null || note.getTags().stream().noneMatch(request.getTags()::contains))) {
            return false;
        }
        if (request.getNoteIds() != null && !request.getNoteIds().isEmpty()
                && !request.getNoteIds().contains(note.getNoteId())) {
            return false;
        }
        return contains(note.getTitle(), query) || contains(note.getContent(), query)
                || (note.getTags() != null && note.getTags().stream().anyMatch(tag -> contains(tag, query)));
    }

    private double score(KnowledgeNoteDocument note, String query) {
        if (contains(note.getTitle(), query)) {
            return 0.92;
        }
        if (note.getTags() != null && note.getTags().stream().anyMatch(tag -> contains(tag, query))) {
            return 0.82;
        }
        return 0.65;
    }

    private boolean contains(String text, String query) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(query);
    }

    private String snippet(String content, String query) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String compact = content.replaceAll("\\s+", " ").trim();
        int idx = compact.toLowerCase(Locale.ROOT).indexOf(query);
        if (idx < 0) {
            return compact.length() <= 180 ? compact : compact.substring(0, 180) + "...";
        }
        int start = Math.max(0, idx - 60);
        int end = Math.min(compact.length(), idx + query.length() + 120);
        return (start > 0 ? "..." : "") + compact.substring(start, end) + (end < compact.length() ? "..." : "");
    }

    private List<String> cleanTags(List<String> tags) {
        if (tags == null) {
            return new ArrayList<>();
        }
        return tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private KnowledgeNoteDocument syncNoteVectors(KnowledgeNoteDocument note, boolean deleteOldVectors) {
        if (deleteOldVectors) {
            documentService.deleteVectors(note.getVectorIds());
        }
        if (Boolean.TRUE.equals(note.getArchived())) {
            note.setVectorIds(new ArrayList<>());
            note.setVectorIndexed(false);
            return noteRepository.save(note);
        }
        List<String> vectorIds = documentService.indexNote(note);
        note.setVectorIds(new ArrayList<>(vectorIds));
        note.setVectorIndexed(!vectorIds.isEmpty());
        note.setUpdatedAt(LocalDateTime.now());
        return noteRepository.save(note);
    }
}
