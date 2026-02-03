package com.lian.petadoption.dao;

import java.io.Serializable;

public class User implements Serializable {
    private int id;          // 对应数据库 _id
    private String username; // 对应 u_name (账号)
    private String password; // 对应 u_psd
    private String nickname; // 对应 u_nickname
    private String state;    // 对应 u_state (1=正常, 0=封禁)
    private String gender;   // 对应 u_gender
    private String head;     // 对应 u_head (头像路径)
    private String info;     // 对应 u_info (简介)
    private String time;     // 对应 u_time (注册时间)

    // 无参构造函数
    public User() {
    }

    // 全参构造函数 (可选，方便测试)
    public User(int id, String username, String password, String nickname, String state, String gender, String head, String info, String time) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.state = state;
        this.gender = gender;
        this.head = head;
        this.info = info;
        this.time = time;
    }

    // Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    // 重点：DatabaseHelper 中使用的是 setHead/getHead
    public String getHead() {
        return head;
    }

    public void setHead(String head) {
        this.head = head;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", nickname='" + nickname + '\'' +
                ", state='" + state + '\'' +
                '}';
    }
}