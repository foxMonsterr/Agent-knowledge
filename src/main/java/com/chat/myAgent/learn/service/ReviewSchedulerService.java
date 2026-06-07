package com.chat.myAgent.learn.service;

import com.chat.myAgent.learn.dto.ReviewQueueItem;
import com.chat.myAgent.learn.model.FlashcardDocument;
import com.chat.myAgent.learn.model.KnowledgeNoteDocument;
import com.chat.myAgent.learn.repository.mongo.FlashcardRepository;
import com.chat.myAgent.learn.repository.mongo.KnowledgeNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewSchedulerService {

    private final FlashcardRepository flashcardRepository;
    private final KnowledgeNoteRepository noteRepository;

    public List<ReviewQueueItem> priorityQueue(String userId) {
        List<FlashcardDocument> cards = flashcardRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        return cards.stream()
                .map(card -> toQueueItem(card, userId))
                .sorted(Comparator.comparingDouble(ReviewQueueItem::getPriority).reversed())
                .toList();
    }

    public Map<String, Object> priorityQueueStats(String userId) {
        List<ReviewQueueItem> queue = priorityQueue(userId);
        long overdueCount = queue.stream().filter(item -> item.getOverdueDays() > 0).count();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalCards", queue.size());
        stats.put("overdueCards", (int) overdueCount);
        stats.put("highestPriority", queue.isEmpty() ? 0 : queue.get(0).getPriority());
        stats.put("queue", queue);
        return stats;
    }

    private ReviewQueueItem toQueueItem(FlashcardDocument card, String userId) {
        int masteryLevel = 0;
        if (card.getNoteId() != null) {
            masteryLevel = noteRepository.findByNoteIdAndUserId(card.getNoteId(), userId)
                    .map(KnowledgeNoteDocument::getMasteryLevel)
                    .orElse(0);
        }
        long overdueDays = 0;
        if (card.getNextReviewAt() != null) {
            overdueDays = Math.max(0, ChronoUnit.DAYS.between(card.getNextReviewAt(), LocalDateTime.now()));
        }
        double priority = calculatePriority(card, masteryLevel, overdueDays);
        return ReviewQueueItem.builder()
                .cardId(card.getCardId())
                .noteId(card.getNoteId())
                .front(card.getFront())
                .back(card.getBack())
                .title(card.getFront())
                .easeFactor(card.getEaseFactor())
                .intervalDays(card.getIntervalDays())
                .reviewCount(card.getReviewCount())
                .masteryLevel(masteryLevel)
                .nextReviewAt(card.getNextReviewAt())
                .overdueDays(overdueDays)
                .priority(priority)
                .build();
    }

    private double calculatePriority(FlashcardDocument card, int masteryLevel, long overdueDays) {
        int intervalDays = card.getIntervalDays() == null || card.getIntervalDays() <= 0 ? 1 : card.getIntervalDays();
        double sm2Score = 1.0 / intervalDays;
        double masteryWeight = masteryLevel < 40 ? 2.0 : masteryLevel < 60 ? 1.5 : 1.0;
        double overdueWeight = 1.0 + overdueDays * 0.1;
        return sm2Score * masteryWeight * overdueWeight;
    }
}
