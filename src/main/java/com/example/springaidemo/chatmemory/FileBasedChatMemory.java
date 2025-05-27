package com.example.springaidemo.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
// 基于文件的会话记录存储
public class FileBasedChatMemory implements ChatMemory {
    private final String CHAT_MEMORY_BASE_PATH;
    private static final ConcurrentMap<String, Kryo> conversationKryoMap = new ConcurrentHashMap<>();

    public static Kryo getKryoForConversation(String conversationId) {
        return conversationKryoMap.computeIfAbsent(conversationId, id -> {
            Kryo kryo = new Kryo();
            kryo.setRegistrationRequired(false);
            kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
            return kryo;
        });
    }

    public FileBasedChatMemory(String basePath) {
        CHAT_MEMORY_BASE_PATH = basePath;
        File chatMemoryDir = new File(CHAT_MEMORY_BASE_PATH);
        if(!chatMemoryDir.exists()){
            chatMemoryDir.mkdirs();
        }
    }
    @Override
    public void add(String conversationId, List<Message> messages) {
         List<Message> all = getOrCreateConversation(conversationId);
         all.addAll(messages);
         saveConversation(conversationId, all);
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<Message> all = getOrCreateConversation(conversationId);
        return all.stream().skip((long)Math.max(0, all.size() - lastN)).toList();
    }


    private List<Message> getOrCreateConversation(String conversationId) {
        File file = getConversationFile(conversationId);
        List<Message> messages = new ArrayList<>();
        if (file.exists()) {
            try (Input input = new Input(new FileInputStream(file))) {
                Kryo kryo = getKryoForConversation(conversationId);
                messages = kryo.readObject(input, ArrayList.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return messages;
    }


    private void saveConversation(String conversationId, List<Message> all) {
        File file = getConversationFile(conversationId);
        try (Output output = new Output(new FileOutputStream(file))) {
            Kryo kryo = getKryoForConversation(conversationId);
            kryo.writeObject(output, all);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private File getConversationFile(String conversationId) {
        return new File(CHAT_MEMORY_BASE_PATH , conversationId + ".kryo");
    }

    @Override
    public void clear(String conversationId) {
         File file = getConversationFile(conversationId);
         if(file.exists()){
             file.delete();
         }
    }
}
