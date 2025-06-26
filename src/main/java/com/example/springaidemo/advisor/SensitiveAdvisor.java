package com.example.springaidemo.advisor;

import com.example.springaidemo.exception.BusinessException;
import com.example.springaidemo.exception.ErrorCode;
import com.github.houbb.sensitive.word.core.SensitiveWordHelper;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import reactor.core.publisher.Flux;

public class SensitiveAdvisor implements CallAdvisor, StreamAdvisor {
    private int order;
    private static final String[] DEFAULT_SENSITIVE_WORDS = {"色情", "赌博"};
    private final String[] sensitiveWords;

    public SensitiveAdvisor() {
        this(0);
    }

    public SensitiveAdvisor(int order) {
        this(order, DEFAULT_SENSITIVE_WORDS);
    }

    public SensitiveAdvisor(int order, String[] sensitiveWords) {
        this.order = order;
        this.sensitiveWords = sensitiveWords;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        chatClientRequest = this.before(chatClientRequest);
        return callAdvisorChain.nextCall(chatClientRequest);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        chatClientRequest = this.before(chatClientRequest);
        return streamAdvisorChain.nextStream(chatClientRequest);
    }


    public ChatClientRequest before(ChatClientRequest chatClientRequest) {
        String userText = chatClientRequest.prompt().getUserMessage().getText();
        String systemText = chatClientRequest.prompt().getSystemMessage().getText();
        for (String sensitiveWord : sensitiveWords) {
            if (userText.contains(sensitiveWord) || systemText.contains(sensitiveWord)) {
                throw new BusinessException(ErrorCode.SENSITIVE_DATA_ACCESS_DENIED, "敏感词[" + sensitiveWord + "]");
            }
        }
        String userSensitiveWord = SensitiveWordHelper.findFirst(userText);
        String systemSensitiveWord = SensitiveWordHelper.findFirst(systemText);
        if (userSensitiveWord != null || systemSensitiveWord != null) {
            throw new BusinessException(ErrorCode.SENSITIVE_DATA_ACCESS_DENIED, "敏感词[" + (userSensitiveWord != null ? userSensitiveWord : systemSensitiveWord) + "]");
        }
        return chatClientRequest;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return this.order;
    }
}
