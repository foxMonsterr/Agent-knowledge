package com.chat.myAgent.common.exception;

/**
 * 统一错误码定义
 *
 * 便于前后端约定一致的错误语义。
 */
public enum ErrorCode {
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    RATE_LIMIT(429, "请求过于频繁"),
    AI_SERVICE_ERROR(503, "AI 服务异常"),
    INTERNAL_ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
