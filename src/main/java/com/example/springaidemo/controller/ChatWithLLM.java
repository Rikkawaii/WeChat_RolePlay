package com.example.springaidemo.controller;

import com.example.springaidemo.common.BaseResponse;
import com.example.springaidemo.common.ResultUtils;
import com.example.springaidemo.config.RoleConfig;
import com.example.springaidemo.manage.GptTTSManager;
import com.example.springaidemo.model.TextAndAudioPathDTO;
import com.example.springaidemo.model.RolePlayReplyDTO;
import com.example.springaidemo.utils.AudioPlayer;
import com.example.springaidemo.utils.TextHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
// 对于final属性可以使用该注解注入
@RequestMapping("/chat")
public class ChatWithLLM {
    private ChatClient rolePlayChatClient; // 默认是佳代子Prompt
    private ChatClient commonChatClient;


    public ChatWithLLM(@Qualifier("rolePlayChatClient") ChatClient rolePlayChatClient, @Qualifier("commonChatClient") ChatClient commonChatClient) {
        this.rolePlayChatClient = rolePlayChatClient;
        this.commonChatClient = commonChatClient;
    }

    @Value("classpath:/role_prompt/Yukino.md")
    private Resource systemResource;

    @GetMapping("")
    public BaseResponse<String> chatWithLLM(@RequestParam("message") String message) {
        String text = commonChatClient.prompt().user(message).advisors(
                advisor -> advisor
                        .param("chat_memory_response_size", "10")
        ).call().content();
        return ResultUtils.success(text);
    }


    /**
     * 供测试使用，调用大模型回复，合成音频，保存音频，自动播放音频
     *
     * @param message
     * @return
     */
    @GetMapping("/test")
    public BaseResponse<String> ChatWithRoleForTest(@RequestParam("message") String message) {
        // 大模型回复
        String text = rolePlayChatClient.prompt().user(message).advisors().call().content();
        log.info("大模型回复：" + text);
        // 保存音频(可选)
        String modify_text = TextHandler.handleTextForRolePlay(text);
        log.info("处理后的文本：" + modify_text);
        // tts合成
        byte[] audioData = GptTTSManager.generateAudio(modify_text);
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
     * <p>
     * 供python的wxAuto调用
     *
     * @param message wxAuto监听到的消息
     * @return 返回大模型回复和合成的音频路径
     */
    @GetMapping("/wx/auto")
    public BaseResponse<TextAndAudioPathDTO> ChatWithRoleForWxAuto(@RequestParam("message") String message) {
        // 大模型回复
        String text = rolePlayChatClient.prompt().user(message).call().content();
        log.info("大模型回复：" + text);
        // 处理文本，输出（）的内容，只保留[]里的内容
        String modify_text = TextHandler.handleTextForRolePlay(text);
        log.info("处理后的文本：" + modify_text);
        // tts合成
        byte[] audioData = GptTTSManager.generateAudio(modify_text);
        // 保存音频
        String audioPath = GptTTSManager.saveAudio(audioData);
        TextAndAudioPathDTO textAndAudioPathDTO = new TextAndAudioPathDTO();
        textAndAudioPathDTO.setText(text);
        textAndAudioPathDTO.setAudioPath(audioPath);
        return ResultUtils.success(textAndAudioPathDTO);
    }

    @GetMapping("/wx/pad")
    public BaseResponse<RolePlayReplyDTO> ChatWithRoleForWxPad(@RequestParam("message") String message) {
        // 大模型回复
        String text = rolePlayChatClient.prompt().user(message)
                .advisors(advisor -> advisor.param("chat_memory_conversation_id", "68")
                        .param("chat_memory_response_size", 30))
                .call().content();
        log.info("大模型回复：" + text);
        // 处理文本，输出（）的内容，只保留[]里的内容
        String modifyText = TextHandler.handleTextForRolePlay(text);
        log.info("回复+感情：" + modifyText);
        String[] strings = modifyText.split("\\|");
        String emotion = strings[1];
        // 这里的role设置为""，默认是佳代子，后续可将当前角色绑定在token上（或session，登录信息之类的，或对话id？？）
        byte[] emotionData = TextHandler.handleEmotion(emotion, "");
        String replyText = strings[0];
        // tts合成(这里得到得是wav格式字节数组)
        byte[] audioData = GptTTSManager.generateAudio(replyText);
        // 获得base64编码的amr格式音频（其中还保存了wav，amr格式的音频）
        byte[] amrData = GptTTSManager.transformToBytesSilk(audioData);
        RolePlayReplyDTO rolePlayReplyDTO = new RolePlayReplyDTO();
        rolePlayReplyDTO.setText(text);
        rolePlayReplyDTO.setImageData(emotionData);
        rolePlayReplyDTO.setVoiceData(amrData);
        return ResultUtils.success(rolePlayReplyDTO);
    }
}
