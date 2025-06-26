package com.example.springaidemo.rolestatus;

import com.example.springaidemo.model.UserSession;

import java.util.concurrent.ConcurrentHashMap;

import static com.example.springaidemo.rolestatus.RolePrompt.KAYOKO;

// 调用大模型获得回复前，通过这个类获得角色提示以及该角色的会话id
public class SessionManager {
    private static final ConcurrentHashMap<String, UserSession> sessionMap = new ConcurrentHashMap<>();

    public static UserSession getOrCreateSession(String wxid) {
        return sessionMap.computeIfAbsent(wxid, id -> {
            UserSession session = new UserSession();
            session.setWxid(id);
            session.setRole(KAYOKO.getRole()); // 默认角色设定为鬼方佳代子
            return session;
        });
    }
}
