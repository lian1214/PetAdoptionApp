package com.lian.petadoption.dao;

import java.io.Serializable;

public class Comment implements Serializable {
    private int id;
    private int checkInId;
    private String username;
    private String content;
    private String time;
    public Comment() {}

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
}