package com.chat.myAgent.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 监控概览视图
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitorOverviewVO {
    private long totalUsers;
    private long totalChatRecords;
    private long totalAgentRuns;
    private long totalTokens;
    private long promptTokens;
    private long completionTokens;
}
