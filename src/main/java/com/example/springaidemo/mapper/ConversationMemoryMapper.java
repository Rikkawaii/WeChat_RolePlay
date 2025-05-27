package com.example.springaidemo.mapper;

import com.example.springaidemo.model.domain.ConversationMemory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author xwzy
* @description 针对表【conversation_memory】的数据库操作Mapper
* @createDate 2025-05-13 21:39:57
* @Entity com.example.springaidemo.model.domain.ConversationMemory
*/
public interface ConversationMemoryMapper extends BaseMapper<ConversationMemory> {
}




