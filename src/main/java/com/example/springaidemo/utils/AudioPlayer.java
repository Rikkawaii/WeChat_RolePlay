package com.example.springaidemo.utils;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;

// 播放音频
public class AudioPlayer {
    public static void playAudioBytes(byte[] audioBytes) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        // 1.将字节数组转为字节输入流(内存)
        ByteArrayInputStream bis = new ByteArrayInputStream(audioBytes);
        // 2.
        try(AudioInputStream audioStream = AudioSystem.getAudioInputStream(bis)){
            // 3.获取音频格式信息
            AudioFormat format = audioStream.getFormat();
            // 4.创建音频线信息对象
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            // 5.打开音频线
            try(SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)){
                line.open(format);
                line.start();
                // 6.读取音频数据
                byte[] buffer = new byte[4096];
                int nBytesRead = 0;
                while ((nBytesRead = audioStream.read(buffer)) != -1) {
                    line.write(buffer, 0, nBytesRead);
                }
                line.drain();
            }
        }
    }
}
