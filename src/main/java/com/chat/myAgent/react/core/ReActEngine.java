package com.chat.myAgent.react.core;

import com.chat.myAgent.config.ModelConfig;
import com.chat.myAgent.react.model.ReActTraceDocument;
import com.chat.myAgent.react.repository.ReActTraceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Service
public class ReActEngine {

    // ========== 依赖注入 ==========
    // 三个 ChatClient,职责完全隔离,降级时互不污染:
    // - baseChatClient:    主模型,无会话记忆(适合单次问答、批处理)
    // - memoryChatClient:  主模型,带 Redis 记忆(适合多轮对话)
    // - fallbackChatClient:备用模型,主备不在同一连接池,主挂掉时快速切换
    private final ChatClient baseChatClient;
    private final ChatClient memoryChatClient;
    private final ChatClient fallbackChatClient;
    private final ModelConfig modelConfig;
    // 推理链路全量溯源:每一步 Thought/Action/Observation 持久化到 MongoDB
    private final ReActTraceRepository traceRepository;
    // 用于 SSE 事件 JSON 序列化(流式输出)
    private final ObjectMapper objectMapper;

    public ReActEngine(@Qualifier("baseChatClient") ChatClient baseChatClient,
                       @Qualifier("memoryChatClient") ChatClient memoryChatClient,
                       @Qualifier("fallbackChatClient") ChatClient fallbackChatClient,
                       ModelConfig modelConfig,
                       ReActTraceRepository traceRepository,
                       ObjectMapper objectMapper) {
        this.baseChatClient = baseChatClient;
        this.memoryChatClient = memoryChatClient;
        this.fallbackChatClient = fallbackChatClient;
        this.modelConfig = modelConfig;
        this.traceRepository = traceRepository;
        this.objectMapper = objectMapper;
    }

    public ReActRunResult chat(ReActProfile profile, ReActRunRequest request) {
        return ReActRunResult.builder().trace(execute(profile, request, event -> { })).build();
    }

    /**
     * SSE 流式输出:把推理过程中的每一步事件(thought/action/observation/final_answer)
     * 实时推给前端,延迟 60ms(避免前端渲染压力)
     */
    public Flux<String> stream(ReActProfile profile, ReActRunRequest request) {
        return Flux.<String>create(sink -> {
            try {
                execute(profile, request, event -> sink.next(toJson(event)));
                sink.complete();
            } catch (Exception e) {
                sink.next(toJson(event("error", null, null, null,
                        Map.of("code", "REACT_ERROR", "message", safeMessage(e), "recoverable", false))));
                sink.complete();
            }
        }).delayElements(Duration.ofMillis(60));
    }

    /**
     * 获取推理链路详情,带域权限校验(LearnAgent 看不到 ChatAgent 的 trace)
     */
    public ReActTraceDocument getTrace(String userId, String traceId, String domain) {
        return traceRepository.findByTraceIdAndUserId(traceId, userId)
                .filter(trace -> domainMatches(trace, domain))
                .orElseThrow(() -> new IllegalArgumentException("推理链路不存在或无权访问"));
    }

    /**
     * 列出指定会话的推理链路,按时间倒序
     */
    public List<ReActTraceDocument> listTraces(String userId, String domain, String sessionId) {
        List<ReActTraceDocument> traces = sessionId != null && !sessionId.isBlank()
                ? traceRepository.findByUserIdAndSessionIdOrderByCreatedAtDesc(userId, sessionId)
                : traceRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return traces.stream().filter(trace -> domainMatches(trace, domain)).toList();
    }

    /**
     * 运行时人工干预:用户在 ReAct 循环中追加补充信息(类似 ChatGPT 的"补充上下文")
     * 实现方式:往 trace 的 steps 列表追加一条 observation 类型的步骤
     */
    public Map<String, Object> intervene(String userId, String traceId, String domain, Integer stepNumber, String message) {
        ReActTraceDocument trace = getTrace(userId, traceId, domain);
        ReActTraceDocument.ReActStepDocument step = ReActTraceDocument.ReActStepDocument.builder()
                .stepId("step-" + id())
                .stepNumber(stepNumber == null ? trace.getSteps().size() + 1 : stepNumber)
                .type("observation")
                .state("observing")
                .observation("用户补充信息：" + message)
                .sources(List.of())
                .status("success")
                .startedAt(LocalDateTime.now())
                .endedAt(LocalDateTime.now())
                .durationMs(0L)
                .build();
        trace.getSteps().add(step);
        trace.setUpdatedAt(LocalDateTime.now());
        traceRepository.save(trace);
        return Map.of("traceId", traceId, "stepNumber", step.getStepNumber(), "status", "accepted");
    }

    /**
     * 用户主动停止 ReAct 循环
     * 实现:把 trace 状态置为 "stopped",下一次 isStopped() 检查时返回 true,循环终止
     */
    public Map<String, Object> stop(String userId, String traceId, String domain) {
        ReActTraceDocument trace = getTrace(userId, traceId, domain);
        if ("completed".equals(trace.getStatus()) || "failed".equals(trace.getStatus())) {
            return Map.of("traceId", traceId, "status", trace.getStatus());
        }
        trace.setRuntimeState("stopped");
        trace.setStatus("stopped");
        trace.setUpdatedAt(LocalDateTime.now());
        traceRepository.save(trace);
        return Map.of("traceId", traceId, "status", "stopped");
    }

    private ReActTraceDocument execute(ReActProfile profile,
                                       ReActRunRequest request,
                                       Consumer<Map<String, Object>> eventConsumer) {
        // ========== 阶段 1:初始化推理上下文 ==========
        LocalDateTime startedAt = LocalDateTime.now();
        long startNanos = System.nanoTime();
        String traceId = "trace-" + id();
        // 若用户没传 sessionId,自动生成一个,保证多轮对话上下文能串起来
        String sessionId = resolveSessionId(request.getSessionId());
        // 由 Profile 决定本次推理用哪个策略(auto/retrieve_first/explain_first/...)
        String strategy = profile.resolveStrategy(request);
        // 把决策结果回填到 request,供下游工具/提示词使用
        request.setSessionId(sessionId);
        request.setDomain(profile.domain());
        request.setAgentType(profile.agentType());
        request.setStrategy(strategy);

        // ========== 阶段 2:构建 trace 文档 ==========
        // trace 是"一次推理会话"的完整记录,所有步骤/来源/答案都挂在这上面
        // 立即持久化,保证即使用户中途关掉浏览器也能看到 trace
        ReActTraceDocument trace = ReActTraceDocument.builder()
                .traceId(traceId)
                .userId(request.getUserId())
                .sessionId(sessionId)
                .domain(profile.domain())
                .agentType(profile.agentType())
                .question(request.getMessage())
                .strategy(strategy)
                .runtimeState("thinking")
                .steps(new ArrayList<>())
                .finalSources(new ArrayList<>())
                .metadata(request.getMetadata() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(request.getMetadata()))
                .model(modelConfig.getPrimaryModel())
                .fallbackUsed(false)
                .status("running")
                .createdAt(startedAt)
                .updatedAt(startedAt)
                .build();
        traceRepository.save(trace);

        // ========== 阶段 3:发送 start 事件(SSE) ==========
        Map<String, Object> startData = new LinkedHashMap<>();
        startData.put("question", request.getMessage());
        startData.put("strategy", strategy);
        startData.put("model", modelConfig.getPrimaryModel());
        startData.put("maxIterations", maxIterations(request));
        startData.put("domain", profile.domain());
        startData.put("agentType", profile.agentType());
        eventConsumer.accept(event("start", traceId, sessionId, null, startData));

        // ========== 阶段 4:记录初始思考(Thought) ==========
        // Profile 决定初始思考文案,比如 LearnAgent 的"我会先检索你的知识库..."
        addStep(trace, eventConsumer, "thought", "thinking", "success",
                profile.initialThought(request), null, null, List.of(), null, null, 0L);

        // ========== 阶段 5:准备工具执行收集器 ==========
        // observations: 每个工具返回的"观察结果",最终喂给 LLM 生成答案
        // contextParts:  每个工具返回的"上下文文本",用于拼接最终 prompt
        // sources:       所有工具返回的"来源引用",用于溯源展示
        List<String> observations = new ArrayList<>();
        List<String> contextParts = new ArrayList<>();
        List<ReActTraceDocument.ReActSourceRefDocument> sources = new ArrayList<>();
        // 工具集由 Profile.selectTools() 决定,最大迭代次数限制 1-10
        List<ReActTool> tools = profile.selectTools(request).stream().limit(maxIterations(request)).toList();

        // ========== 阶段 6:ReAct 核心循环(Action → Observation) ==========
        for (ReActTool tool : tools) {
            // 6.1 检查用户是否中途停止(每次循环开始前都查一次)
            if (isStopped(trace)) {
                return finalizeStopped(profile, request, trace, eventConsumer, sources, contextParts, startNanos);
            }
            // 6.2 构造工具输入参数(让工具自己决定怎么从 request 提取参数)
            Map<String, Object> input = tool.input(request);
            // 6.3 记录 action 步骤(工具名 + 输入)
            addStep(trace, eventConsumer, "action", "acting", "running",
                    null, tool.name(), input, List.of(), null, null, 0L);

            // 6.4 执行工具(异常捕获:工具失败不中断整体流程)
            long toolStart = System.nanoTime();
            try {
                ReActToolResult result = tool.execute(request, input);
                long durationMs = (System.nanoTime() - toolStart) / 1_000_000;
                // observation 是工具的"简短描述",context 是"详细文本",sources 是"来源引用"
                String observation = blankToDefault(result.getObservation(), tool.displayName() + " 已完成。");
                observations.add(observation);
                if (result.getContext() != null && !result.getContext().isBlank()) {
                    contextParts.add(result.getContext());
                }
                if (result.getSources() != null) {
                    sources.addAll(result.getSources());
                }
                // 6.5 记录 observation 步骤(工具返回内容 + 耗时)
                addStep(trace, eventConsumer, "observation", "observing", "success",
                        null, null, null, result.getSources(), observation, null, durationMs);
            } catch (Exception e) {
                // 工具执行失败:记录错误,继续下一个工具,不中断整个 ReAct
                long durationMs = (System.nanoTime() - toolStart) / 1_000_000;
                String message = tool.displayName() + " 执行失败: " + safeMessage(e);
                observations.add(message);
                addStep(trace, eventConsumer, "observation", "observing", "failed",
                        null, null, null, List.of(), message, safeMessage(e), durationMs);
            }
        }

        // 6.6 发送 source 事件(告诉前端所有已收集到的来源)
        eventConsumer.accept(event("source", trace.getTraceId(), trace.getSessionId(), trace.getSteps().size(),
                Map.of("sources", sources)));

        // 6.7 再次检查停止信号(工具执行完到生成答案之间的间隙)
        if (isStopped(trace)) {
            return finalizeStopped(profile, request, trace, eventConsumer, sources, contextParts, startNanos);
        }

        // ========== 阶段 7:生成最终答案 ==========
        // 把所有工具的 context 拼成大文本,交给 LLM 综合回答
        String context = String.join("\n\n", contextParts);
        ModelAnswer answer = generateAnswer(profile, request, context, observations, sources);

        // ========== 阶段 8:更新 trace 并发送最终事件 ==========
        trace.setModel(answer.model());
        trace.setFallbackUsed(answer.fallbackUsed());
        trace.setFinalAnswer(answer.answer());
        trace.setFinalSources(sources);
        trace.setRuntimeState("finalizing");
        trace.setStatus("completed");
        trace.setTotalIterations(trace.getSteps().size());
        trace.setTotalDurationMs((System.nanoTime() - startNanos) / 1_000_000);
        trace.setUpdatedAt(LocalDateTime.now());
        traceRepository.save(trace);

        // 发送 final_answer 事件(包含答案 + 来源 + 建议下一步动作)
        eventConsumer.accept(event("final_answer", traceId, sessionId, null,
                Map.of("answer", trace.getFinalAnswer(), "sources", sources,
                        "suggestedActions", profile.suggestedActions(request, trace, sources))));
        // 发送 done 事件(包含总耗时、是否降级)
        eventConsumer.accept(event("done", traceId, sessionId, null,
                Map.of("status", trace.getStatus(), "totalIterations", trace.getTotalIterations(),
                        "totalDurationMs", trace.getTotalDurationMs(), "fallbackUsed", trace.getFallbackUsed())));

        // ========== 阶段 9:执行 Profile 的后置钩子(学习数据记录) ==========
        // 比如 LearnAgent 会在此处调用 studyService.record() 记录本次对话
        try {
            profile.afterCompleted(request, trace);
        } catch (Exception e) {
            log.warn("ReAct afterCompleted hook failed, traceId={}: {}", trace.getTraceId(), safeMessage(e));
        }
        return trace;
    }

    /**
     * 三级降级生成答案:
     * 第一级 主模型(带/不带记忆)
     * 第二级 备用模型(独立连接池,不与主模型共享)
     * 第三级 本地兜底(纯 Java 拼接,完全不调 LLM)
     *
     * 设计原则:快速失败而非重试。
     * LLM API 故障通常持续较久(限流/宕机),重试只会增加用户等待时间。
     */
    private ModelAnswer generateAnswer(ReActProfile profile,
                                       ReActRunRequest request,
                                       String context,
                                       List<String> observations,
                                       List<ReActTraceDocument.ReActSourceRefDocument> sources) {
        // 让 Profile 拼最终答案的 prompt(注入工具观察 + 来源 + 用户问题)
        String prompt = profile.buildAnswerPrompt(request, context, observations, sources);
        try {
            // 第一级:主模型(默认使用,带 sessionId 维持多轮上下文)
            String answer = memoryEnabled(request)
                    ? memoryChatClient.prompt()
                    .user(prompt)
                    .advisors(advisor -> advisor.param(org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID, request.getSessionId()))
                    .call()
                    .content()
                    : baseChatClient.prompt().user(prompt).call().content();
            // 成功时 fallbackUsed=false,模型名记为主模型
            return new ModelAnswer(answer, modelConfig.getPrimaryModel(), false);
        } catch (Exception primaryEx) {
            // 第二级:备用模型(连接池完全独立,主挂掉时不会污染)
            try {
                String answer = fallbackChatClient.prompt().user(prompt).call().content();
                // fallbackUsed=true,后续可聚合统计降级率
                return new ModelAnswer(answer, modelConfig.getFallbackModelName(), true);
            } catch (Exception fallbackEx) {
                // 第三级:本地兜底(完全不调 LLM,把工具观察拼成结构化摘要)
                return new ModelAnswer(profile.localAnswer(request, context, observations), modelConfig.getFallbackModelName(), true);
            }
        }
    }

    /**
     * 记录一步推理,并把事件推给 SSE
     * 每次调用都会:
     * 1. 构造 ReActStepDocument,追加到 trace.steps
     * 2. 更新 trace.runtimeState(thinking/acting/observing)
     * 3. 立即持久化(让前端刷新时能看到最新状态)
     * 4. 推一个 SSE 事件给前端
     */
    private void addStep(ReActTraceDocument trace,
                         Consumer<Map<String, Object>> eventConsumer,
                         String type,
                         String state,
                         String status,
                         String thought,
                         String actionName,
                         Map<String, Object> actionInput,
                         List<ReActTraceDocument.ReActSourceRefDocument> sources,
                         String observation,
                         String errorMessage,
                         Long durationMs) {
        LocalDateTime now = LocalDateTime.now();
        ReActTraceDocument.ReActStepDocument step = ReActTraceDocument.ReActStepDocument.builder()
                .stepId("step-" + id())
                .stepNumber(trace.getSteps().size() + 1)
                .type(type)
                .state(state)
                .thought(thought)
                .actionName(actionName)
                .actionInput(actionInput)
                .observation(observation)
                .sources(sources == null ? List.of() : sources)
                .status(status)
                .startedAt(now)
                .endedAt(LocalDateTime.now())
                .durationMs(durationMs == null ? 0L : durationMs)
                .errorMessage(errorMessage)
                .build();
        trace.getSteps().add(step);
        trace.setRuntimeState(state);
        trace.setUpdatedAt(LocalDateTime.now());
        traceRepository.save(trace);

        // 构造 SSE 事件 data(只放非空字段,减少前端解析负担)
        Map<String, Object> data = new LinkedHashMap<>();
        if (thought != null) data.put("content", thought);
        if (actionName != null) data.put("toolName", actionName);
        if (actionName != null) data.put("displayName", actionName);
        if (actionInput != null) data.put("input", actionInput);
        if (observation != null) data.put("content", observation);
        if (durationMs != null) data.put("durationMs", durationMs);
        if (errorMessage != null) data.put("errorMessage", errorMessage);
        data.put("state", state);
        data.put("status", status);
        eventConsumer.accept(event(type, trace.getTraceId(), trace.getSessionId(), step.getStepNumber(), data));
    }

    /**
     * 用户主动停止时的收尾:不调 LLM,基于已有工具观察拼一个简短总结
     */
    private ReActTraceDocument finalizeStopped(ReActProfile profile,
                                               ReActRunRequest request,
                                               ReActTraceDocument trace,
                                               Consumer<Map<String, Object>> eventConsumer,
                                               List<ReActTraceDocument.ReActSourceRefDocument> sources,
                                               List<String> contextParts,
                                               long startNanos) {
        String context = String.join("\n\n", contextParts);
        trace.setRuntimeState("stopped");
        trace.setStatus("stopped");
        trace.setFinalSources(sources);
        trace.setFinalAnswer(stoppedAnswer(profile, request, context));
        trace.setTotalIterations(trace.getSteps().size());
        trace.setTotalDurationMs((System.nanoTime() - startNanos) / 1_000_000);
        trace.setUpdatedAt(LocalDateTime.now());
        traceRepository.save(trace);
        eventConsumer.accept(event("final_answer", trace.getTraceId(), trace.getSessionId(), null,
                Map.of("answer", trace.getFinalAnswer(), "sources", sources, "suggestedActions", List.of())));
        eventConsumer.accept(event("done", trace.getTraceId(), trace.getSessionId(), null,
                Map.of("status", "stopped", "totalIterations", trace.getTotalIterations(),
                        "totalDurationMs", trace.getTotalDurationMs(), "fallbackUsed", trace.getFallbackUsed())));
        return trace;
    }

    /**
     * 拼一个"已停止"的兜底答案
     * - 没有任何工具观察:直接告诉用户"还没有可用结果"
     * - 有工具观察:复用 Profile.localAnswer() 拼个简短总结
     */
    private String stoppedAnswer(ReActProfile profile, ReActRunRequest request, String context) {
        if (context == null || context.isBlank()) {
            return "本次 ReAct 对话已停止，当前还没有可用于总结的工具观察结果。";
        }
        return "本次 ReAct 对话已停止。基于已有观察结果，先保留一个简短总结：\n\n"
                + profile.localAnswer(request, context, List.of());
    }

    /**
     * 每次循环都查一次数据库,看用户是否把 trace 状态改成 stopped
     * 注意:不用内存变量,因为 stop() 在另一个 HTTP 请求里执行,内存不共享
     */
    private boolean isStopped(ReActTraceDocument trace) {
        return traceRepository.findByTraceIdAndUserId(trace.getTraceId(), trace.getUserId())
                .map(saved -> "stopped".equals(saved.getStatus()))
                .orElse(false);
    }

    /**
     * 域权限校验:LearnAgent 看不到 ChatAgent 的 trace(避免跨域泄露)
     * learn 域允许查 domain=null 的旧数据(兼容升级前的 trace)
     */
    private boolean domainMatches(ReActTraceDocument trace, String domain) {
        if (domain == null || domain.isBlank()) {
            return true;
        }
        if ("learn".equals(domain)) {
            return trace.getDomain() == null || "learn".equals(trace.getDomain());
        }
        return domain.equals(trace.getDomain());
    }

    /**
     * 构造统一格式的 SSE 事件
     * 字段顺序固定,前端按字段名解析
     */
    private Map<String, Object> event(String type, String traceId, String sessionId, Integer stepNumber, Map<String, Object> data) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventId", "evt-" + id());
        event.put("type", type);
        event.put("traceId", traceId);
        event.put("sessionId", sessionId);
        if (stepNumber != null) {
            event.put("stepNumber", stepNumber);
        }
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("data", data);
        return event;
    }

    /**
     * Map → JSON(用于 SSE 推字符串)
     * 序列化失败时返回一个 error 事件,而不是抛出异常
     */
    private String toJson(Map<String, Object> event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            return "{\"type\":\"error\",\"data\":{\"message\":\"SSE事件序列化失败\"}}";
        }
    }

    /**
     * 解析 sessionId:为 null/空时自动生成一个"session-uuid"
     * 保证多轮对话能基于同一个 sessionId 拉历史
     */
    private String resolveSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return "session-" + id();
        }
        return sessionId;
    }

    /**
     * 限制最大工具迭代次数(1-10),防止 LLM 失控死循环
     */
    private int maxIterations(ReActRunRequest request) {
        if (request.getMaxIterations() == null || request.getMaxIterations() <= 0) {
            return 6;
        }
        return Math.min(request.getMaxIterations(), 10);
    }

    /**
     * 是否启用会话记忆(默认 true;设为 false 可用于批处理、测试)
     */
    private boolean memoryEnabled(ReActRunRequest request) {
        return request.getMemoryEnabled() == null || request.getMemoryEnabled();
    }

    /**
     * 提取异常的简短消息(避免 NPE)
     */
    private String safeMessage(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    /**
     * null/空时返回兜底值
     */
    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * 生成 32 位 UUID(去掉短横线),URL/文件名安全
     */
    private String id() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 模型降级结果三元组:答案内容 + 实际使用的模型名 + 是否降级
     */
    private record ModelAnswer(String answer, String model, boolean fallbackUsed) {
    }
}
