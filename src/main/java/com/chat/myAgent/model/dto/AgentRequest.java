package com.chat.myAgent.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * Agent 请求体
 */
@Data
public class AgentRequest {

    /**
     * 会话ID（可选，用于带记忆的工具调用）
     */
    private String conversationId;

    /**
     * 用户消息
     */
    @NotBlank(message = "消息内容不能为空")
    private String message;

    /**
     * 指定启用的工具列表（可选）
     * 不传则启用所有工具
     * 可选值：datetime, calculator, translate, doc, db
     */
    private List<String> tools;

    /**
     * 是否开启思考模式
     */
    private Boolean thinkingMode = false;
}
