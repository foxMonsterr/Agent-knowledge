package com.chat.myAgent.learn.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateNoteRequest {
    private String title;
    private String content;
    private List<String> tags;
    private String category;
    private String summary;
    private Integer masteryLevel;
    private Boolean archived;
    private Boolean vectorReindex = false;
}
