package com.chat.myAgent.common.audit;

import com.chat.myAgent.config.ModelConfig;
import com.chat.myAgent.model.dto.AgentRequest;
import com.chat.myAgent.model.dto.ChatRequest;
import com.chat.myAgent.react.core.ReActRunResult;
import com.chat.myAgent.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;
import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;
    private final ModelConfig modelConfig;

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) {
        long startTime = System.currentTimeMillis();
        String conversationId = extractConversationId(joinPoint);
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = "audit-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }
        String message = extractMessage(joinPoint);
        String username = extractUsername();
        String agentType = auditable.agentType();
        String model = modelConfig.getPrimaryModel();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> returnType = signature.getReturnType();

        try {
            Object result = joinPoint.proceed();

            if (Flux.class.isAssignableFrom(returnType)) {
                return handleFlux((Flux<?>) result, conversationId, username, message, agentType, model, startTime);
            } else {
                return handleSync(result, conversationId, username, message, agentType, model, startTime);
            }
        } catch (Throwable e) {
            long latency = System.currentTimeMillis() - startTime;
            auditService.saveChatHistory(conversationId, username, "user", message, agentType, model, null, null, latency);
            auditService.saveAgentInvocation(conversationId, agentType, model, message, null, null, "FAILED", latency);
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(e);
        }
    }

    private Object handleSync(Object result, String conversationId, String username,
                               String message, String agentType, String model, long startTime) {
        long latency = System.currentTimeMillis() - startTime;
        String reply = extractReply(result);
        String thinking = extractThinking(result);
        String resultModel = extractModel(result);
        if (resultModel != null && !resultModel.isBlank()) {
            model = resultModel;
        }
        auditService.saveChatHistory(conversationId, username, "user", message, agentType, model, null, null, latency);
        auditService.saveChatHistory(conversationId, username, "assistant", reply, agentType, model, null, null, latency);
        auditService.saveAgentInvocation(conversationId, agentType, model, message, reply, thinking, "SUCCESS", latency);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object handleFlux(Flux<?> flux, String conversationId, String username,
                               String message, String agentType, String model, long startTime) {
        StringBuilder fullResponse = new StringBuilder();
        return ((Flux<String>) flux)
                .doOnNext(chunk -> {
                    if (chunk != null) {
                        fullResponse.append(chunk);
                    }
                })
                .doFinally(signalType -> {
                    long latency = System.currentTimeMillis() - startTime;
                    String status = signalType == reactor.core.publisher.SignalType.ON_COMPLETE ? "SUCCESS" : "FAILED";
                    String reply = fullResponse.toString();
                    auditService.saveChatHistory(conversationId, username, "user", message, agentType, model, null, null, latency);
                    auditService.saveChatHistory(conversationId, username, "assistant", reply, agentType, model, null, null, latency);
                    auditService.saveAgentInvocation(conversationId, agentType, model, message, reply, null, status, latency);
                });
    }

    private String extractConversationId(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof AgentRequest r) return r.getConversationId();
            if (arg instanceof ChatRequest r) return r.getConversationId();
        }
        for (Object arg : args) {
            if (arg instanceof String s && (s.startsWith("chat-") || s.startsWith("agent-")
                    || s.startsWith("rag-") || s.startsWith("plan-") || s.startsWith("stream-")
                    || s.startsWith("full-") || s.startsWith("session-"))) {
                return s;
            }
        }
        if (args.length >= 2 && args[1] instanceof String s) return s;
        return null;
    }

    private String extractMessage(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof AgentRequest r) return r.getMessage();
            if (arg instanceof ChatRequest r) return r.getMessage();
        }
        if (args.length >= 1 && args[0] instanceof String s) return s;
        return "";
    }

    private String extractUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())
                ? auth.getName() : "anonymous";
    }

    private String extractReply(Object result) {
        if (result == null) return "";
        try {
            for (Method m : result.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                String name = m.getName();
                if (name.equals("getReply") || name.equals("getAnswer") || name.equals("getFinalAnswer")) {
                    Object v = m.invoke(result);
                    return v == null ? "" : v.toString();
                }
            }
            if (result instanceof ReActRunResult r) {
                return r.getTrace() != null ? r.getTrace().getFinalAnswer() : "";
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private String extractThinking(Object result) {
        if (result == null) return null;
        try {
            Method m = result.getClass().getMethod("getThinking");
            Object v = m.invoke(result);
            return v == null ? null : v.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractModel(Object result) {
        if (result == null) return null;
        try {
            for (Method m : result.getClass().getMethods()) {
                if (m.getParameterCount() != 0 && m.getName().equals("getModel")) {
                    Object v = m.invoke(result);
                    return v == null ? null : v.toString();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
