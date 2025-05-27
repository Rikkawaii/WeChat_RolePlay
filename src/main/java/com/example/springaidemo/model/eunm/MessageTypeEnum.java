package com.example.springaidemo.model.eunm;

import lombok.Getter;
import org.springframework.ai.chat.messages.*;
// 改自org.springframework.ai.chat.messages;

public enum MessageTypeEnum {

    USER("user", UserMessage.class),
    ASSISTANT("assistant", AssistantMessage.class),
    SYSTEM("system", SystemMessage.class),
    TOOL("tool", ToolResponseMessage.class);

    @Getter
    private final String value;
    private final Class<? extends Message> clazz;

    MessageTypeEnum(String value, Class<? extends Message> clazz) {
        this.value = value;
        this.clazz = clazz;
    }

    // 根据value获取枚举类型
    public static MessageTypeEnum fromValue(String value) {
        for (MessageTypeEnum type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid message type: " + value);
    }

    public String getValue() {
        return value;
    }

    public Class<? extends Message> getClazz() {
        return clazz;
    }
}
