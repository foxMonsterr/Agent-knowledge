package com.chat.myAgent.learn.service;

import com.chat.myAgent.learn.model.KnowledgeNoteDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GraphService {

    private final NoteService noteService;

    public Map<String, Object> graph(String userId, String category, int maxNodes) {
        List<KnowledgeNoteDocument> notes = noteService.list(userId, false).stream()
                .filter(note -> category == null || category.isBlank() || category.equals(note.getCategory()))
                .limit(Math.max(1, maxNodes))
                .toList();
        return buildGraph(userId, notes, Map.of("category", category == null ? "" : category, "maxNodes", maxNodes));
    }

    public Map<String, Object> around(String userId, String noteId, int depth) {
        KnowledgeNoteDocument root = noteService.getOwned(userId, noteId);
        Set<String> related = new HashSet<>(root.getRelatedNoteIds() == null ? List.of() : root.getRelatedNoteIds());
        List<KnowledgeNoteDocument> notes = new ArrayList<>();
        notes.add(root);
        noteService.list(userId, false).stream()
                .filter(note -> related.contains(note.getNoteId()) || shareTag(root, note))
                .limit(Math.max(5, depth * 10L))
                .forEach(notes::add);
        return buildGraph(userId, notes.stream().distinct().toList(), Map.of("around", noteId, "depth", depth));
    }

    private Map<String, Object> buildGraph(String userId, List<KnowledgeNoteDocument> notes, Map<String, Object> filters) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        Set<String> addedTags = new HashSet<>();

        for (KnowledgeNoteDocument note : notes) {
            nodes.add(node(note));
            if (note.getCategory() != null && !note.getCategory().isBlank()) {
                String categoryId = "category-" + note.getCategory();
                if (addedTags.add(categoryId)) {
                    nodes.add(categoryNode(categoryId, note.getCategory()));
                }
                edges.add(edge("edge-category-" + note.getNoteId(), note.getNoteId(), categoryId, "category", 0.3));
            }
            if (note.getTags() != null) {
                for (String tag : note.getTags()) {
                    String tagId = "tag-" + tag;
                    if (addedTags.add(tagId)) {
                        nodes.add(tagNode(tagId, tag));
                    }
                    edges.add(edge("edge-tag-" + note.getNoteId() + "-" + tag, note.getNoteId(), tagId, "tag", 0.6));
                }
            }
            if (note.getRelatedNoteIds() != null) {
                for (String related : note.getRelatedNoteIds()) {
                    edges.add(edge("edge-ref-" + note.getNoteId() + "-" + related, note.getNoteId(), related, "reference", 0.8));
                }
            }
        }
        Map<String, Object> graph = new LinkedHashMap<>();
        graph.put("userId", userId);
        graph.put("nodes", nodes);
        graph.put("edges", edges);
        graph.put("generatedAt", LocalDateTime.now());
        graph.put("filters", filters);
        graph.put("summary", buildSummary(nodes));
        return graph;
    }

    private Map<String, Object> node(KnowledgeNoteDocument note) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", note.getNoteId());
        node.put("type", "note");
        node.put("label", note.getTitle());
        node.put("noteId", note.getNoteId());
        node.put("category", note.getCategory());
        node.put("masteryLevel", note.getMasteryLevel());
        node.put("size", 18);
        node.put("colorGroup", colorGroup(note.getMasteryLevel(), note.getReviewCount()));
        node.put("metadata", Map.of("tags", note.getTags() == null ? List.of() : note.getTags()));
        return node;
    }

    private Map<String, Object> tagNode(String id, String label) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", id);
        node.put("type", "tag");
        node.put("label", label);
        node.put("tag", label);
        node.put("size", 24);
        node.put("colorGroup", "category");
        node.put("metadata", Map.of());
        return node;
    }

    private Map<String, Object> categoryNode(String id, String label) {
        Map<String, Object> node = tagNode(id, label);
        node.put("type", "category");
        node.put("category", label);
        return node;
    }

    private Map<String, Object> edge(String id, String source, String target, String type, double weight) {
        Map<String, Object> edge = new LinkedHashMap<>();
        edge.put("id", id);
        edge.put("source", source);
        edge.put("target", target);
        edge.put("type", type);
        edge.put("weight", weight);
        edge.put("metadata", Map.of());
        return edge;
    }

    private String colorGroup(Integer masteryLevel, Integer reviewCount) {
        int mastery = masteryLevel == null ? 0 : masteryLevel;
        if (reviewCount == null || reviewCount == 0) {
            return "new";
        }
        if (mastery >= 80) {
            return "mastered";
        }
        return mastery < 40 ? "weak" : "learning";
    }

    private Map<String, Object> buildSummary(List<Map<String, Object>> nodes) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("mastered", (int) nodes.stream().filter(n -> "mastered".equals(n.get("colorGroup"))).count());
        summary.put("learning", (int) nodes.stream().filter(n -> "learning".equals(n.get("colorGroup"))).count());
        summary.put("weak", (int) nodes.stream().filter(n -> "weak".equals(n.get("colorGroup"))).count());
        summary.put("new", (int) nodes.stream().filter(n -> "new".equals(n.get("colorGroup"))).count());
        summary.put("total", nodes.size());
        summary.put("averageMastery", nodes.stream()
                .filter(n -> n.get("masteryLevel") instanceof Number)
                .mapToInt(n -> ((Number) n.get("masteryLevel")).intValue())
                .average().orElse(0));
        return summary;
    }

    private boolean shareTag(KnowledgeNoteDocument a, KnowledgeNoteDocument b) {
        if (a.getTags() == null || b.getTags() == null) {
            return false;
        }
        return a.getTags().stream().anyMatch(b.getTags()::contains);
    }
}
