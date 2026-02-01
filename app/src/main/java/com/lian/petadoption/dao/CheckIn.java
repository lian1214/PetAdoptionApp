package com.lian.petadoption.dao;

import android.text.TextUtils;
import com.lian.petadoption.config.AppConfig;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CheckIn implements Serializable {
    private int id;
    private String username;
    private int petId;
    private String petName;
    private String content;
    private String pic;
    private String time;
    private int likeCount;

    public List<String> getImageList() {
        List<String> list = new ArrayList<>();
        if (TextUtils.isEmpty(pic)) return list;
        String[] paths = pic.split("[#," + AppConfig.IMAGE_SPLIT_SYMBOL + "]");
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
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public int getPetId() { return petId; }
    public void setPetId(int petId) { this.petId = petId; }
    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getPic() { return pic; }
    public void setPic(String pic) { this.pic = pic; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
}