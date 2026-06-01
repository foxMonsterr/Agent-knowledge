package com.chat.myAgent.agent;

import com.chat.myAgent.common.context.TraceContext;
import com.chat.myAgent.common.stream.StreamEvent;
import com.chat.myAgent.config.ModelConfig;
import com.chat.myAgent.model.vo.KnowledgeResponse;
import com.chat.myAgent.rag.RetrievalService;
import com.chat.myAgent.service.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class RagAgent {

    private final ChatClient ragChatClient;
    private final ChatClient baseChatClient;
    private final RetrievalService retrievalService;
    private final AuditService auditService;
    private final ModelConfig modelConfig;

    public RagAgent(
            @Qualifier("ragChatClient") ChatClient ragChatClient,
            @Qualifier("baseChatClient") ChatClient baseChatClient,
            RetrievalService retrievalService,
            AuditService auditService,
            ModelConfig modelConfig) {
        this.ragChatClient = ragChatClient;
        this.baseChatClient = baseChatClient;
        this.retrievalService = retrievalService;
        this.auditService = auditService;
        this.modelConfig = modelConfig;
    }

    public Flux<String> askStream(String question, String conversationId, boolean manual) {
        final String resolvedConversationId = resolveConversationId(conversationId);
        try {
            List<Document> relatedDocs = retrievalService.retrieve(question);
            List<String> sources = retrievalService.getSourceFiles(relatedDocs);

            if (relatedDocs.isEmpty()) {
                String emptyAnswer = "抱歉，知识库中暂无与您问题相关的信息。请尝试上传相关文档后再提问。";
                auditService.saveChatHistory(resolvedConversationId, "anonymous", "user", question, "rag-stream", modelConfig.getPrimaryModel(), null, null, 0L);
                auditService.saveChatHistory(resolvedConversationId, "anonymous", "assistant", emptyAnswer, "rag-stream", modelConfig.getPrimaryModel(), null, null, 0L);
                auditService.saveAgentInvocation(resolvedConversationId, manual ? "rag-manual-stream" : "rag-stream", modelConfig.getPrimaryModel(), question, emptyAnswer, null, "SUCCESS", 0L);
                return Flux.just(StreamEvent.start("开始检索知识库").toJson(), StreamEvent.message(emptyAnswer).toJson(), StreamEvent.done("完成").toJson());
            }

            StringBuilder context = new StringBuilder();
            context.append("以下是从知识库中检索到的相关参考资料：\n\n");
            for (int i = 0; i < relatedDocs.size(); i++) {
                Document doc = relatedDocs.get(i);
                String source = (String) doc.getMetadata().getOrDefault("source", "未知");
                context.append("[参考").append(i + 1).append("] 来源: ").append(source).append("\n");
                context.append(doc.getText()).append("\n\n");
            }
            context.append("---\n请基于以上参考资料回答用户的问题。如果参考资料中没有相关信息，请说明。\n");

            StringBuilder fullResponse = new StringBuilder();
            Flux<String> contentFlux = baseChatClient.prompt().system(context.toString()).user(question).stream().content();

            return Flux.concat(
                    Flux.just(StreamEvent.start("开始检索知识库").toJson()),
                    contentFlux.map(chunk -> {
                        fullResponse.append(chunk);
                        return StreamEvent.delta(chunk).toJson();
                    }),
                    Flux.just(StreamEvent.message(String.join("\n", sources)).toJson(), StreamEvent.done("完成").toJson())
            ).doFinally(signalType -> {
                String status = signalType == reactor.core.publisher.SignalType.ON_COMPLETE ? "SUCCESS" : "FAILED";
                auditService.saveChatHistory(resolvedConversationId, "anonymous", "user", question, "rag-stream", modelConfig.getPrimaryModel(), null, null, 0L);
                auditService.saveChatHistory(resolvedConversationId, "anonymous", "assistant", fullResponse.toString(), "rag-stream", modelConfig.getPrimaryModel(), null, null, 0L);
                auditService.saveAgentInvocation(resolvedConversationId, manual ? "rag-manual-stream" : "rag-stream", modelConfig.getPrimaryModel(), question, fullResponse.toString(), null, status, 0L);
            });
        } catch (Exception ex) {
            log.error("RagAgent askStream failed", ex);
            return Flux.just(StreamEvent.error("知识库流式问答失败: " + ex.getMessage()).toJson());
        }
    }

    public KnowledgeResponse ask(String question, String conversationId) {
        final String resolvedConversationId = resolveConversationId(conversationId);
        List<Document> relatedDocs = retrievalService.retrieve(question);
        List<String> sources = retrievalService.getSourceFiles(relatedDocs);

        if (relatedDocs.isEmpty()) {
            KnowledgeResponse response = KnowledgeResponse.builder().conversationId(resolvedConversationId).answer("抱歉，知识库中暂无与您问题相关的信息。请尝试上传相关文档后再提问。").sources(List.of()).retrievedChunks(0).model(modelConfig.getPrimaryModel()).traceId(TraceContext.getTraceId()).build();
            auditService.saveAgentInvocation(resolvedConversationId, "rag", modelConfig.getPrimaryModel(), question, response.getAnswer(), null, "SUCCESS", 0L);
            return response;
        }

        StringBuilder context = new StringBuilder();
        context.append("以下是从知识库中检索到的相关参考资料：\n\n");
        for (int i = 0; i < relatedDocs.size(); i++) {
            Document doc = relatedDocs.get(i);
            String source = (String) doc.getMetadata().getOrDefault("source", "未知");
            context.append("[参考").append(i + 1).append("] 来源: ").append(source).append("\n");
            context.append(doc.getText()).append("\n\n");
        }
        context.append("---\n请基于以上参考资料回答用户的问题。如果参考资料中没有相关信息，请说明。\n");

        String reply = baseChatClient.prompt().system(context.toString()).user(question).call().content();
        KnowledgeResponse response = KnowledgeResponse.builder().conversationId(resolvedConversationId).answer(reply).sources(sources).retrievedChunks(relatedDocs.size()).model(modelConfig.getPrimaryModel()).traceId(TraceContext.getTraceId()).build();
        auditService.saveAgentInvocation(resolvedConversationId, "rag", modelConfig.getPrimaryModel(), question, response.getAnswer(), null, "SUCCESS", 0L);
        return response;
    }

    public KnowledgeResponse askManual(String question, String conversationId) {
        conversationId = resolveConversationId(conversationId);
        List<Document> relatedDocs = retrievalService.retrieve(question);
        List<String> sources = retrievalService.getSourceFiles(relatedDocs);
        if (relatedDocs.isEmpty()) {
            KnowledgeResponse response = KnowledgeResponse.builder().conversationId(conversationId).answer("抱歉，知识库中暂无与您问题相关的信息。请尝试上传相关文档后再提问。").sources(List.of()).retrievedChunks(0).model(modelConfig.getPrimaryModel()).traceId(TraceContext.getTraceId()).build();
            auditService.saveAgentInvocation(conversationId, "rag-manual", modelConfig.getPrimaryModel(), question, response.getAnswer(), null, "SUCCESS", 0L);
            return response;
        }

        StringBuilder context = new StringBuilder();
        context.append("以下是从知识库中检索到的相关参考资料：\n\n");
        for (int i = 0; i < relatedDocs.size(); i++) {
            Document doc = relatedDocs.get(i);
            String source = (String) doc.getMetadata().getOrDefault("source", "未知");
            context.append("[参考").append(i + 1).append("] 来源: ").append(source).append("\n");
            context.append(doc.getText()).append("\n\n");
        }
        context.append("---\n请基于以上参考资料回答用户的问题。如果参考资料中没有相关信息，请说明。\n");

        String reply = baseChatClient.prompt().system(context.toString()).user(question).call().content();
        KnowledgeResponse response = KnowledgeResponse.builder().conversationId(conversationId).answer(reply).sources(sources).retrievedChunks(relatedDocs.size()).model(modelConfig.getPrimaryModel()).traceId(TraceContext.getTraceId()).build();
        auditService.saveAgentInvocation(conversationId, "rag-manual", modelConfig.getPrimaryModel(), question, response.getAnswer(), null, "SUCCESS", 0L);
        return response;
    }

    public String searchOnly(String query) { return retrievalService.retrieveFormatted(query); }

    private String resolveConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) return "rag-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return conversationId;
    }
}
