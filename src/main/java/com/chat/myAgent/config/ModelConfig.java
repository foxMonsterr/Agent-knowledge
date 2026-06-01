package com.chat.myAgent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 多模型配置
 *
 * 说明：
 * - defaultModel：默认主模型
 * - fallbackModel：主模型失败时的兜底模型
 * - available：统一维护模型别名到真实模型名的映射
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "smart-agent.models")
public class ModelConfig {

    /**
     * 默认主模型
     */
    private String defaultModel = "deepseek-chat";

    /**
     * 兜底模型
     */
    private String fallbackModel = "qwen3.6-plus";

    /**
     * 可用模型列表
     * key: 模型别名
     * value: 实际模型名称
     */
    private Map<String, String> available = new LinkedHashMap<>();

    /**
     * 获取实际模型名称
     */
    public String resolveModel(String alias) {
        if (alias == null || alias.isBlank()) {
            return defaultModel;
        }
        return available.getOrDefault(alias, alias);
    }

    /**
     * 获取主模型名称
     */
    public String getPrimaryModel() {
        return resolveModel(defaultModel);
    }

    /**
     * 获取兜底模型名称
     */
    public String getFallbackModelName() {
        return resolveModel(fallbackModel);
    }
}
