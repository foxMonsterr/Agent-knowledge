package com.chat.myAgent.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionVO {
    private String sessionId;
    private String title;
    private String summary;
    private String lastMessageAt;
    private String status;
}
