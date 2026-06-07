package com.chat.myAgent.learn.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class FlashcardReviewRequest {
    @Min(0)
    @Max(5)
    private Integer quality;
}
