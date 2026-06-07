package com.chat.myAgent.conversation.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ConversationSummaryVO {
    private String conversationId;
    private String title;
    private String summary;
    private String status;
    private List<String> agentTypes;
    private String lastMessageAt;
    private String createdAt;
    private String updatedAt;
}
