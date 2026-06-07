package com.chat.myAgent.learn.service.websearch;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WebHit {
    private String title;
    private String url;
    private String snippet;
    private Double score;
}
