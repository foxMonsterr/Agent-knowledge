package com.chat.myAgent.learn.service;

import com.chat.myAgent.learn.dto.FlashcardReviewRequest;
import com.chat.myAgent.learn.model.FlashcardDocument;
import com.chat.myAgent.learn.repository.mongo.FlashcardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlashcardServiceTest {

    @Mock
    private FlashcardRepository flashcardRepository;

    @Mock
    private NoteService noteService;

    @Mock
    private StudyService studyService;

    @InjectMocks
    private FlashcardService flashcardService;

    @Test
    void reviewAdvancesSm2ScheduleWhenQualityIsRemembered() {
        FlashcardDocument card = card(6, 2, 0, 2.5);
        when(flashcardRepository.findByCardIdAndUserId("card-1", "user-1")).thenReturn(Optional.of(card));
        when(flashcardRepository.save(any(FlashcardDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FlashcardReviewRequest request = new FlashcardReviewRequest();
        request.setQuality(5);

        Map<String, Object> response = flashcardService.review("user-1", "card-1", request);

        assertThat(response)
                .containsEntry("cardId", "card-1")
                .containsEntry("quality", 5)
                .containsEntry("remembered", true)
                .containsEntry("previousIntervalDays", 6)
                .containsEntry("nextIntervalDays", 15)
                .containsEntry("masteryDelta", 5);

        ArgumentCaptor<FlashcardDocument> savedCaptor = ArgumentCaptor.forClass(FlashcardDocument.class);
        verify(flashcardRepository).save(savedCaptor.capture());
        FlashcardDocument saved = savedCaptor.getValue();
        assertThat(saved.getReviewCount()).isEqualTo(3);
        assertThat(saved.getLapseCount()).isZero();
        assertThat(saved.getIntervalDays()).isEqualTo(15);
        assertThat(saved.getEaseFactor()).isEqualTo(2.6);
        assertThat(saved.getLastReviewAt()).isNotNull();
        assertThat(saved.getNextReviewAt()).isAfter(LocalDateTime.now());

        verify(studyService).applyMasteryDelta("user-1", "note-1", 5, "闪卡复习评分 5");
        verify(studyService).record(eq("user-1"), eq("flashcard_review"), eq("front"),
                eq(List.of("tag")), eq("category"), isNull(), isNull(), eq("note-1"),
                isNull(), eq("card-1"), eq(0), eq(100), eq(5), anyMap());
    }

    @Test
    void reviewResetsScheduleWhenQualityIsForgotten() {
        FlashcardDocument card = card(10, 4, 1, 2.0);
        when(flashcardRepository.findByCardIdAndUserId("card-1", "user-1")).thenReturn(Optional.of(card));
        when(flashcardRepository.save(any(FlashcardDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FlashcardReviewRequest request = new FlashcardReviewRequest();
        request.setQuality(2);

        Map<String, Object> response = flashcardService.review("user-1", "card-1", request);

        assertThat(response)
                .containsEntry("remembered", false)
                .containsEntry("previousIntervalDays", 10)
                .containsEntry("nextIntervalDays", 1)
                .containsEntry("masteryDelta", -2);

        ArgumentCaptor<FlashcardDocument> savedCaptor = ArgumentCaptor.forClass(FlashcardDocument.class);
        verify(flashcardRepository).save(savedCaptor.capture());
        FlashcardDocument saved = savedCaptor.getValue();
        assertThat(saved.getReviewCount()).isZero();
        assertThat(saved.getLapseCount()).isEqualTo(2);
        assertThat(saved.getIntervalDays()).isEqualTo(1);
        assertThat(saved.getEaseFactor()).isEqualTo(1.8);

        verify(studyService).applyMasteryDelta("user-1", "note-1", -2, "闪卡复习评分 2");
    }

    private FlashcardDocument card(int intervalDays, int reviewCount, int lapseCount, double easeFactor) {
        return FlashcardDocument.builder()
                .cardId("card-1")
                .userId("user-1")
                .noteId("note-1")
                .front("front")
                .back("back")
                .tags(List.of("tag"))
                .category("category")
                .intervalDays(intervalDays)
                .reviewCount(reviewCount)
                .lapseCount(lapseCount)
                .easeFactor(easeFactor)
                .nextReviewAt(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
    }
}

