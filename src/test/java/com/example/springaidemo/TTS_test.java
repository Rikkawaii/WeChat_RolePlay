package com.example.springaidemo;

import ch.qos.logback.classic.pattern.SyslogStartConverter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootTest
public class TTS_test {
    @Test
    public void testTTS() {
//        GptTTSManager.generateAudio("先生,もう遅いです,帰りましょう");
//        System.out.println("当前工作目录：" + System.getProperty("user.dir"));
        String text = "いいえ、今日は遠慮しておきます。でも…先生が行くなら、私も少し離れたところで見ています」\n" +
                "（赤い瞳を伏せて、少しだけ距離を取るようにしながら話しています）<宠溺>";
        String processedText = text.replaceAll("\\（.*?\\）", "");

        // 正则匹配<感情>标签内容
        Pattern pattern = Pattern.compile("<([^>]*)>");
        Matcher matcher = pattern.matcher(processedText);
        String emotion = "";
        // 如果匹配到<感情>标签
        if (matcher.find()) {
            // 获取<感情>标签内容
            emotion = matcher.group(1);
            // 移除整个<感情>标签
            String output = processedText.replaceAll("<[^>]*>", "");
        }
        System.out.println();
    }
}
