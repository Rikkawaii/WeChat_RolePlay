package com.example.springaidemo.rolestatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.example.springaidemo.rolestatus.RolePrompt.*;

public class RoleRepository {

    // 存储角色名称以及对应[角色会话id, 角色prompt]
    private static final Map<String, String[]> rolePrompts = new HashMap<>();
    // 存储角色名称以及对应的语音模型路径
    private static final Map<String, String[]> roleVoiceModels = new HashMap<>();


    static {
        rolePrompts.put(KAYOKO.getRole(), new String[]{KAYOKO.getConversationIdSuffix(), KAYOKO.getPrompt()});
        rolePrompts.put(ARONA.getRole(), new String[]{ARONA.getConversationIdSuffix(), ARONA.getPrompt()});
        rolePrompts.put(RIKKA.getRole(), new String[]{RIKKA.getConversationIdSuffix(), RIKKA.getPrompt()});
        roleVoiceModels.put(KAYOKO.getRole(), new String[]{KAYOKO.getGPT_model_path(), KAYOKO.getSoVITS_model_path()});
        roleVoiceModels.put(ARONA.getRole(), new String[]{ARONA.getGPT_model_path(), ARONA.getSoVITS_model_path()});
        roleVoiceModels.put(RIKKA.getRole(), new String[]{RIKKA.getGPT_model_path(), RIKKA.getSoVITS_model_path()});
    }

    /**
     * 根据角色名称获得角色提示词和角色会话id,用于模型调用配置
     * @param roleName 角色名称
     * @return
     */
    public static Optional<String[]> getRolePrompt(String roleName) {
        return Optional.ofNullable(rolePrompts.get(roleName));
    }
    /**
     * 根据角色名称获得角色语音模型
     */
    public static Optional<String[]> getRoleModel(String roleName) {
        return Optional.ofNullable(roleVoiceModels.get(roleName));
    }

    /**
     * 获得角色列表
     * * @return
     */
    public static Set<String> getAllRoles() {
        return rolePrompts.keySet();
    }
}