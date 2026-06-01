package com.chat.myAgent.common.result;

import com.chat.myAgent.common.constant.ResultCode;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一响应体
 * 所有接口返回此格式，保证前端/调试工具能统一处理
 */
@Data
@NoArgsConstructor
public class R<T> implements Serializable {

    private int code;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    private R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    // ==================== 成功响应 ====================

    public static <T> R<T> ok(T data) {
        return new R<>(ResultCode.SUCCESS, "success", data);
    }

    public static <T> R<T> ok(String message, T data) {
        return new R<>(ResultCode.SUCCESS, message, data);
    }

    public static R<Void> ok() {
        return new R<>(ResultCode.SUCCESS, "success", null);
    }

    // ==================== 失败响应 ====================

    public static <T> R<T> fail(String message) {
        return new R<>(ResultCode.INTERNAL_ERROR, message, null);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }

    // ==================== 预定义错误码（扩展性预留） ====================

    public static <T> R<T> paramError(String message) {
        return new R<>(ResultCode.BAD_REQUEST, message, null);
    }

    public static <T> R<T> unauthorized(String message) {
        return new R<>(ResultCode.UNAUTHORIZED, message, null);
    }

    public static <T> R<T> forbidden(String message) {
        return new R<>(ResultCode.FORBIDDEN, message, null);
    }

    public static <T> R<T> modelError(String message) {
        return new R<>(ResultCode.THIRD_PARTY_ERROR, "模型服务异常: " + message, null);
    }
}
