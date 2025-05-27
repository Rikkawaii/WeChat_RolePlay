package com.example.springaidemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "roles")
@PropertySource("classpath:role-config.properties") // 无需自定义工厂类
public class RoleConfig {
    private Map<String, RoleInfo> roles = new HashMap<>();

    // getter和setter
    public static class RoleInfo {
        private String promptFile;
        private String modelA1;
        private String modelA2;

        // getters和setters
    }

    // Map的getter和setter
    public Map<String, RoleInfo> getRoles() {
        return roles;
    }

    public void setRoles(Map<String, RoleInfo> roles) {
        this.roles = roles;
    }
}
