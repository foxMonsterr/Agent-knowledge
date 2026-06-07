package com.chat.myAgent.react.core;

import com.chat.myAgent.react.model.ReActTraceDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReActToolResult {
    private String observation;
    private String context;

    @Builder.Default
    private List<ReActTraceDocument.ReActSourceRefDocument> sources = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
