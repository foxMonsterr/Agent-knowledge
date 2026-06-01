package com.chat.myAgent.config;

import com.chat.myAgent.common.context.TraceContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 统一响应头补充 traceId
 */
@ControllerAdvice
public class ResponseHeaderAdvice {

    @ModelAttribute
    public void addCommonHeaders(HttpServletResponse response) {
        response.setHeader("X-Trace-Id", TraceContext.getTraceId());
    }
}
