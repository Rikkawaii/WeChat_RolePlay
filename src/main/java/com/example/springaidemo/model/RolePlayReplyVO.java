package com.example.springaidemo.model;

import lombok.Data;

@Data
public class RolePlayReplyVO {
    private String text;
    private byte[] imageData;
    private byte[] voiceData;
}
