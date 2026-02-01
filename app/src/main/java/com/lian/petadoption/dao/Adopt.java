package com.lian.petadoption.dao;

import android.text.TextUtils;
import com.lian.petadoption.config.AppConfig;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Adopt implements Serializable {
    private int id;
    private String sendName;
    private String petName; // 宠物名字
    private String breed; // 品种
    private String gender; // 性别
    private String age; // 年龄
    private String deworming; // 驱虫状态
    private String sterilization; // 绝育状态
    private String vaccine; // 疫苗状态
    private String cycle; // 打卡周期
    private String address; // 地址
    private String remark; // 备注
    private String state; // 状态：待领养/已领养
    private String pic; // 图片路径
    private String time; // 发布时间
    private String phone;

    // 获取图片列表
    public List<String> getImageList() {
        List<String> list = new ArrayList<>();
        if (TextUtils.isEmpty(pic)) {
            return list;
        }
        String splitRegex = "[#," + AppConfig.IMAGE_SPLIT_SYMBOL + "]";
        String[] paths = pic.split(splitRegex);

        for (String p : paths) {
            if (!TextUtils.isEmpty(p)) {
                list.add(p);
            }
        }
        return list;
    }

    // 获取封面图（第一张）
    public String getCoverImage() {
        List<String> imgs = getImageList();
        return imgs.isEmpty() ? "" : imgs.get(0);
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getSendName() { return sendName; }
    public void setSendName(String sendName) { this.sendName = sendName; }
    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }
    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getAge() { return age; }
    public void setAge(String age) { this.age = age; }
    public String getDeworming() { return deworming; }
    public void setDeworming(String deworming) { this.deworming = deworming; }
    public String getSterilization() { return sterilization; }
    public void setSterilization(String sterilization) { this.sterilization = sterilization; }
    public String getVaccine() { return vaccine; }
    public void setVaccine(String vaccine) { this.vaccine = vaccine; }
    public String getCycle() { return cycle; }
    public void setCycle(String cycle) { this.cycle = cycle; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPic() { return pic; }
    public void setPic(String pic) { this.pic = pic; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
}