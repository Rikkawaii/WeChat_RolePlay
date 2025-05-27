package com.example.springaidemo.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 
 * @TableName conversation_memory
 */
@TableName(value ="conversation_memory")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConversationMemory implements Serializable {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 对话ID
     */
    @TableField(value = "conversation_id")
    private String conversationId;

    /**
     * 类型
     */
    @TableField(value = "type")
    private String type;

    /**
     * 消息内容（json,包括textContent,media,metadata等内容）
     */
    @TableField(value = "message")
    private String message;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private Date updateTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    @TableField(value = "is_delete")
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}