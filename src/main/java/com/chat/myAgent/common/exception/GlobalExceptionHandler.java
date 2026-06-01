package com.chat.myAgent.common.exception;

import com.chat.myAgent.common.constant.ResultCode;
import com.chat.myAgent.common.result.R;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;

/**
 * 全局异常处理
 * 捕获所有异常，返回统一格式，避免接口返回原始报错堆栈
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private Environment env;

    /**
     * 处理自定义业务异常
     */
    @ExceptionHandler(BizException.class)
    public R<Void> handleBizException(BizException e) {
        log.warn("业务异常: {}", e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理 Spring AI 相关异常（模型调用失败、API Key 无效等）
     */
    @ExceptionHandler({org.springframework.ai.retry.NonTransientAiException.class})
    public R<Void> handleAiException(Exception e) {
        log.error("AI 模型调用异常: {}", e.getMessage(), e);
        return R.modelError(e.getMessage());
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("请求参数不合法");
        log.warn("参数校验失败: {}", message);
        return R.paramError(message);
    }

    /**
     * 处理约束校验异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("约束校验失败: {}", e.getMessage());
        return R.paramError(e.getMessage());
    }

    /**
     * 处理权限不足
     */
    @ExceptionHandler(AccessDeniedException.class)
    public R<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return R.forbidden("无权限访问该资源");
    }

    /**
     * 兜底：处理所有未捕获异常
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("未知异常: {}", e.getMessage(), e);
        boolean dev = env != null && Arrays.asList(env.getActiveProfiles()).contains("dev");
        String message = dev ? e.getMessage() : "服务器内部错误，请稍后重试";
        return R.fail(ResultCode.INTERNAL_ERROR, message);
    }
}
