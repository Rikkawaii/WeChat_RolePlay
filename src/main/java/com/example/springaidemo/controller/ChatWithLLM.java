package com.example.springaidemo.controller;

import com.example.springaidemo.common.BaseResponse;
import com.example.springaidemo.common.ResultUtils;
import com.example.springaidemo.exception.BusinessException;
import com.example.springaidemo.exception.ErrorCode;
import com.example.springaidemo.manage.GptTTSManager;
import com.example.springaidemo.model.RoleChangeDTO;
import com.example.springaidemo.model.RolePlayReplyVO;
import com.example.springaidemo.model.TextAndAudioPathVO;
import com.example.springaidemo.model.UserSession;
import com.example.springaidemo.rolestatus.RolePrompt;
import com.example.springaidemo.rolestatus.RoleRepository;
import com.example.springaidemo.rolestatus.SessionManager;
import com.example.springaidemo.utils.AudioPlayer;
import com.example.springaidemo.utils.TextHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.util.Optional;

import static com.example.springaidemo.rolestatus.RolePrompt.KAYOKO;

@Slf4j
@RestController
// 对于final属性可以使用该注解注入
@RequestMapping("/chat")
public class ChatWithLLM {
    private final ChatClient rolePlayChatClient; // 默认是佳代子Prompt
    private final ChatClient commonChatClient;

    // 以下四个属性只在/chat这个api中使用过,用于测试新功能，可忽略

    private final VectorStore myVectorStoreLocal;

    private final DocumentRetriever myDocumentRetrieverCloud;

    private final ToolCallbackProvider toolCallbackProvider;

    private final ToolCallback[] allTools;


    public ChatWithLLM(@Qualifier("rolePlayChatClient") ChatClient rolePlayChatClient,
                       @Qualifier("commonChatClient") ChatClient commonChatClient,
                       @Qualifier("myVectorStoreLocal") VectorStore myVectorStoreLocal,
                       @Qualifier("myDocumentRetrieverCloud") DocumentRetriever myDocumentRetrieverCloud,
                       ToolCallbackProvider toolCallbackProvider,
                       ToolCallback[] allTools
    ) {
        this.rolePlayChatClient = rolePlayChatClient;
        this.commonChatClient = commonChatClient;
        this.myVectorStoreLocal = myVectorStoreLocal;
        this.myDocumentRetrieverCloud = myDocumentRetrieverCloud;
        this.toolCallbackProvider = toolCallbackProvider;
        this.allTools = allTools;
    }

    /**
     * 测试新功能
     * @param message
     * @return
     */
    @GetMapping("")
    public BaseResponse<String> chatWithLLM(@RequestParam("message") String message) {
        /**
         QuestionAnswerAdvisor
         */
//        String text = commonChatClient.prompt().user(message).advisors(
//                advisor -> advisor
//                        .param("chat_memory_response_size", "10")
//        ).advisors(
//                QuestionAnswerAdvisor.builder(myVectorStoreLocal)
//                        .searchRequest(SearchRequest.builder().similarityThreshold(0.5f).topK(4).build())
//                        .build()
//        ).call().content();
        /**
         * RetrievalAugmentationAdvisor
         */
//        String text = commonChatClient.prompt().user(message).advisors(
//                advisor -> advisor
//                        .param("chat_memory_response_size", "10")
//        ).advisors(
//                RetrievalAugmentationAdvisor.builder()
//                        .documentRetriever(VectorStoreDocumentRetriever.builder()
//                                .similarityThreshold(0.50)
//                                .topK(4)
//                                .vectorStore(myVectorStoreLocal)
//                                .build())
//                        .queryAugmenter(ContextualQueryAugmenter.builder().allowEmptyContext(true).build())
//                        .build()
//        ).call().content();

//        String text = commonChatClient.prompt().user(message).advisors(
//                advisor -> advisor
//                        .param("chat_memory_response_size", "10")
//        ).advisors(
//                RetrievalAugmentationAdvisor.builder()
//                        .documentRetriever(myDocumentRetrieverCloud)
//                        .queryAugmenter(ContextualQueryAugmenter.builder().allowEmptyContext(true).build())
//                        .build()
//        ).call().content();
        String text = commonChatClient.prompt().user(message).advisors(
                        advisor -> advisor
                                .param("chat_memory_response_size", "10"))
                .toolCallbacks(allTools)
                .toolCallbacks(toolCallbackProvider)
                .call()
                .content();
        return ResultUtils.success(text);
    }

    /**
     * 通过该请求输入消息，调用大模型回复，合成音频，保存音频，自动播放音频（快速体验，不需要使用py模块）
     *
     * @param message
     * @return
     */
    @GetMapping("/quick")
    public BaseResponse<String> ChatWithRoleForTest(@RequestParam("message") String message) {
        // 大模型回复
        String text = rolePlayChatClient.prompt().user(message).advisors().call().content();
        log.info("大模型回复：" + text);
        // 保存音频(可选)
        String modify_text = TextHandler.handleTextForRolePlay(text);
        log.info("处理后的文本：" + modify_text);
        // tts合成
        byte[] audioData = GptTTSManager.generateAudio(modify_text, "Kayoko");
        GptTTSManager.saveAudio(audioData);
        // 播放音频
        try {
            AudioPlayer.playAudioBytes(audioData);
        } catch (UnsupportedAudioFileException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
        return ResultUtils.success(text);
    }

    /**
     * todo: 增加请求参数role,用于切换角色Prompt和角色语音模型。支持用户多会话
     * 供python的wxAuto调用
     *
     * @param message wxAuto监听到的消息
     * @return 返回大模型回复和合成的音频路径
     */
    @GetMapping("/wx/auto")
    public BaseResponse<TextAndAudioPathVO> ChatWithRoleForWxAuto(@RequestParam("message") String message) {
        // 大模型回复
        String text = rolePlayChatClient.prompt().user(message).call().content();
        log.info("大模型回复：" + text);
        // 处理文本，输出（）的内容，只保留[]里的内容
        String modify_text = TextHandler.handleTextForRolePlay(text);
        log.info("处理后的文本：" + modify_text);
        // tts合成
        byte[] audioData = GptTTSManager.generateAudio(modify_text, "Kayoko");
        // 保存音频
        String audioPath = GptTTSManager.saveAudio(audioData);
        TextAndAudioPathVO textAndAudioPathDTO = new TextAndAudioPathVO();
        textAndAudioPathDTO.setText(text);
        textAndAudioPathDTO.setAudioPath(audioPath);
        return ResultUtils.success(textAndAudioPathDTO);
    }

    //    @GetMapping("/wx/pad")
//    public BaseResponse<RolePlayReplyVO> ChatWithRoleForWxPad(@RequestParam("message") String message) {
//        // 大模型回复
//        String text = rolePlayChatClient.prompt().user(message)
//                .advisors(advisor -> advisor.param(ChatMemory.DEFAULT_CONVERSATION_ID, "68")
//                        .param("chat_memory_response_size", 30))
//                .call().content();
//        log.info("大模型回复：" + text);
//        // 处理文本，删除（）的内容，只保留[]里的内容
//        String modifyText = TextHandler.handleTextForRolePlay(text);
//        log.info("回复+感情：" + modifyText);
//        String[] strings = modifyText.split("\\|");
//        String emotion = strings[1];
//        // 这里的role设置为""，默认是佳代子，后续可将当前角色绑定在token上（或session，登录信息之类的，或对话id？？）
//        byte[] emotionData = TextHandler.handleEmotion(emotion, "");
//        String replyText = strings[0];
//        // tts合成(这里得到得是wav格式字节数组)
//        byte[] audioData = GptTTSManager.generateAudio(replyText);
//        // 获得base64编码的silk格式音频字节
//        byte[] silkData = GptTTSManager.transformToBytesSilk(audioData);
//        RolePlayReplyVO rolePlayReplyDTO = new RolePlayReplyVO();
//        rolePlayReplyDTO.setText(text);
//        rolePlayReplyDTO.setImageData(emotionData);
//        rolePlayReplyDTO.setVoiceData(silkData);
//        return ResultUtils.success(rolePlayReplyDTO);
//    }
    @GetMapping("/wx/pad")
    public BaseResponse<RolePlayReplyVO> ChatWithRoleForWxPad(@RequestParam("message") String message, @RequestParam("wxid") String wxid) {
        // 根据用户状态获得角色prompt和会话id
        UserSession session = SessionManager.getOrCreateSession(wxid);
        String role = session.getRole();
        Optional<String[]> rolePrompt = RoleRepository.getRolePrompt(role);
        String[] promptAndId = rolePrompt.orElse(null);
        final String conversationId = promptAndId != null
                ? wxid + "_" + promptAndId[0]
                : wxid + "_" + KAYOKO.getConversationIdSuffix();
        final String prompt = promptAndId != null
                ? promptAndId[1]
                : KAYOKO.getPrompt();


        // 大模型回复
        String text = rolePlayChatClient.prompt().user(message).system(prompt)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call().content();
        log.info("大模型回复：" + text);
        // 处理文本，删除()和[]的内容
        String modifyText = TextHandler.handleTextForRolePlay(text);
        log.info("纯回复：" + modifyText);
        // tts合成(这里得到得是wav格式字节数组)
        byte[] audioData = GptTTSManager.generateAudio(modifyText, role);
        // 获得base64编码的silk格式音频字节
        byte[] silkData = GptTTSManager.transformToBytesSilk(audioData);
        RolePlayReplyVO rolePlayReplyVO = new RolePlayReplyVO();
        rolePlayReplyVO.setText(text);
        rolePlayReplyVO.setVoiceData(silkData);
        return ResultUtils.success(rolePlayReplyVO);
    }

    /**
     * 角色切换
     * @param roleChangeDTO
     * @return
     */
    @PostMapping("/role")
    public BaseResponse<Boolean> ChangeRole(@RequestBody RoleChangeDTO roleChangeDTO) {
        String wxid = roleChangeDTO.getWxid();
        String role = roleChangeDTO.getRole();
        if (SessionManager.getOrCreateSession(wxid).getRole().equals(role)){
            return ResultUtils.success(true);
        }
        // 切换角色语音模型
        Optional<String[]> roleModel = RoleRepository.getRoleModel(role);
        if (roleModel.isPresent()) {
            GptTTSManager.switchGptModel(roleModel.get()[0]);
            GptTTSManager.switchSOVITSModel(roleModel.get()[1]);
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "角色不存在");
        }

        log.info("用户{}的{}语音模型切换成功!", wxid, role);

        // 设置用户状态
        UserSession session = SessionManager.getOrCreateSession(wxid);
        session.setRole(role);

        return ResultUtils.success(true);
    }
}
