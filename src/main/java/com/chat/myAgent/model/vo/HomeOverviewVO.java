package com.chat.myAgent.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 首页概览数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeOverviewVO {
    private String title;
    private String subtitle;
    private List<OpsMetricVO> metrics;
    private List<String> highlights;
}
