package com.lian.petadoption.dao;

import android.text.TextUtils;
import com.lian.petadoption.config.AppConfig;
import java.io.Serializable;

public class ApplyInfo implements Serializable {
    // === 基础字段 ===
    private int id;
    private String petName;
    private String breed;
    private String pic;         // 宠物封面图
    private String state;       // 申请状态：待审核、已同意、已拒绝
    private String time;        // 申请时间

    // === 消息列表专用字段 ===
    private String name;            // 申请人账号 (m_name)
    private String publisherName;   // 发布者账号 (m_pname)
    private String applicantAvatar; // 申请人头像 (联表查询结果)
    private int readState;          // 阅读状态 (0:未读, 1:已读)

    // === 审核详情专用字段 (对应数据库 my_apply 表的详细列) ===
    private String realName;     // 真实姓名 (m_app_name)
    private String age;          // 年龄 (m_age)
    private String gender;       // 性别 (m_gender)
    private String phone;        // 联系电话 (m_pphone)
    private String idCard;       // 身份证号 (m_idcard)
    private String address;      // 详细地址 (m_address)
    private String incomeSource; // 经济来源 (m_income_source)
    private String intent;       // 领养意向 (m_intent)
    private String livePics;     // 居住环境照片 (m_live_pics)
    private String incomePics;   // 收入证明照片 (m_income_pics)

    public ApplyInfo() {}

    /**
     * 获取封面图 (处理多图情况，取第一张)
     */
    public String getCoverImage() {
        if (TextUtils.isEmpty(pic)) return "";
        // 兼容 # 和 , 作为分隔符
        if (pic.contains(AppConfig.IMAGE_SPLIT_SYMBOL) || pic.contains("#") || pic.contains(",")) {
            String[] split = pic.split("[#," + AppConfig.IMAGE_SPLIT_SYMBOL + "]");
            return split.length > 0 ? split[0] : "";
        }
        return pic;
    }

    // ================= Getter / Setter =================

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

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPublisherName() { return publisherName; }
    public void setPublisherName(String publisherName) { this.publisherName = publisherName; }

    public String getApplicantAvatar() { return applicantAvatar; }
    public void setApplicantAvatar(String applicantAvatar) { this.applicantAvatar = applicantAvatar; }

    public int getReadState() { return readState; }
    public void setReadState(int readState) { this.readState = readState; }

    // --- 详情字段 Getter/Setter ---

    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }

    public String getAge() { return age; }
    public void setAge(String age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getIncomeSource() { return incomeSource; }
    public void setIncomeSource(String incomeSource) { this.incomeSource = incomeSource; }

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public String getLivePics() { return livePics; }
    public void setLivePics(String livePics) { this.livePics = livePics; }

    public String getIncomePics() { return incomePics; }
    public void setIncomePics(String incomePics) { this.incomePics = incomePics; }
}