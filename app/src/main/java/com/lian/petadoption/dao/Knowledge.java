package com.lian.petadoption.dao;

import android.text.TextUtils;
import com.lian.petadoption.config.AppConfig;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Knowledge implements Serializable {
    private int id;
    private String type;
    private String username;
    private String title;
    private String content;
    private String time;
    private int isOfficial; // 1为官方 0为用户
    private String pics;

    public Knowledge() {}

    public List<String> getImageList() {
        List<String> list = new ArrayList<>();
        if (TextUtils.isEmpty(pics)) return list;
        String[] paths = pics.split("[#," + AppConfig.IMAGE_SPLIT_SYMBOL + "]");
        for (String p : paths) {
            if (!TextUtils.isEmpty(p)) list.add(p);
        }
        return list;
    }

    public String getCoverImage() {
        List<String> list = getImageList();
        return list.isEmpty() ? "" : list.get(0);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public int getIsOfficial() { return isOfficial; }
    public void setIsOfficial(int isOfficial) { this.isOfficial = isOfficial; }
    public String getPics() { return pics; }
    public void setPics(String pics) { this.pics = pics; }
}