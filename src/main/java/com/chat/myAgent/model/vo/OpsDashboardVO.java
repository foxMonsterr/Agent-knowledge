package com.chat.myAgent.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 运营看板聚合数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpsDashboardVO {
    private List<OpsMetricVO> metrics;
    private List<TrendPointVO> dailyChats;
    private List<TrendPointVO> dailyRuns;
    private List<TrendPointVO> dailyTokens;
    private List<AuditLogVO> recentAudits;
}
