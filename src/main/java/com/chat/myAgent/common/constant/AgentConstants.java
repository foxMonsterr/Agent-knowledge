package com.chat.myAgent.common.constant;

/**
 * Agent 全局常量
 */
public class AgentConstants {

    private AgentConstants() {}

    // ==================== 模型相关 ====================
    public static final String DEFAULT_MODEL = "deepseek-chat";
    public static final String QWEN_MODEL = "qwen-plus";

    // ==================== 会话相关 ====================
    public static final int MAX_MEMORY_SIZE = 20;
    public static final int SESSION_TIMEOUT_MINUTES = 30;

    // ==================== Prompt 角色相关 ====================
    public static final String SYSTEM_ROLE_DEFAULT = "你是一个智能助手，名叫 SmartAgent。";

    // ==================== 专家角色预设 ====================
    public static final String ROLE_JAVA = "Java";
    public static final String ROLE_PYTHON = "Python";
    public static final String ROLE_FRONTEND = "前端开发";
    public static final String ROLE_DATABASE = "数据库";
    public static final String ROLE_ARCHITECTURE = "架构设计";

    // ==================== 用户水平 ====================
    public static final String LEVEL_BEGINNER = "beginner";
    public static final String LEVEL_INTERMEDIATE = "intermediate";
    public static final String LEVEL_ADVANCED = "advanced";

    // ==================== 结构化输出类型 ====================
    public static final String OUTPUT_TYPE_BOOK = "book";
    public static final String OUTPUT_TYPE_TASK = "task";
    public static final String OUTPUT_TYPE_SENTIMENT = "sentiment";

    // ==================== 工具名称（阶段3使用） ====================
    public static final String TOOL_DATETIME = "datetime";
    public static final String TOOL_CALCULATOR = "calculator";
    public static final String TOOL_TRANSLATE = "translate";
    public static final String TOOL_DOC_PARSE = "doc_parse";
    public static final String TOOL_DB_QUERY = "db_query";
}
