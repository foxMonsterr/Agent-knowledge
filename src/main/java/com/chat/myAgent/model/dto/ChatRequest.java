package com.chat.myAgent.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 聊天请求体
 *
 * 说明：
 * - thinkingMode：是否开启思考模式
 * - 该字段用于企业级控制台前后端联动
 */
@Data
public class ChatRequest {

    /**
     * 会话ID（用于多轮对话隔离）
     * 不传则自动生成新会话
     */
    private String conversationId;

    /**
     * 用户消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    private String message;

    /**
     * 指定模型（可选，扩展预留）
     */
    private String model;

    /**
     * 专家角色（可选，用于 expertChat 接口）
     * 如：Java、Python、前端、数据库、架构设计
     */
    private String role;

    /**
     * 用户水平（可选，配合 role 使用）
     * 如：beginner(初学者)、intermediate(中级)、advanced(高级)
     */
    private String level;

    /**
     * 是否开启思考模式
     */
    private Boolean thinkingMode = false;
}
