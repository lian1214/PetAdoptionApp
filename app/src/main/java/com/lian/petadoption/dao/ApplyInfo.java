package com.lian.petadoption.dao;

import android.text.TextUtils;
import com.lian.petadoption.config.AppConfig;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ApplyInfo implements Serializable {
    private int id;
    private String petName;
    private String breed;
    private String pic;
    private String state;
    private String time;

    public ApplyInfo() {}

    public String getCoverImage() {
        if (TextUtils.isEmpty(pic)) return "";
        if (pic.contains(AppConfig.IMAGE_SPLIT_SYMBOL) || pic.contains("#") || pic.contains(",")) {
            String[] split = pic.split("[#," + AppConfig.IMAGE_SPLIT_SYMBOL + "]");
            return split.length > 0 ? split[0] : "";
        }
        return pic;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }
    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }
    public String getPic() { return pic; }
    public void setPic(String pic) { this.pic = pic; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
}