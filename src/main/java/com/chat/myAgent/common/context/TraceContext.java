package com.chat.myAgent.common.context;

import java.util.UUID;

/**
 * 请求追踪上下文
 *
 * 用于在一次 HTTP 请求链路中生成并保存 traceId，方便日志串联、问题排查和审计。
 */
public final class TraceContext {

    private static final ThreadLocal<String> TRACE_ID_HOLDER = new ThreadLocal<>();

    private TraceContext() {
    }

    public static String getTraceId() {
        String traceId = TRACE_ID_HOLDER.get();
        if (traceId == null || traceId.isBlank()) {
            traceId = generateTraceId();
            TRACE_ID_HOLDER.set(traceId);
        }
        return traceId;
    }

    public static void setTraceId(String traceId) {
        TRACE_ID_HOLDER.set(traceId);
    }

    public static void clear() {
        TRACE_ID_HOLDER.remove();
    }

    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
