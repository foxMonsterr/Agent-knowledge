package com.chat.myAgent.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 结构化输出响应体
 */
@Data
@Builder
public class StructuredResponse<T> {

    /**
     * 结构化结果对象
     */
    private T result;

    /**
     * 输出类型标识
     */
    private String outputType;

    /**
     * 原始输入
     */
    private String originalInput;

    /**
     * 本次响应关联的 traceId
     */
    private String traceId;
}
