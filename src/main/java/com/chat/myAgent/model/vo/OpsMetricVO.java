package com.chat.myAgent.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 运营指标视图
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpsMetricVO {
    private String title;
    private String value;
    private String subTitle;
    private String trend;
    private String status;
}
