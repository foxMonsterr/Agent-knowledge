package com.chat.myAgent.conversation.memory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ConversationMemoryService {

    private final ChatMemory chatMemory;

    public ConversationMemoryService(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    public String resolveConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return "conv-" + UUID.randomUUID().toString().replace("-", "");
        }
        return conversationId.trim();
    }

    public boolean isMemoryEnabled(Boolean memoryEnabled) {
        return memoryEnabled == null || memoryEnabled;
    }

    public List<Message> listMemoryMessages(String conversationId) {
        return chatMemory.get(conversationId);
    }

    public int memorySize(String conversationId) {
        List<Message> messages = listMemoryMessages(conversationId);
        return messages == null ? 0 : messages.size();
    }

    public void clearMemory(String conversationId) {
        if (conversationId != null && !conversationId.isBlank()) {
            chatMemory.clear(conversationId);
        }
    }
}
