package com.example.springaidemo.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Slf4j
public class TextHandler {
    public final static String BASE_PATH = "D:/新建文件夹/回复表情";
    public final static String DefaultRole = "佳代子";
    public final static String DefaultEmotion = "冷漠";

    /**
     * 处理ROLE_PLAY模式下回复的文本，包括删除 () 及其内容，匹配<感情>标签内容，并返回处理后的文本和情绪标签
     * @param text
     * @return
     */
    public static String handleTextForRolePlay(String text) {
        // 使用正则表达式删除所有 () 及其内容
        String processedText = text.replaceAll("\\（.*?\\）", "");

        // 正则匹配<感情>标签内容
        Pattern pattern = Pattern.compile("<([^>]*)>");
        Matcher matcher = pattern.matcher(processedText);
        String emotion = "";
        String output = processedText;
        // 如果匹配到<感情>标签
        if (matcher.find()) {
            // 获取<感情>标签内容
            emotion = matcher.group(1);
            // 移除整个<感情>标签
            output = processedText.replaceAll("<[^>]*>", "");
        }
        return output + '|' + emotion;
    }

    /**
     * 根据emotion返回对应情绪的图片,设置70%的概率返回图片
     * @param emotion 情绪标签
     * @param role 当前角色
     * @return 图片字节数组
     */
    public static byte[] handleEmotion(String emotion, String role) {
        if (Math.random() < 0.3) {
            return null;
        }
        if("".equals(role)){
            role = DefaultRole;
        }
        String imagePath = BASE_PATH + "/" + role + "/" + emotion + ".jpg";
        byte[] imageBytes = null;
        try {
            imageBytes = Files.readAllBytes(Paths.get(imagePath));
        } catch (IOException e) {
            log.error("图片不存在：{}", imagePath);
        }
        return imageBytes;
    }
}
