package com.example.springaidemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springaidemo.model.domain.ConversationMemory;
import com.example.springaidemo.service.ConversationMemoryService;
import com.example.springaidemo.mapper.ConversationMemoryMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author xwzy
* @description 针对表【conversation_memory】的数据库操作Service实现
* @createDate 2025-05-13 21:39:57
*/
@Service
public class ConversationMemoryServiceImpl extends ServiceImpl<ConversationMemoryMapper, ConversationMemory>
    implements ConversationMemoryService{

    @Override
    public List<ConversationMemory> getMessageByConversationId(String conversationId) {
        return this.lambdaQuery().eq(ConversationMemory::getConversationId, conversationId).list();
    }
}




