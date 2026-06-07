package com.chat.myAgent.conversation.stream;

import com.chat.myAgent.conversation.core.ConversationContext;
import com.chat.myAgent.conversation.dto.ConversationSseEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class ConversationStreamService {

    private final ObjectMapper objectMapper;

    public String start(ConversationContext context) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", context.getMessage());
        data.put("memoryEnabled", context.isMemoryEnabled());
        data.put("thinkingMode", context.isThinkingMode());
        return frame(event(context, "start", data, context.getTraceId()));
    }

    public String delta(ConversationContext context, String content) {
        return frame(event(context, "delta", Map.of("content", content == null ? "" : content), context.getTraceId()));
    }

    public String message(ConversationContext context, String content) {
        return frame(event(context, "message", Map.of("content", content == null ? "" : content), context.getTraceId()));
    }

    public String error(ConversationContext context, String code, String message, boolean recoverable) {
        return frame(event(context, "error", Map.of(
                "code", code,
                "message", message == null ? "流式请求失败" : message,
                "recoverable", recoverable
        ), context.getTraceId()));
    }

    public String done(ConversationContext context, String status, String finalContent, long totalDurationMs) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", status == null ? "success" : status);
        data.put("finalContent", finalContent == null ? "" : finalContent);
        data.put("totalDurationMs", totalDurationMs);
        data.put("memoryEnabled", context.isMemoryEnabled());
        return frame(event(context, "done", data, context.getTraceId()));
    }

    public Flux<String> wrapRawEvents(Flux<String> rawEvents, ConversationContext context) {
        long startedAt = System.currentTimeMillis();
        StringBuilder finalContent = new StringBuilder();
        AtomicBoolean seenDone = new AtomicBoolean(false);
        AtomicBoolean seenError = new AtomicBoolean(false);
        return rawEvents
                .map(raw -> normalizeRawEvent(raw, context, finalContent, seenDone, seenError))
                .concatWith(Flux.defer(() -> seenDone.get()
                        ? Flux.empty()
                        : Flux.just(done(context, seenError.get() ? "failed" : "success",
                        finalContent.toString(), System.currentTimeMillis() - startedAt))))
                .onErrorResume(ex -> Flux.just(
                        error(context, "STREAM_FAILED", safeMessage(ex), false),
                        done(context, "failed", finalContent.toString(), System.currentTimeMillis() - startedAt)
                ));
    }

    public String normalizeRawEvent(String raw,
                                    ConversationContext context,
                                    StringBuilder finalContent,
                                    AtomicBoolean seenDone,
                                    AtomicBoolean seenError) {
        Map<String, Object> parsed = parseRaw(raw);
        String type = stringValue(parsed.getOrDefault("type", "delta"));
        Object dataValue = parsed.get("data");
        Map<String, Object> data = new LinkedHashMap<>();
        if (dataValue instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                data.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }

        if (data.isEmpty()) {
            Object content = parsed.get("content");
            if (content == null) {
                content = parsed.get("message");
            }
            if (content == null && !"start".equals(type) && !"done".equals(type)) {
                content = raw;
            }
            if (content != null) {
                data.put("content", content);
            }
        }
        if (parsed.get("stepNumber") != null && data.get("stepNumber") == null) {
            data.put("stepNumber", parsed.get("stepNumber"));
        }

        if ("delta".equals(type)) {
            Object content = data.get("content");
            if (content != null) {
                finalContent.append(content);
            }
        } else if ("final_answer".equals(type)) {
            Object answer = data.get("answer");
            if (answer != null) {
                finalContent.setLength(0);
                finalContent.append(answer);
            }
        }
        if ("done".equals(type)) {
            seenDone.set(true);
        }
        if ("error".equals(type)) {
            seenError.set(true);
        }

        String traceId = stringValue(parsed.get("traceId"));
        if (traceId != null && !traceId.isBlank()) {
            context.setTraceId(traceId);
        }
        if (data.get("status") == null && "done".equals(type)) {
            data.put("status", "success");
        }
        if (data.get("finalContent") == null && "done".equals(type)) {
            data.put("finalContent", finalContent.toString());
        }

        return frame(event(context, type, data, traceId));
    }

    public ConversationSseEvent<Object> event(ConversationContext context, String type, Object data, String traceId) {
        return ConversationSseEvent.builder()
                .eventId("evt-" + UUID.randomUUID().toString().replace("-", ""))
                .type(type)
                .conversationId(resolveConversationId(context))
                .traceId(traceId == null || traceId.isBlank() ? context.getTraceId() : traceId)
                .agentType(context.agentTypeValue())
                .mode(context.modeValue())
                .timestamp(LocalDateTime.now().toString())
                .data(data)
                .build();
    }

    public String frame(ConversationSseEvent<?> event) {
        try {
            // Spring serializes Flux<String> as SSE data frames for text/event-stream.
            // Returning raw JSON avoids accidental "data: data: {...}" double framing.
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            return "{\"type\":\"error\",\"data\":{\"message\":\"SSE事件序列化失败\"}}";
        }
    }

    private Map<String, Object> parseRaw(String raw) {
        if (raw == null) {
            return Map.of("type", "delta", "content", "");
        }
        String text = raw.trim();
        if (text.startsWith("data:")) {
            StringBuilder payload = new StringBuilder();
            for (String line : text.split("\\r?\\n")) {
                if (line.startsWith("data:")) {
                    payload.append(line.replaceFirst("^data:\\s?", ""));
                }
            }
            text = payload.toString().trim();
        }
        if (text.isBlank()) {
            return Map.of("type", "message", "content", "");
        }
        try {
            return objectMapper.readValue(text, new TypeReference<>() {
            });
        } catch (Exception e) {
            return Map.of("type", "delta", "content", text);
        }
    }

    private String resolveConversationId(ConversationContext context) {
        return context.getConversationId() == null ? "" : context.getConversationId();
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String safeMessage(Throwable throwable) {
        return throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }
}
