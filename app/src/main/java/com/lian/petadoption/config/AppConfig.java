package com.lian.petadoption.config;

/**
 * 全局常量配置类
 */
public final class AppConfig {
    private AppConfig(){}

    // 图片分隔符
    public static final String IMAGE_SPLIT_SYMBOL=",";
    // 日期格式
    public static final String DATE_FORMATE="yyyy-MM-dd HH:mm:ss";

    // SharedPreferences
    public static class SP{
        public static final String NAME="User_Preferences";

        // 存储字段
        public static final String USER_ACCOUNT="sp_account"; // 账号
        public static final String USER_ROLE="sp_role"; // 角色
        public static final String USER_NICKNAME="sp_nickname"; // 昵称
        public static final String USER_AVATAR="sp_avatar"; // 头像
        public static final String USER_BIO="sp_bio"; // 简介
    }

    // Intent 跳转传参
    public static class Extra{
        public static final String ADOPT_DATA="extra_adopt_obj";
        public static final String APPLY_ID="extra_apply_id";
        public static final String KNOWLEDGE_DATA="extra_knowledge";
        public static final String PUNCH_DATA="extra_punch";
    }

    // 状态
    public static class State{
        // 用户状态
        public static final String USER_NORMAL="1"; // 正常
        public static final String USER_BANNED="0"; // 禁用

        // 宠物状态
        public static final String PET_WAITING="待领养";
        public static final String PET_ADOPTED="已领养";
        public static final String PET_DISPLAY="展示中";

        // 申请状态
        public static final String APPLY_PENDING="待审核";
        public static final String APPLY_PASSED="已通过";
        public static final String APPLY_REJECTED="已拒绝";
    }

    // 用户角色
    public static class Role{
        public static final String ADMIN="admin";
        public static final String USER="user";
    }
}
