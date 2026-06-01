package com.chat.myAgent.config;

import com.chat.myAgent.common.context.TraceContextFilter;
import com.chat.myAgent.common.context.TraceContextMdcFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Trace 上下文配置
 *
 * 注册请求级 traceId 过滤器，便于日志、审计和问题排查。
 */
@Configuration
public class TraceConfig {

    @Bean
    @Order(1)
    public OncePerRequestFilter traceContextFilter() {
        return new TraceContextFilter();
    }

    @Bean
    @Order(2)
    public OncePerRequestFilter traceContextMdcFilter() {
        return new TraceContextMdcFilter();
    }
}
