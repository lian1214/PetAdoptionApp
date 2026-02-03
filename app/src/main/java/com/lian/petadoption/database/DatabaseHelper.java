package com.lian.petadoption.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.lian.petadoption.config.AppConfig;
import com.lian.petadoption.dao.Adopt;
import com.lian.petadoption.dao.ApplyInfo;
import com.lian.petadoption.dao.CheckIn;
import com.lian.petadoption.dao.Comment;
import com.lian.petadoption.dao.Knowledge;
import com.lian.petadoption.dao.User; // 确保你有 User 实体类，如果没有请创建或使用 Map
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseHelper extends SQLiteOpenHelper {
    // 基础配置与建表
    private static final String DB_NAME = "pet_adoption.db";
    private static final int DB_VERSION = 1;

    // 表名常量
    public static final String TABLE_USER = "user";
    public static final String TABLE_ADOPT = "adopt";
    public static final String TABLE_MY_APPLY = "my_apply";
    public static final String TABLE_CHECKIN = "check_in";
    public static final String TABLE_CHECKIN_LIKE = "check_in_like";
    public static final String TABLE_CHECKIN_COMMENT = "check_in_comment";
    public static final String TABLE_KNOWLEDGE = "knowledge";
    public static final String TABLE_KNOWLEDGE_COMMENT = "knowledge_comment";
    public static final String TABLE_KNOWLEDGE_LIKE = "knowledge_like";
    public static final String TABLE_FAVORITE = "favorite";
    public static final String COL_ID = "_id"; // 统一主键

    // 异步处理工具
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 用户表
        db.execSQL("CREATE TABLE " + TABLE_USER + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                "u_name TEXT UNIQUE, u_psd TEXT, u_nickname TEXT DEFAULT '未设置'," +
                "u_state TEXT, u_gender TEXT, u_head TEXT, u_info TEXT, u_time TEXT)");

        // 领养信息表
        db.execSQL("CREATE TABLE " + TABLE_ADOPT + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                "sendName TEXT, petName TEXT, breed TEXT, gender TEXT, age TEXT," +
                "deworming TEXT, sterilization TEXT, vaccine TEXT, cycle TEXT," +
                "address TEXT, remark TEXT, pic TEXT, state TEXT, phone TEXT, time TEXT)");

        // 申请表
        db.execSQL("CREATE TABLE " + TABLE_MY_APPLY + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "m_vid INTEGER, m_pname TEXT, m_name TEXT, m_app_name TEXT, " +
                "m_age TEXT, m_gender TEXT, m_idcard TEXT, m_pphone TEXT, m_address TEXT, " +
                "m_live_pics TEXT, m_income_pics TEXT, m_income_source TEXT, m_intent TEXT, " +
                "m_state TEXT, m_read_state INTEGER DEFAULT 0, m_is_deleted INTEGER DEFAULT 0, m_time TEXT)");

        // 打卡表
        db.execSQL("CREATE TABLE " + TABLE_CHECKIN + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                "pet_id INTEGER, username TEXT, pic TEXT, check_time TEXT," +
                "pet_name TEXT, content TEXT, like_count INTEGER DEFAULT 0)");

        // 打卡点赞与评论
        db.execSQL("CREATE TABLE " + TABLE_CHECKIN_LIKE + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, check_in_id INTEGER, username TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_CHECKIN_COMMENT + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, check_in_id INTEGER, username TEXT, content TEXT, time TEXT)");

        // 知识库
        db.execSQL("CREATE TABLE " + TABLE_KNOWLEDGE + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "type TEXT, username TEXT, title TEXT, content TEXT, time TEXT, is_official INTEGER, pics TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_KNOWLEDGE_COMMENT + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, knowledge_id INTEGER, username TEXT, content TEXT, time TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_KNOWLEDGE_LIKE + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, knowledge_id INTEGER, username TEXT)");

        // 收藏表
        db.execSQL("CREATE TABLE " + TABLE_FAVORITE + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, pet_id INTEGER)");

        // 初始化默认管理员
        initAdmin(db);
    }

    private void initAdmin(SQLiteDatabase db) {
        ContentValues cv = new ContentValues();
        cv.put("u_name", "admin");
        cv.put("u_psd", "admin");
        cv.put("u_state", AppConfig.State.USER_NORMAL);
        cv.put("u_nickname", "系统管理员");
        cv.put("u_time", getNowTime());
        db.insertWithOnConflict(TABLE_USER, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String[] tables = {TABLE_USER, TABLE_ADOPT, TABLE_MY_APPLY, TABLE_CHECKIN, TABLE_CHECKIN_LIKE,
                TABLE_CHECKIN_COMMENT, TABLE_KNOWLEDGE, TABLE_KNOWLEDGE_COMMENT,
                TABLE_KNOWLEDGE_LIKE, TABLE_FAVORITE};
        for (String table : tables) {
            db.execSQL("DROP TABLE IF EXISTS " + table);
        }
        onCreate(db);
    }

    private String getNowTime() {
        return new SimpleDateFormat(AppConfig.DATE_FORMATE, Locale.getDefault()).format(new Date());
    }

    private <T> void postSuccess(DataCallback<T> callback, T data) {
        mainHandler.post(() -> { if (callback != null) callback.onSuccess(data); });
    }

    private void postFail(DataCallback<?> callback, String msg) {
        mainHandler.post(() -> { if (callback != null) callback.onFail(msg); });
    }

    // 用户管理模块
    /**
     * 用户注册
     */
    public void register(String username, String password, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            if (isUserExistSync(username)) {
                postFail(callback, "用户名已存在");
                return;
            }
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("u_name", username);
            values.put("u_psd", password);
            values.put("u_state", AppConfig.State.USER_NORMAL);
            values.put("u_time", getNowTime());
            values.put("u_nickname", "新用户" + System.currentTimeMillis() % 1000);
            values.put("u_head", "");

            long result = db.insert(TABLE_USER, null, values);
            if (result != -1) postSuccess(callback, true);
            else postFail(callback, "注册失败");
        });
    }

    /**
     * 用户登录
     */
    public void login(String username, String password, DataCallback<Map<String, String>> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.query(TABLE_USER, null, "u_name=?", new String[]{username}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                @SuppressLint("Range") String dbPwd = cursor.getString(cursor.getColumnIndex("u_psd"));
                @SuppressLint("Range") String state = cursor.getString(cursor.getColumnIndex("u_state"));

                if (AppConfig.State.USER_BANNED.equals(state)) {
                    cursor.close();
                    postFail(callback, "账号已被封禁");
                    return;
                }

                if (password.equals(dbPwd)) {
                    Map<String, String> userInfo = new HashMap<>();
                    // 提取需要存入 SP 的信息
                    userInfo.put("u_nickname", getCursorString(cursor, "u_nickname"));
                    userInfo.put("u_head", getCursorString(cursor, "u_head"));
                    userInfo.put("u_info", getCursorString(cursor, "u_info"));
                    cursor.close();
                    postSuccess(callback, userInfo);
                } else {
                    cursor.close();
                    postFail(callback, "密码错误");
                }
            } else {
                if(cursor != null) cursor.close();
                postFail(callback, "用户不存在");
            }
        });
    }

    /**
     * 获取用户信息详情
     */
    @SuppressLint("Range")
    public Map<String, String> getUserInfo(String username) {
        Map<String, String> map = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USER, null, "u_name=?", new String[]{username}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            map.put("u_head", getCursorString(cursor, "u_head"));
            map.put("u_nickname", getCursorString(cursor, "u_nickname"));
            map.put("u_info", getCursorString(cursor, "u_info"));
            cursor.close();
            return map;
        }
        if (cursor != null) cursor.close();
        return null;
    }

    /**
     * 修改用户信息
     */
    public void updateUserInfo(String oldAccount, String newAccount, String newPassword, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            try {
                ContentValues values = new ContentValues();
                values.put("u_name", newAccount);
                if (newPassword != null && !newPassword.isEmpty()) {
                    values.put("u_psd", newPassword);
                }
                int rows = db.update(TABLE_USER, values, "u_name = ?", new String[]{oldAccount});

                if (rows > 0) {
                    ContentValues adoptValues = new ContentValues();
                    adoptValues.put("sendName", newAccount);
                    db.update(TABLE_ADOPT, adoptValues, "sendName = ?", new String[]{oldAccount});

                    db.setTransactionSuccessful();
                    postSuccess(callback, true);
                } else {
                    postFail(callback, "更新失败");
                }
            } catch (Exception e) {
                postFail(callback, e.getMessage());
            } finally {
                db.endTransaction();
            }
        });
    }

    /**
     * 更新用户信息 (支持修改账号、密码、昵称、简介)
     * @param oldAccount 旧账号 (用于查找)
     * @param newAccount 新账号 (若不修改传 null 或与旧账号相同)
     * @param newNick    新昵称 (若不修改传 null)
     * @param newBio     新简介 (若不修改传 null)
     * @param newPassword 新密码 (若不修改传 null)
     */
    public void updateUserProfile(String oldAccount, String newAccount, String newNick, String newBio, String newPassword, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction(); // 开启事务
            try {
                ContentValues values = new ContentValues();

                // 1. 处理账号变更 (如果传了且不一样)
                boolean isAccountChanged = newAccount != null && !newAccount.isEmpty() && !newAccount.equals(oldAccount);
                if (isAccountChanged) {
                    // 检查新账号是否已存在
                    if (isUserExistSync(newAccount)) {
                        postFail(callback, "新用户名已存在");
                        db.endTransaction();
                        return;
                    }
                    values.put("u_name", newAccount);
                }

                // 2. 处理其他字段
                if (newNick != null) values.put("u_nickname", newNick);
                if (newBio != null) values.put("u_info", newBio);
                if (newPassword != null && !newPassword.isEmpty()) {
                    values.put("u_psd", newPassword);
                }

                // 3. 执行 User 表更新
                int rows = db.update(TABLE_USER, values, "u_name = ?", new String[]{oldAccount});

                // 4. 如果账号变了，需要同步更新关联表 (比如发布的宠物 sendName)
                if (rows > 0 && isAccountChanged) {
                    ContentValues adoptValues = new ContentValues();
                    adoptValues.put("sendName", newAccount);
                    db.update(TABLE_ADOPT, adoptValues, "sendName = ?", new String[]{oldAccount});

                    // 同步更新申请表、打卡表等 (如果有用到 username 作为外键)
                    ContentValues applyValues = new ContentValues();
                    applyValues.put("m_name", newAccount);
                    db.update(TABLE_MY_APPLY, applyValues, "m_name = ?", new String[]{oldAccount});

                    applyValues.clear();
                    applyValues.put("m_pname", newAccount);
                    db.update(TABLE_MY_APPLY, applyValues, "m_pname = ?", new String[]{oldAccount});
                }

                db.setTransactionSuccessful(); // 标记成功
                postSuccess(callback, rows > 0);

            } catch (Exception e) {
                e.printStackTrace();
                postFail(callback, "更新失败: " + e.getMessage());
            } finally {
                db.endTransaction(); // 提交或回滚
            }
        });
    }

    // 内部同步检查用户是否存在
    private boolean isUserExistSync(String username) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_USER, new String[]{COL_ID}, "u_name=?", new String[]{username}, null, null, null);
        boolean exist = cursor.getCount() > 0;
        cursor.close();
        return exist;
    }

    // 安全获取 String
    @SuppressLint("Range")
    private String getCursorString(Cursor cursor, String colName) {
        int idx = cursor.getColumnIndex(colName);
        return idx != -1 ? cursor.getString(idx) : "";
    }

    // 宠物管理模块
    /**
     * 发布宠物
     */
    public void publishPet(Adopt adopt, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            // 提取数据
            cv.put("sendName", adopt.getSendName());
            cv.put("petName", adopt.getPetName());
            cv.put("breed", adopt.getBreed());
            cv.put("gender", adopt.getGender());
            cv.put("age", adopt.getAge());
            cv.put("pic", adopt.getPic());
            cv.put("address", adopt.getAddress());
            cv.put("phone", adopt.getPhone());
            cv.put("remark", adopt.getRemark());
            // 医疗信息
            cv.put("deworming", adopt.getDeworming());
            cv.put("sterilization", adopt.getSterilization());
            cv.put("vaccine", adopt.getVaccine());
            cv.put("cycle", adopt.getCycle());
            // 状态与时间
            cv.put("state", AppConfig.State.PET_WAITING);
            cv.put("time", getNowTime());

            long ret = db.insert(TABLE_ADOPT, null, cv);
            if (ret != -1) postSuccess(callback, true);
            else postFail(callback, "发布失败");
        });
    }

    /**
     * 获取所有待领养宠物
     */
    public void getAllPets(DataCallback<List<Adopt>> callback) {
        executor.execute(() -> {
            List<Adopt> list = new ArrayList<>();
            SQLiteDatabase db = getReadableDatabase();
            // 排除已领养的
            String sql = "SELECT * FROM " + TABLE_ADOPT + " WHERE state != ? ORDER BY " + COL_ID + " DESC";
            Cursor cursor = db.rawQuery(sql, new String[]{AppConfig.State.PET_ADOPTED});

            while (cursor.moveToNext()) {
                list.add(cursorToAdopt(cursor));
            }
            cursor.close();
            postSuccess(callback, list);
        });
    }

    /**
     * 搜索宠物
     */
    public void searchPets(String keyword, DataCallback<List<Adopt>> callback) {
        executor.execute(() -> {
            List<Adopt> list = new ArrayList<>();
            SQLiteDatabase db = getReadableDatabase();
            String sql = "SELECT * FROM " + TABLE_ADOPT +
                    " WHERE (petName LIKE ? OR breed LIKE ?) AND state = ? ORDER BY " + COL_ID + " DESC";
            String[] args = new String[]{"%" + keyword + "%", "%" + keyword + "%", AppConfig.State.PET_WAITING};

            Cursor cursor = db.rawQuery(sql, args);
            while (cursor.moveToNext()) {
                list.add(cursorToAdopt(cursor));
            }
            cursor.close();
            postSuccess(callback, list);
        });
    }

    /**
     * 获取所有待领养宠物 (异步)
     * 替代了原有的同步 getPendingAdopts 方法
     */
    public void getPendingAdopts(DataCallback<List<Adopt>> callback) {
        executor.execute(() -> {
            List<Adopt> list = new ArrayList<>();
            SQLiteDatabase db = getReadableDatabase();

            // 逻辑说明：
            // 查询所有状态 不是 "已领养" 的宠物。
            // 这样 "待领养"、"展示中" 等状态都会被查出来。
            // ORDER BY _id DESC 保证最新发布的显示在最前面。
            String sql = "SELECT * FROM " + TABLE_ADOPT +
                    " WHERE state != ? ORDER BY " + COL_ID + " DESC";

            // AppConfig.State.PET_ADOPTED 通常定义为 "已领养"
            Cursor cursor = db.rawQuery(sql, new String[]{AppConfig.State.PET_ADOPTED});

            while (cursor.moveToNext()) {
                // 复用 cursorToAdopt 方法解析数据
                list.add(cursorToAdopt(cursor));
            }
            cursor.close();

            // 回调主线程
            postSuccess(callback, list);
        });
    }

    /**
     * 根据 ID 获取单只宠物的详细领养信息 (异步)
     * @param id 宠物在数据库中的 _id
     */
    public void getAdoptById(int id, DataCallback<Adopt> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = getReadableDatabase();
            Adopt adopt = null;
            Cursor cursor = null;
            try {
                String sql = "SELECT * FROM " + TABLE_ADOPT + " WHERE " + COL_ID + " = ?";
                cursor = db.rawQuery(sql, new String[]{String.valueOf(id)});

                if (cursor != null && cursor.moveToFirst()) {
                    // 复用现有的 cursorToAdopt 方法（注意：需要确保 cursorToAdopt 读取了所有详情字段）
                    // 或者在这里单独写全量读取逻辑
                    adopt = new Adopt();
                    adopt.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)));
                    adopt.setSendName(cursor.getString(cursor.getColumnIndexOrThrow("sendName")));
                    adopt.setPetName(cursor.getString(cursor.getColumnIndexOrThrow("petName")));
                    adopt.setBreed(cursor.getString(cursor.getColumnIndexOrThrow("breed")));
                    adopt.setGender(cursor.getString(cursor.getColumnIndexOrThrow("gender")));
                    adopt.setAge(cursor.getString(cursor.getColumnIndexOrThrow("age")));

                    // 详情字段
                    adopt.setDeworming(getCursorString(cursor, "deworming"));
                    adopt.setSterilization(getCursorString(cursor, "sterilization"));
                    adopt.setVaccine(getCursorString(cursor, "vaccine"));
                    adopt.setCycle(getCursorString(cursor, "cycle"));
                    adopt.setAddress(getCursorString(cursor, "address"));
                    adopt.setRemark(getCursorString(cursor, "remark"));
                    adopt.setPic(getCursorString(cursor, "pic"));
                    adopt.setState(getCursorString(cursor, "state"));
                    adopt.setPhone(getCursorString(cursor, "phone"));
                    adopt.setTime(getCursorString(cursor, "time"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) cursor.close();
            }

            // 回调
            if (adopt != null) {
                postSuccess(callback, adopt);
            } else {
                postFail(callback, "未找到该宠物信息");
            }
        });
    }

    /**
     * 根据申请ID获取申请详情 (用于 ApproveDetailActivity)
     */
    public void getApplyDetailById(int applyId, DataCallback<ApplyInfo> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = getReadableDatabase();
            ApplyInfo info = null;
            // 不需要联表，只查申请详情
            Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_MY_APPLY + " WHERE " + COL_ID + " = ?", new String[]{String.valueOf(applyId)});

            if (cursor != null && cursor.moveToFirst()) {
                info = new ApplyInfo();
                info.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)));
                // 填充所有审核需要的字段 (真实姓名、年龄、图片等)
                // 这里为了方便，可以把 ApplyInfo 类扩展一下，或者用 Map，或者直接在 Adapter/Activity 里用 cursorToApplyInfo 解析基础字段
                // 关键字段：
                info.setName(getCursorString(cursor, "m_name"));       // 申请人账号
                info.setRealName(getCursorString(cursor, "m_app_name")); // 真实姓名
                info.setAge(getCursorString(cursor, "m_age"));
                info.setGender(getCursorString(cursor, "m_gender"));
                info.setPhone(getCursorString(cursor, "m_pphone"));
                info.setIdCard(getCursorString(cursor, "m_idcard"));
                info.setAddress(getCursorString(cursor, "m_address"));
                info.setIncomeSource(getCursorString(cursor, "m_income_source"));
                info.setIntent(getCursorString(cursor, "m_intent"));
                info.setLivePics(getCursorString(cursor, "m_live_pics"));
                info.setIncomePics(getCursorString(cursor, "m_income_pics"));
                info.setState(getCursorString(cursor, "m_state"));
            }
            if (cursor != null) cursor.close();

            if (info != null) postSuccess(callback, info);
            else postFail(callback, "未找到申请记录");
        });
    }

    /**
     * 获取“我”发布的宠物
     */
    public void getMyPublishList(String username, DataCallback<List<Adopt>> callback) {
        executor.execute(() -> {
            List<Adopt> list = new ArrayList<>();
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.query(TABLE_ADOPT, null, "sendName=?", new String[]{username}, null, null, COL_ID + " DESC");
            while (cursor.moveToNext()) {
                list.add(cursorToAdopt(cursor));
            }
            cursor.close();
            postSuccess(callback, list);
        });
    }

    /**
     * 删除宠物
     */
    public void deletePet(int petId, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();
            int rows = db.delete(TABLE_ADOPT, COL_ID + "=?", new String[]{String.valueOf(petId)});
            if (rows > 0) postSuccess(callback, true);
            else postFail(callback, "删除失败");
        });
    }

    // 申请与审核模块
    /**
     * 提交领养申请
     */
    public void submitApplication(int vid, String pName, String applicantName, String realName,
                                  String age, String gender, String idCard, String phone,
                                  String address, String livePics, String incomePics,
                                  String incomeSource, String intent, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("m_vid", vid);
            cv.put("m_pname", pName);       // 发布者
            cv.put("m_name", applicantName);// 申请人
            cv.put("m_app_name", realName);
            cv.put("m_age", age);
            cv.put("m_gender", gender);
            cv.put("m_idcard", idCard);
            cv.put("m_pphone", phone);
            cv.put("m_address", address);
            cv.put("m_live_pics", livePics);
            cv.put("m_income_pics", incomePics);
            cv.put("m_income_source", incomeSource);
            cv.put("m_intent", intent);
            cv.put("m_state", AppConfig.State.APPLY_PENDING);
            cv.put("m_time", getNowTime());

            long ret = db.insert(TABLE_MY_APPLY, null, cv);
            if (ret != -1) postSuccess(callback, true);
            else postFail(callback, "申请提交失败");
        });
    }

    /**
     * 获取我的申请记录
     */
    public void getMyApplyList(String username, DataCallback<List<ApplyInfo>> callback) {
        executor.execute(() -> {
            List<ApplyInfo> list = new ArrayList<>();
            SQLiteDatabase db = getReadableDatabase();
            String sql = "SELECT a._id, a.m_state, a.m_time, p.petName, p.pic " +
                    "FROM " + TABLE_MY_APPLY + " a " +
                    "INNER JOIN " + TABLE_ADOPT + " p ON a.m_vid = p._id " +
                    "WHERE a.m_name = ? " +
                    "ORDER BY a._id DESC";

            Cursor cursor = db.rawQuery(sql, new String[]{username});
            while (cursor.moveToNext()) {
                ApplyInfo info = new ApplyInfo();
                info.setId(cursor.getInt(0));
                info.setState(cursor.getString(1));
                info.setTime(cursor.getString(2));
                info.setPetName(cursor.getString(3));
                info.setPic(cursor.getString(4));
                list.add(info);
            }
            cursor.close();
            postSuccess(callback, list);
        });
    }

    /**
     * 审核申请
     */
    public void auditApplication(int applyId, boolean isPass, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction(); // 开启事务
            try {
                // 更新申请单状态
                String applyStatus = isPass ? AppConfig.State.APPLY_PASSED : AppConfig.State.APPLY_REJECTED;
                ContentValues applyCv = new ContentValues();
                applyCv.put("m_state", applyStatus);
                applyCv.put("m_read_state", 0); // 重置为未读 通知申请人
                db.update(TABLE_MY_APPLY, applyCv, COL_ID + "=?", new String[]{String.valueOf(applyId)});

                // 如果通过 把宠物锁定为“已领养”
                if (isPass) {
                    // 查宠物 ID
                    Cursor c = db.rawQuery("SELECT m_vid FROM " + TABLE_MY_APPLY + " WHERE _id=?", new String[]{String.valueOf(applyId)});
                    if (c.moveToFirst()) {
                        int petId = c.getInt(0);
                        ContentValues petCv = new ContentValues();
                        petCv.put("state", AppConfig.State.PET_ADOPTED);
                        db.update(TABLE_ADOPT, petCv, COL_ID + "=?", new String[]{String.valueOf(petId)});
                    }
                    c.close();
                }

                db.setTransactionSuccessful(); // 标记成功
                postSuccess(callback, true);
            } catch (Exception e) {
                postFail(callback, "审核异常: " + e.getMessage());
            } finally {
                db.endTransaction(); // 提交
            }
        });
    }

    // 知识库模块
    /**
     * 首页知识推荐（随机获取）
     */
    // 1. 修改 getKnowledgeList 方法
    public void getKnowledgeList(String baseType, DataCallback<List<Knowledge>> callback) {
        executor.execute(() -> {
            List<Knowledge> list = new ArrayList<>();
            SQLiteDatabase db = getReadableDatabase();

            // 【修改点】把 = 改为 LIKE，支持匹配 "pet:xxx"
            String sql = "SELECT * FROM " + TABLE_KNOWLEDGE +
                    " WHERE type = ? OR type LIKE ? " +
                    " ORDER BY is_official DESC, _id DESC";

            // 参数：匹配 "pet" 或者 "pet:%"
            Cursor cursor = db.rawQuery(sql, new String[]{baseType, baseType + ":%"});

            while (cursor.moveToNext()) {
                list.add(cursorToKnowledge(cursor));
            }
            cursor.close();
            postSuccess(callback, list);
        });
    }

    // 2. 修改 searchKnowledge 方法
    public void searchKnowledge(String baseType, String keyword, DataCallback<List<Knowledge>> callback) {
        executor.execute(() -> {
            List<Knowledge> list = new ArrayList<>();
            SQLiteDatabase db = getReadableDatabase();

            // 【修改点】把 type 匹配改为 LIKE
            String sql = "SELECT * FROM " + TABLE_KNOWLEDGE +
                    " WHERE type LIKE ? AND (title LIKE ? OR content LIKE ? OR type LIKE ?) " +
                    " ORDER BY is_official DESC, _id DESC";

            String[] args = new String[]{
                    baseType + "%", // 匹配 pet%
                    "%" + keyword + "%",
                    "%" + keyword + "%",
                    "%" + keyword + "%"
            };

            Cursor cursor = db.rawQuery(sql, args);
            while (cursor.moveToNext()) {
                list.add(cursorToKnowledge(cursor));
            }
            cursor.close();
            postSuccess(callback, list);
        });
    }

    public void addKnowledge(String type, String username, String title, String content, String pic, boolean isOfficial, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("type", type);
            cv.put("username", username);
            cv.put("title", title);
            cv.put("content", content);
            cv.put("pics", pic);
            cv.put("is_official", isOfficial ? 1 : 0);
            cv.put("time", getNowTime());
            long ret = db.insert(TABLE_KNOWLEDGE, null, cv);
            if (ret != -1 && callback != null) postSuccess(callback, true);
            else if (callback != null) postFail(callback, "发布失败");
        });
    }

    public void getRandomMixedKnowledge(int count, DataCallback<List<Knowledge>> callback) {
        executor.execute(() -> {
            List<Knowledge> list = new ArrayList<>();
            SQLiteDatabase db = getReadableDatabase();
            String sql = "SELECT * FROM " + TABLE_KNOWLEDGE + " ORDER BY RANDOM() LIMIT " + count;
            Cursor cursor = db.rawQuery(sql, null);
            while (cursor.moveToNext()) list.add(cursorToKnowledge(cursor));
            cursor.close();
            postSuccess(callback, list);
        });
    }

    public int getKnowledgeLikeCount(int kId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_KNOWLEDGE_LIKE + " WHERE knowledge_id = ?", new String[]{String.valueOf(kId)});
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    public boolean isKnowledgeLiked(int kId, String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_KNOWLEDGE_LIKE + " WHERE knowledge_id = ? AND username = ?",
                new String[]{String.valueOf(kId), username});
        boolean liked = cursor.getCount() > 0;
        cursor.close();
        return liked;
    }

    public void toggleKnowledgeLike(int kId, String username, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();
            String where = "knowledge_id=? AND username=?";
            String[] args = new String[]{String.valueOf(kId), username};
            Cursor c = db.query(TABLE_KNOWLEDGE_LIKE, null, where, args, null, null, null);
            boolean isLiked;
            if (c.moveToFirst()) {
                db.delete(TABLE_KNOWLEDGE_LIKE, where, args);
                isLiked = false;
            } else {
                ContentValues cv = new ContentValues();
                cv.put("knowledge_id", kId);
                cv.put("username", username);
                db.insert(TABLE_KNOWLEDGE_LIKE, null, cv);
                isLiked = true;
            }
            c.close();
            postSuccess(callback, isLiked);
        });
    }

    public void addKnowledgeComment(int kId, String username, String content, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("knowledge_id", kId);
            cv.put("username", username);
            cv.put("content", content);
            cv.put("time", getNowTime());
            long ret = db.insert(TABLE_KNOWLEDGE_COMMENT, null, cv);
            if (ret != -1) postSuccess(callback, true);
            else postFail(callback, "评论失败");
        });
    }

    public void getKnowledgeComments(int kId, DataCallback<List<Comment>> callback) {
        executor.execute(() -> {
            List<Comment> list = new ArrayList<>();
            SQLiteDatabase db = getReadableDatabase();
            // 联表查询：获取评论者头像和昵称 (与打卡评论逻辑一致)
            String sql = "SELECT c.*, u.u_nickname, u.u_head FROM " + TABLE_KNOWLEDGE_COMMENT + " c " +
                    "LEFT JOIN " + TABLE_USER + " u ON c.username = u.u_name " +
                    "WHERE c.knowledge_id = ? ORDER BY c._id DESC";
            Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(kId)});
            while (cursor.moveToNext()) {
                Comment c = new Comment();
                c.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)));
                c.setUsername(getCursorString(cursor, "username"));
                c.setContent(getCursorString(cursor, "content"));
                c.setTime(getCursorString(cursor, "time"));
                String nick = getCursorString(cursor, "u_nickname");
                c.setNickname(TextUtils.isEmpty(nick) ? c.getUsername() : nick);
                c.setAvatar(getCursorString(cursor, "u_head"));
                list.add(c);
            }
            cursor.close();
            postSuccess(callback, list);
        });
    }

    /**
     * 批量删除知识库文章 (级联删除评论和点赞)
     * @param ids 要删除的知识库 ID 列表
     */
    public void deleteKnowledgeList(List<Integer> ids, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction(); // 开启事务
            try {
                for (int id : ids) {
                    String idStr = String.valueOf(id);
                    // 1. 删除关联的评论
                    db.delete(TABLE_KNOWLEDGE_COMMENT, "knowledge_id=?", new String[]{idStr});
                    // 2. 删除关联的点赞
                    db.delete(TABLE_KNOWLEDGE_LIKE, "knowledge_id=?", new String[]{idStr});
                    // 3. 删除文章本体
                    db.delete(TABLE_KNOWLEDGE, COL_ID + "=?", new String[]{idStr});
                }
                db.setTransactionSuccessful(); // 标记事务成功
                postSuccess(callback, true);
            } catch (Exception e) {
                e.printStackTrace();
                postFail(callback, "删除失败: " + e.getMessage());
            } finally {
                db.endTransaction(); // 提交或回滚
            }
        });
    }

    /**
     * 【新增】管理员后台：获取所有知识（不分类型，不分官方/用户）
     */
    public void getAllKnowledge(DataCallback<List<Knowledge>> callback) {
        executor.execute(() -> {
            List<Knowledge> list = new ArrayList<>();
            SQLiteDatabase db = getReadableDatabase();
            // 查询所有数据，按时间倒序排列 (最新的在前)
            String sql = "SELECT * FROM " + TABLE_KNOWLEDGE + " ORDER BY time DESC";

            Cursor cursor = db.rawQuery(sql, null);
            while (cursor != null && cursor.moveToNext()) {
                list.add(cursorToKnowledge(cursor));
            }
            if (cursor != null) cursor.close();

            postSuccess(callback, list);
        });
    }

    // 消息通知模块
    /**
     * 获取用户所有消息 (申请人视角 + 发布者视角)
     */
    public void getMyAllMessages(String username, DataCallback<List<ApplyInfo>> callback) {
        executor.execute(() -> {
            List<ApplyInfo> list = new ArrayList<>();
            SQLiteDatabase db = getReadableDatabase();

            // 复杂查询：
            // 1. 我是发布者 (m_pname = me) -> 收到申请
            // 2. 我是申请人 (m_name = me) 且 状态不是"待审核" -> 收到审批结果
            // 排除已删除的 (m_is_deleted = 0)
            // 按未读优先，然后按时间倒序
            String sql = "SELECT a.*, p.petName, u.u_head as applicant_avatar " +
                    "FROM " + TABLE_MY_APPLY + " a " +
                    "LEFT JOIN " + TABLE_ADOPT + " p ON a.m_vid = p._id " +
                    "LEFT JOIN " + TABLE_USER + " u ON a.m_name = u.u_name " +
                    "WHERE (a.m_pname = ? OR (a.m_name = ? AND a.m_state != ?)) " +
                    "AND a.m_is_deleted = 0 " +
                    "ORDER BY a.m_read_state ASC, a._id DESC";

            Cursor cursor = db.rawQuery(sql, new String[]{username, username, AppConfig.State.APPLY_PENDING});

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    ApplyInfo info = new ApplyInfo();
                    // 基础字段
                    info.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)));
                    info.setPetName(getCursorString(cursor, "petName"));
                    info.setState(getCursorString(cursor, "m_state"));
                    info.setTime(getCursorString(cursor, "m_time"));

                    // 关系字段
                    info.setName(getCursorString(cursor, "m_name")); // 申请人
                    info.setPublisherName(getCursorString(cursor, "m_pname")); // 发布者
                    info.setApplicantAvatar(getCursorString(cursor, "applicant_avatar")); // 申请人头像
                    info.setReadState(cursor.getInt(cursor.getColumnIndexOrThrow("m_read_state")));

                    list.add(info);
                }
                cursor.close();
            }
            postSuccess(callback, list);
        });
    }

    /**
     * 获取未读消息数 (用于 TabBar 红点)
     */
    public void getUnreadMessageCount(String username, DataCallback<Integer> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = getReadableDatabase();
            // 逻辑同上，只统计 count
            String sql = "SELECT COUNT(*) FROM " + TABLE_MY_APPLY + " WHERE m_is_deleted = 0 AND m_read_state = 0 AND (" +
                    "(m_pname = ? AND m_state = ?) OR " + // 发布者视角：收到待审核的申请
                    "(m_name = ? AND m_state != ?))";     // 申请人视角：收到已处理的结果

            Cursor cursor = db.rawQuery(sql, new String[]{username, AppConfig.State.APPLY_PENDING, username, AppConfig.State.APPLY_PENDING});
            int count = 0;
            if (cursor.moveToFirst()) count = cursor.getInt(0);
            cursor.close();
            postSuccess(callback, count);
        });
    }

    /**
     * 标记消息为已读
     */
    public void markAsRead(int id) {
        executor.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("m_read_state", 1);
            db.update(TABLE_MY_APPLY, cv, COL_ID + " = ?", new String[]{String.valueOf(id)});
            // 不需要回调
        });
    }

    /**
     * 清空所有已读消息 (逻辑删除)
     */
    public void deleteAllReadMessages() {
        executor.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("m_is_deleted", 1);
            db.update(TABLE_MY_APPLY, cv, "m_read_state = 1", null);
        });
    }

    // 社交模块
    /**
     * 发布打卡
     */
    public void publishCheckIn(String username, int petId, String petName, String content, String pic, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("username", username);
            cv.put("pet_id", petId);
            cv.put("pet_name", petName);
            cv.put("content", content);
            cv.put("pic", pic);
            cv.put("check_time", getNowTime());
            cv.put("like_count", 0);

            long ret = db.insert(TABLE_CHECKIN, null, cv);
            if (ret != -1) postSuccess(callback, true);
            else postFail(callback, "打卡失败");
        });
    }

    /**
     * 获取所有打卡动态
     */
    public void getAllCheckIns(DataCallback<List<CheckIn>> callback) {
        executor.execute(() -> {
            List<CheckIn> list = new ArrayList<>();
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.query(TABLE_CHECKIN, null, null, null, null, null, COL_ID + " DESC");
            while (cursor.moveToNext()) {
                list.add(cursorToCheckIn(cursor));
            }
            cursor.close();
            postSuccess(callback, list);
        });
    }

    /**
     * 获取我的打卡记录
     */
    public void getMyCheckInList(String username, DataCallback<List<CheckIn>> callback) {
        executor.execute(() -> {
            List<CheckIn> list = new ArrayList<>();
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.query(TABLE_CHECKIN, null, "username=?", new String[]{username}, null, null, COL_ID + " DESC");
            while (cursor.moveToNext()) {
                list.add(cursorToCheckIn(cursor));
            }
            cursor.close();
            postSuccess(callback, list);
        });
    }

    /**
     * 点赞/取消点赞
     * true=已赞, false=未赞
     */
    public void toggleCheckInLike(int checkInId, String username, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();
            String where = "check_in_id=? AND username=?";
            String[] args = new String[]{String.valueOf(checkInId), username};

            Cursor c = db.query(TABLE_CHECKIN_LIKE, null, where, args, null, null, null);
            boolean isLiked;
            if (c.moveToFirst()) {
                // 已赞 -> 取消
                db.delete(TABLE_CHECKIN_LIKE, where, args);
                // 减少计数
                db.execSQL("UPDATE " + TABLE_CHECKIN + " SET like_count = like_count - 1 WHERE " + COL_ID + "=" + checkInId);
                isLiked = false;
            } else {
                // 未赞 -> 添加
                ContentValues cv = new ContentValues();
                cv.put("check_in_id", checkInId);
                cv.put("username", username);
                db.insert(TABLE_CHECKIN_LIKE, null, cv);
                db.execSQL("UPDATE " + TABLE_CHECKIN + " SET like_count = like_count + 1 WHERE " + COL_ID + "=" + checkInId);
                isLiked = true;
            }
            c.close();
            postSuccess(callback, isLiked);
        });
    }

    /**
     * [同步] 获取某条打卡动态的点赞数
     */
    public int getCheckInLikeCount(int checkInId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT like_count FROM " + TABLE_CHECKIN + " WHERE " + COL_ID + "=?",
                new String[]{String.valueOf(checkInId)});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    /**
     * [同步] 判断用户是否给某条动态点过赞
     */
    public boolean isCheckInLiked(int checkInId, String username) {
        if (username == null || username.isEmpty()) return false;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_CHECKIN_LIKE, null, "check_in_id=? AND username=?",
                new String[]{String.valueOf(checkInId), username}, null, null, null);
        boolean liked = cursor.getCount() > 0;
        cursor.close();
        return liked;
    }

    /**
     * 切换收藏状态 (如果已收藏则取消，未收藏则添加)
     */
    public void toggleFavorite(String username, int petId, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();
            // 1. 查询是否存在
            Cursor c = db.query(TABLE_FAVORITE, null, "username=? AND pet_id=?",
                    new String[]{username, String.valueOf(petId)}, null, null, null);
            boolean exists = c.moveToFirst();
            c.close();

            boolean isNowFav;
            if (exists) {
                // 2. 存在 -> 删除
                db.delete(TABLE_FAVORITE, "username=? AND pet_id=?", new String[]{username, String.valueOf(petId)});
                isNowFav = false;
            } else {
                // 3. 不存在 -> 添加
                ContentValues cv = new ContentValues();
                cv.put("username", username);
                cv.put("pet_id", petId);
                db.insert(TABLE_FAVORITE, null, cv);
                isNowFav = true;
            }
            // 返回当前的最新状态
            postSuccess(callback, isNowFav);
        });
    }

    /**
     * [同步] 判断是否已收藏
     */
    public boolean isFavorite(String username, int petId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_FAVORITE, null, "username=? AND pet_id=?",
                new String[]{username, String.valueOf(petId)}, null, null, null);
        boolean isFav = cursor.getCount() > 0;
        cursor.close();
        return isFav;
    }

    /**
     * 获取我的收藏列表 (联表查询)
     */
    public void getMyFavoriteList(String username, DataCallback<List<Adopt>> callback) {
        executor.execute(() -> {
            List<Adopt> list = new ArrayList<>();
            SQLiteDatabase db = getReadableDatabase();
            String sql = "SELECT a.* FROM " + TABLE_ADOPT + " a " +
                    "INNER JOIN " + TABLE_FAVORITE + " f ON a._id = f.pet_id " +
                    "WHERE f.username = ? ORDER BY f._id DESC";
            Cursor cursor = db.rawQuery(sql, new String[]{username});
            while (cursor.moveToNext()) {
                list.add(cursorToAdopt(cursor));
            }
            cursor.close();
            postSuccess(callback, list);
        });
    }

    /**
     * 取消收藏
     */
    public void deleteFavorite(String username, int petId, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();
            // 删除条件：用户名 AND 宠物ID
            int rows = db.delete(TABLE_FAVORITE, "username = ? AND pet_id = ?",
                    new String[]{username, String.valueOf(petId)});

            if (rows > 0) {
                postSuccess(callback, true);
            } else {
                postFail(callback, "取消失败或未找到记录");
            }
        });
    }

    // 评论模块

    /**
     * 添加打卡评论 (异步)
     */
    public void addCheckInComment(String username, int punchId, String content, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("check_in_id", punchId);
            cv.put("username", username);
            cv.put("content", content);
            cv.put("time", getNowTime());

            long ret = db.insert(TABLE_CHECKIN_COMMENT, null, cv);
            if (ret != -1) {
                postSuccess(callback, true);
            } else {
                postFail(callback, "评论失败");
            }
        });
    }

    /**
     * 获取打卡评论列表 (异步 + 联表查询用户信息)
     * 解决了在 Adapter 中循环查库导致的卡顿问题
     */
    public void getCheckInComments(int punchId, DataCallback<List<Comment>> callback) {
        executor.execute(() -> {
            List<Comment> list = new ArrayList<>();
            SQLiteDatabase db = getReadableDatabase();

            // 联表查询：从 comment 表关联 user 表，获取头像(u_head)和昵称(u_nickname)
            // c.* 代表评论表所有字段
            String sql = "SELECT c.*, u.u_nickname, u.u_head " +
                    "FROM " + TABLE_CHECKIN_COMMENT + " c " +
                    "LEFT JOIN " + TABLE_USER + " u ON c.username = u.u_name " +
                    "WHERE c.check_in_id = ? " +
                    "ORDER BY c._id DESC";

            Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(punchId)});

            while (cursor.moveToNext()) {
                Comment c = new Comment();
                // 基础字段
                c.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)));
                c.setUsername(getCursorString(cursor, "username"));
                c.setContent(getCursorString(cursor, "content"));
                c.setTime(getCursorString(cursor, "time"));

                // 扩展字段 (来自 User 表)
                String nick = getCursorString(cursor, "u_nickname");
                c.setNickname(TextUtils.isEmpty(nick) ? c.getUsername() : nick); // 如果没昵称显示账号
                c.setAvatar(getCursorString(cursor, "u_head"));

                list.add(c);
            }
            cursor.close();
            postSuccess(callback, list);
        });
    }

    // 管理员模块
    /**
     * 获取所有普通用户
     */
    public void getAllUsers(DataCallback<List<User>> callback) {
        executor.execute(() -> {
            List<User> list = new ArrayList<>();
            SQLiteDatabase db = getReadableDatabase();
            // 排除管理员
            Cursor cursor = db.query(TABLE_USER, null, "u_name != 'admin'", null, null, null, COL_ID + " DESC");
            while (cursor.moveToNext()) {
                list.add(cursorToUser(cursor));
            }
            cursor.close();
            postSuccess(callback, list);
        });
    }

    /**
     * 搜索用户
     */
    public void searchUsers(String keyword, DataCallback<List<User>> callback) {
        executor.execute(() -> {
            List<User> list = new ArrayList<>();
            SQLiteDatabase db = getReadableDatabase();
            String sql = "SELECT * FROM " + TABLE_USER + " WHERE u_name LIKE ? AND u_name != 'admin'";
            Cursor cursor = db.rawQuery(sql, new String[]{"%" + keyword + "%"});
            while (cursor.moveToNext()) {
                list.add(cursorToUser(cursor));
            }
            cursor.close();
            postSuccess(callback, list);
        });
    }

    /**
     * 删除用户 (级联删除相关数据)
     */
    public void deleteUser(int userId) {
        executor.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();
            db.delete(TABLE_USER, COL_ID + "=?", new String[]{String.valueOf(userId)});
            // 可以在这里补充删除该用户发布的宠物、打卡等逻辑
        });
    }

    /**
     * 更新用户状态 (封禁/解封)
     */
    public void updateUserStatus(int userId, String newState) {
        executor.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("u_state", newState);
            db.update(TABLE_USER, cv, COL_ID + "=?", new String[]{String.valueOf(userId)});
        });
    }


    // 统计与图表模块

    public long getUserCount() {
        // 调用下面的 3参数 getCount
        return getCount(TABLE_USER, "u_name != 'admin'", null);
    }

    public long getPetCount() {
        return getCount(TABLE_ADOPT, "state != ?", new String[]{AppConfig.State.PET_ADOPTED});
    }

    public long getTotalApplicationCount() {
        return getCount(TABLE_MY_APPLY, null, null);
    }

    public long getSuccessfulAdoptionCount() {
        return getCount(TABLE_MY_APPLY, "m_state = ?", new String[]{AppConfig.State.APPLY_PASSED});
    }

    /**
     * 【新增】公共重载方法：自动获取数据库连接
     * 供 getUserCount 等单次查询使用
     */
    public long getCount(String table, String where, String[] args) {
        SQLiteDatabase db = getReadableDatabase();
        // 复用底层的 4参数 核心逻辑
        return getCount(db, table, where, args);
    }

    /**
     * 【核心】私有方法：需传入 db 对象
     * 供 getLast7DaysStats 循环中使用，避免反复开关数据库
     */
    private long getCount(SQLiteDatabase db, String table, String where, String[] args) {
        Cursor c = db.query(table, new String[]{"COUNT(*)"}, where, args, null, null, null);
        long count = 0;
        if (c != null) {
            if (c.moveToFirst()) {
                count = c.getLong(0);
            }
            c.close();
        }
        return count;
    }

    /**
     * 获取过去7天数据
     */
    public List<Map<String, Object>> getLast7DaysStats() {
        List<Map<String, Object>> result = new ArrayList<>();
        // 获取一次连接，循环复用
        SQLiteDatabase db = getReadableDatabase();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (int i = 6; i >= 0; i--) {
            long timeInMillis = System.currentTimeMillis() - i * 24 * 3600 * 1000L;
            String dateStr = sdf.format(new Date(timeInMillis));
            String likeDate = dateStr + "%"; // SQL 模糊匹配

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", dateStr.substring(5)); // 只取 MM-dd

            // 使用 4参数 getCount，传入同一个 db 对象，性能更高
            dayData.put("user", getCount(db, TABLE_USER, "u_time LIKE ?", new String[]{likeDate}));
            dayData.put("pet", getCount(db, TABLE_ADOPT, "time LIKE ?", new String[]{likeDate}));
            dayData.put("apply", getCount(db, TABLE_MY_APPLY, "m_time LIKE ?", new String[]{likeDate}));

            // 统计成功领养
            dayData.put("success", getCount(db, TABLE_MY_APPLY, "m_state=? AND m_time LIKE ?",
                    new String[]{AppConfig.State.APPLY_PASSED, likeDate}));

            result.add(dayData);
        }
        return result;
    }

    // 映射辅助方法
    @SuppressLint("Range")
    private User cursorToUser(Cursor cursor) {
        User user = new User();
        user.setId(cursor.getInt(cursor.getColumnIndex(COL_ID)));
        user.setUsername(cursor.getString(cursor.getColumnIndex("u_name")));
        user.setPassword(cursor.getString(cursor.getColumnIndex("u_psd")));
        user.setNickname(cursor.getString(cursor.getColumnIndex("u_nickname")));
        user.setState(cursor.getString(cursor.getColumnIndex("u_state")));
        user.setHead(cursor.getString(cursor.getColumnIndex("u_head")));
        user.setInfo(cursor.getString(cursor.getColumnIndex("u_info")));
        user.setTime(cursor.getString(cursor.getColumnIndex("u_time")));
        return user;
    }

    @SuppressLint("Range")
    private Adopt cursorToAdopt(Cursor cursor) {
        Adopt adopt = new Adopt();
        adopt.setId(cursor.getInt(cursor.getColumnIndex(COL_ID)));
        adopt.setSendName(getCursorString(cursor, "sendName"));
        adopt.setPetName(getCursorString(cursor, "petName"));
        adopt.setBreed(getCursorString(cursor, "breed"));
        adopt.setGender(getCursorString(cursor, "gender"));
        adopt.setAge(getCursorString(cursor, "age"));
        adopt.setPic(getCursorString(cursor, "pic"));
        adopt.setState(getCursorString(cursor, "state"));
        adopt.setAddress(getCursorString(cursor, "address"));
        adopt.setPhone(getCursorString(cursor, "phone"));
        adopt.setRemark(getCursorString(cursor, "remark"));
        adopt.setDeworming(getCursorString(cursor, "deworming"));
        adopt.setSterilization(getCursorString(cursor, "sterilization"));
        adopt.setVaccine(getCursorString(cursor, "vaccine"));
        adopt.setCycle(getCursorString(cursor, "cycle"));
        adopt.setTime(getCursorString(cursor, "time"));
        return adopt;
    }

    @SuppressLint("Range")
    private CheckIn cursorToCheckIn(Cursor cursor) {
        CheckIn bean = new CheckIn();
        bean.setId(cursor.getInt(cursor.getColumnIndex(COL_ID)));
        bean.setPetId(cursor.getInt(cursor.getColumnIndex("pet_id")));
        bean.setUsername(getCursorString(cursor, "username"));
        bean.setPetName(getCursorString(cursor, "pet_name"));
        bean.setContent(getCursorString(cursor, "content"));
        bean.setPic(getCursorString(cursor, "pic"));
        bean.setTime(getCursorString(cursor, "check_time"));
        bean.setLikeCount(cursor.getInt(cursor.getColumnIndex("like_count")));
        return bean;
    }

    @SuppressLint("Range")
    private Knowledge cursorToKnowledge(Cursor cursor) {
        Knowledge k = new Knowledge();
        k.setId(cursor.getInt(cursor.getColumnIndex(COL_ID)));
        k.setType(getCursorString(cursor, "type"));
        k.setTitle(getCursorString(cursor, "title"));
        k.setContent(getCursorString(cursor, "content"));
        k.setPics(getCursorString(cursor, "pics"));
        k.setTime(getCursorString(cursor, "time"));
        k.setUsername(getCursorString(cursor, "username"));
        k.setIsOfficial(cursor.getInt(cursor.getColumnIndex("is_official")));
        return k;
    }
}