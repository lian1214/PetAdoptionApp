package com.lian.petadoption.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.lian.petadoption.config.AppConfig;
import com.lian.petadoption.dao.Adopt;
import com.lian.petadoption.dao.ApplyInfo;
import com.lian.petadoption.dao.CheckIn;
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

    // 统计与图表
    /**
     * 获取过去7天数据
     */
    public List<Map<String, Object>> getLast7DaysStats() {
        List<Map<String, Object>> result = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (int i = 6; i >= 0; i--) {
            // 计算日期
            long timeInMillis = System.currentTimeMillis() - i * 24 * 3600 * 1000L;
            String dateStr = sdf.format(new Date(timeInMillis));
            String likeDate = dateStr + "%";

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", dateStr.substring(5)); // MM-dd

            // 统计各项数据
            dayData.put("user", getCount(db, TABLE_USER, "u_time LIKE ?", new String[]{likeDate}));
            dayData.put("pet", getCount(db, TABLE_ADOPT, "time LIKE ?", new String[]{likeDate}));
            dayData.put("apply", getCount(db, TABLE_MY_APPLY, "m_time LIKE ?", new String[]{likeDate}));
            // 统计成功领养 (状态为已通过且时间匹配)
            dayData.put("success", getCount(db, TABLE_MY_APPLY, "m_state=? AND m_time LIKE ?",
                    new String[]{AppConfig.State.APPLY_PASSED, likeDate}));

            result.add(dayData);
        }
        return result;
    }

    private long getCount(SQLiteDatabase db, String table, String where, String[] args) {
        Cursor c = db.query(table, new String[]{"COUNT(*)"}, where, args, null, null, null);
        long count = 0;
        if (c.moveToFirst()) count = c.getLong(0);
        c.close();
        return count;
    }

    // 映射辅助方法
    @SuppressLint("Range")
    private Adopt cursorToAdopt(Cursor cursor) {
        Adopt adopt = new Adopt();
        adopt.setId(cursor.getInt(cursor.getColumnIndex(COL_ID)));
        adopt.setSendName(cursor.getString(cursor.getColumnIndex("sendName")));
        adopt.setPetName(cursor.getString(cursor.getColumnIndex("petName")));
        adopt.setBreed(cursor.getString(cursor.getColumnIndex("breed")));
        adopt.setGender(cursor.getString(cursor.getColumnIndex("gender")));
        adopt.setAge(cursor.getString(cursor.getColumnIndex("age")));
        adopt.setPic(cursor.getString(cursor.getColumnIndex("pic")));
        adopt.setState(cursor.getString(cursor.getColumnIndex("state")));
        adopt.setAddress(cursor.getString(cursor.getColumnIndex("address")));
        adopt.setTime(cursor.getString(cursor.getColumnIndex("time")));
        return adopt;
    }

    @SuppressLint("Range")
    private CheckIn cursorToCheckIn(Cursor cursor) {
        CheckIn bean = new CheckIn();
        bean.setId(cursor.getInt(cursor.getColumnIndex(COL_ID)));
        bean.setPetId(cursor.getInt(cursor.getColumnIndex("pet_id")));
        bean.setUsername(cursor.getString(cursor.getColumnIndex("username")));
        bean.setPetName(cursor.getString(cursor.getColumnIndex("pet_name")));
        bean.setContent(cursor.getString(cursor.getColumnIndex("content")));
        bean.setPic(cursor.getString(cursor.getColumnIndex("pic")));
        bean.setTime(cursor.getString(cursor.getColumnIndex("check_time")));
        bean.setLikeCount(cursor.getInt(cursor.getColumnIndex("like_count")));
        return bean;
    }
}