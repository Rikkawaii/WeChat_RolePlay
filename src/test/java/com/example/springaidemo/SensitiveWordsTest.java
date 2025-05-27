package com.example.springaidemo;


import com.github.houbb.sensitive.word.core.SensitiveWordHelper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SensitiveWordsTest {
    // 敏感词检测
    @Test
    public void testSensitiveWords() {
        String text = "赌博是性爱不色情的";
        System.out.println(SensitiveWordHelper.findAll(text));
    }
}
