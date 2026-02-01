package com.lian.petadoption.dao;

import java.io.Serializable;

public class User implements Serializable {
    private int id;
    private String username;
    private String password;
    private String nickname;
    private String state;
    private String gender;
    private String avatar;
    private String info;
    private String time;

    public User() {}

    public User(String username, String nickname, String avatar, String info) {
        this.username = username;
        this.nickname = nickname;
        this.avatar = avatar;
        this.info = info;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getInfo() { return info; }
    public void setInfo(String info) { this.info = info; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
}