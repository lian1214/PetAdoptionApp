package com.lian.petadoption.dao;

import java.io.Serializable;

public class Comment implements Serializable {
    private int id;
    private int checkInId;
    private String username;
    private String content;
    private String time;

    // --- 新增扩展字段 (不存入评论表，仅用于显示) ---
    private String nickname; // 评论人的昵称
    private String avatar;   // 评论人的头像

    public Comment() {}

    // Getter & Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCheckInId() { return checkInId; }
    public void setCheckInId(int checkInId) { this.checkInId = checkInId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
}