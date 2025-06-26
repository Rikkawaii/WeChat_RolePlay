package com.example.springaidemo.service;

import com.example.springaidemo.model.domain.ConversationMemory;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author xwzy
* @description 针对表【conversation_memory】的数据库操作Service
* @createDate 2025-05-13 21:39:57
*/
public interface ConversationMemoryService extends IService<ConversationMemory> {
    List<ConversationMemory> getMessageByConversationId(String conversationId);
}
