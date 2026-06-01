package com.chat.myAgent.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 结构化输出请求体
 */
@Data
public class StructuredRequest {

    /**
     * 用户输入内容
     */
    @NotBlank(message = "输入内容不能为空")
    private String input;

    /**
     * 输出类型（可选，扩展预留）
     * book / task / sentiment
     */
    private String outputType;
}
