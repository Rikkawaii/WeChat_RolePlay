package com.example.springaidemo.model;

import lombok.Data;

@Data
public class PixivPictureDTO {
    private String id; // 图片ID
    private String title; // 图片标题
    private String authorId; // 作者id
    private String authorName; // 作者名称
    private String url; // 图片URL
}
