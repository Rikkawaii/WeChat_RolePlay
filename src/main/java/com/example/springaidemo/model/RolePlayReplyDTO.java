package com.example.springaidemo.model;

import lombok.Data;

@Data
public class RolePlayReplyDTO {
    private String text;
    private byte[] imageData;
    private byte[] voiceData;
}
