package com.example.springaidemo.manage;

import cn.hutool.core.map.MapUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import org.springframework.stereotype.Component;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.example.springaidemo.rolestatus.RolePrompt.ARONA;
import static com.example.springaidemo.rolestatus.RolePrompt.KAYOKO;

// 文本转对应模型语音
@Slf4j
@Component
public class GptTTSManager {
    private static final String baseUrl = "http://localhost:9880";
    private static final String CHINESE = "zh";
    private static final String JAPANESE = "ja";

    public static byte[] generateAudio(String text, String role) {
        Map<String, Object> params;
        if(role.equals(KAYOKO.getRole())) {
            params = MapUtil.builder(new HashMap<String, Object>())
                    .put("text_lang", JAPANESE)
                    .put("ref_audio_path", "D:/新建文件夹/ckyp.wav")
                    .put("prompt_lang", JAPANESE)
                    .put("prompt_text", "先生の依頼なら、いつでも歓迎だよ")
                    .put("text_split_method", "cut1")//四句一切)
                    .put("batch_size", 1)
                    .build();
        }else if(role.equals(ARONA.getRole())) {
            params = MapUtil.builder(new HashMap<String, Object>())
                    .put("text", text)
                    .put("text_lang", CHINESE)
                    .put("ref_audio_path", "D:/AI语音模型参考音频/阿罗娜/参考音频2(中文)/arona_work_talk_5.wav")
                    .put("prompt_lang", CHINESE)
                    .put("prompt_text", "偶尔也要为自己的健康着想哦，老师！我会很担心的")
                    .put("text_split_method", "cut1")//四句一切)
                    .put("batch_size", 1)
                    .build();
        }else {
            params = MapUtil.builder(new HashMap<String, Object>())
                    .put("text", text)
                    .put("text_lang", JAPANESE)
                    .put("ref_audio_path","D:/AI语音模型参考音频/六花/新しい拠点に移ったらもうここには来られない_.wav")
//                    .put("ref_audio_path","D:/AI语音模型参考音频/六花/魔界に動きがあってもユータの部屋に報告に行けないずっと一緒にいたくないのか.wav")
                    .put("prompt_lang", JAPANESE)
                    .put("prompt_text", "新しい拠点に移ったらもうここには来られない_")
//                    .put("prompt_text", "魔界に動きがあってもユータの部屋に報告に行けないずっと一緒にいたくないのか")
                    .put("text_split_method", "cut1")//四句一切)
                    .put("batch_size", 2)
                    .build();
        }

        // 返回音频字节流需特殊处理
        byte[] audioData = HttpRequest.get(baseUrl + "/tts")
                .header(Header.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8")
                .form(params)
                .execute()
                .bodyBytes();
        log.info("获得音频数据长度：" + audioData.length);
        return audioData;
    }

    public static String saveAudio(byte[] audioData) {
        long currentTimeMillis = System.currentTimeMillis();
        String filePath = "D:/新建文件夹/生成音频/" + currentTimeMillis + ".wav";
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(audioData);
        } catch (IOException e) {
            throw new RuntimeException("文件写入失败！原因：" + e.getMessage(), e);
        }
        return filePath;
    }


    public static void switchGptModel(String modelPath) {
        HttpResponse httpResponse = HttpRequest.get(baseUrl + "/set_gpt_weights")
                .form(MapUtil.of("weights_path", modelPath))
                .execute();
        if (httpResponse.isOk()) {
            log.info("GPT模型切换成功"); // 成功返回"success"
        } else {
            log.info("GPT模型切换失败"); // 失败返回错误信息
        }
    }

    public static void switchSOVITSModel(String modelPath) {
        HttpResponse httpResponse = HttpRequest.get(baseUrl + "/set_sovits_weights")
                .form(MapUtil.of("weights_path", modelPath))
                .execute();
        if (httpResponse.isOk()) {
            log.info("SOVITS模型切换成功"); // 成功返回"success"
        } else {
            log.info("SOVITS模型切换失败"); // 失败返回错误信息
        }
    }

    /**
     * 将wav格式的音频转换为amr格式的音频，并转换为Base64字符串返回
     *
     * @param audioData
     * @return
     */
    public static byte[] transformToBytesAmr(byte[] audioData) {
        // 1. 先保存音频为wav格式
        String wavAudioPath = saveAudio(audioData);
        // 2. 转换为 AMR 格式
        Encoder encoder = new Encoder();
        File source = new File(wavAudioPath);
        File target = null;
        byte[] amrData = null;
        try {
            target = File.createTempFile("audio_", ".amr");
            // 2.1 设置音频属性
            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("libopencore_amrnb");  // AMR-NB编码器（微信官方推荐）
            audio.setBitRate(12200);               // 4.75kbps（平衡体积与可懂度）
            audio.setChannels(1);                 // 单声道（强制）
            audio.setSamplingRate(8000);          // 8kHz（AMR-NB标准）
            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("amr");
            attrs.setAudioAttributes(audio);
            // 2.2 执行转换
            encoder.encode(new MultimediaObject(source), target, attrs);
            // 3. 读取转换后的 AMR 格式文件
            amrData = Files.readAllBytes(target.toPath());
        } catch (EncoderException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }finally {
            // 4. 删除临时文件
            if (target != null && target.exists()) target.delete();
        }
        return amrData;
    }

    /**
     * 输入wav格式的音频字节
     * @param audioData
     * @return
     */
    public static byte[] transformToBytesSilk(byte[] audioData) {
        // 1. 先保存音频为wav格式
        String wavAudioPath = saveAudio(audioData);
        // 临时PCM文件
        Path tempPcm = null;
        Path outputSilk = null;
        byte[] silkBytes = null;
        try {
            tempPcm = Files.createTempFile("audio_", ".pcm");
            outputSilk = Files.createTempFile("audio_", ".silk");
            // Step 1: WAV -> PCM
            ProcessBuilder ffmpeg = new ProcessBuilder(
                    "ffmpeg",
                    "-i", wavAudioPath,
                    "-ar", "16000",
                    "-ac", "1",
                    "-f", "s16le",
                    "-fflags", "+bitexact",
                    tempPcm.toString(),
                    "-y"
            );
            runProcess(ffmpeg, "FFmpeg");

            // Step 2: PCM -> SILK
            ProcessBuilder silkEncoder = new ProcessBuilder(
                    "D:/新建文件夹/silk2mp3-full/silk_v3_encoder",
                    tempPcm.toString(),
                    outputSilk.toString(),
                    "-Fs_API", "16000",
                    "-Fs_maxInternal", "16000",
                    "-tencent"
            );
            runProcess(silkEncoder, "Silk Encoder");
            silkBytes = Files.readAllBytes(outputSilk);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            // 4. 删除临时文件pcm和silk
            if (tempPcm != null && tempPcm.toFile().exists()) tempPcm.toFile().delete();
            if (outputSilk != null && outputSilk.toFile().exists()) outputSilk.toFile().delete();
        }
        return silkBytes;
    }

    private static void runProcess(ProcessBuilder pb, String toolName) {
        pb.redirectErrorStream(true);
        try {
            Process process = pb.start();
            // 打印日志
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info(toolName + ": " + line);
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException(toolName + " failed with code " + exitCode);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
