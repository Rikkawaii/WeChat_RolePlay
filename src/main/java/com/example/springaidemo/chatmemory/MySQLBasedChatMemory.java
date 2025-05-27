package com.example.springaidemo.chatmemory;

import com.alibaba.nacos.shaded.com.google.gson.Gson;
import com.example.springaidemo.model.domain.ConversationMemory;
import com.example.springaidemo.model.eunm.MessageTypeEnum;
import com.example.springaidemo.service.ConversationMemoryService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MySQLBasedChatMemory implements ChatMemory {
    @Resource
    private ConversationMemoryService conversationMemoryService;

    @Override
    public void add(String conversationId, List<Message> messages) {
        Gson gson = new Gson();
        List<ConversationMemory> conversationMemories = messages.stream()
                .map(message -> {
                    String type = message.getMessageType().getValue();
                    String msg = gson.toJson(message);
                    return ConversationMemory.builder()
                            .conversationId(conversationId)
                            .type(type)
                            .message(msg).build();
                }).toList();
        conversationMemoryService.saveBatch(conversationMemories);
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<ConversationMemory> memories = conversationMemoryService.getMessageByConversationId(conversationId);
        return memories.stream()
                .skip((long) Math.max(0, memories.size() - lastN))
                .map(memory -> {
                    String typeValue = memory.getType();
                    Gson gson = new Gson();
                    return (Message) gson.fromJson(memory.getMessage(), MessageTypeEnum.fromValue(typeValue).getClazz());
                }).toList();
    }

    @Override
    public void clear(String conversationId) {
        conversationMemoryService.removeById(conversationId);
    }
}
