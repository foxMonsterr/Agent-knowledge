package com.chat.myAgent.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 演示流程步骤
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemoFlowStepVO {
    private String title;
    private String description;
    private String route;
    private String highlight;
}
