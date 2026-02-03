package com.lian.petadoption.fragment;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.lian.petadoption.R;
import com.lian.petadoption.activity.LoginActivity;
import com.lian.petadoption.activity.MyApplyActivity;
import com.lian.petadoption.activity.MyCheckInActivity;
import com.lian.petadoption.activity.MyFavoriteActivity;
import com.lian.petadoption.activity.MyPublishActivity;
import com.lian.petadoption.activity.SettingsActivity;
import com.lian.petadoption.base.BaseFragment;
import com.lian.petadoption.config.AppConfig;
import com.lian.petadoption.database.DataCallback;
import com.lian.petadoption.utils.GlideUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MineFragment extends BaseFragment {

    private TextView tvNickname, tvBio;
    private ImageView ivAvatar;
    private ActivityResultLauncher<String> pickAvatarLauncher;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_mine;
    }

    @Override
    protected void initView(View root) {
        tvNickname = root.findViewById(R.id.tv_mine_name);
        tvBio = root.findViewById(R.id.tv_mine_bio);
        ivAvatar = root.findViewById(R.id.iv_mine_avatar);

        setupMenu(root.findViewById(R.id.ll_my_publish), "我的发布", android.R.drawable.ic_menu_gallery);
        setupMenu(root.findViewById(R.id.ll_my_apply), "我的申请", android.R.drawable.ic_menu_edit);
        setupMenu(root.findViewById(R.id.ll_my_favorite), "我的收藏", android.R.drawable.btn_star_big_on);
        setupMenu(root.findViewById(R.id.ll_my_checkin), "我的打卡", android.R.drawable.ic_menu_mylocation);
        setupMenu(root.findViewById(R.id.ll_settings), "个人设置", android.R.drawable.ic_menu_preferences); // 改名

        // 只有头像点击保留，用于快捷换头像 (你也可以选择移到设置里，看你喜好，这里保留方便一点)
        root.findViewById(R.id.cv_avatar_container).setOnClickListener(v -> pickAvatarLauncher.launch("image/*"));

        // 注意：这里删除了 btn_edit_profile 的点击事件

        // 菜单跳转
        root.findViewById(R.id.ll_my_publish).setOnClickListener(v -> navigateTo(MyPublishActivity.class));
        root.findViewById(R.id.ll_my_apply).setOnClickListener(v -> navigateTo(MyApplyActivity.class));
        root.findViewById(R.id.ll_my_favorite).setOnClickListener(v -> navigateTo(MyFavoriteActivity.class));
        root.findViewById(R.id.ll_my_checkin).setOnClickListener(v -> navigateTo(MyCheckInActivity.class));
        root.findViewById(R.id.ll_settings).setOnClickListener(v -> navigateTo(SettingsActivity.class));

        root.findViewById(R.id.ll_logout).setOnClickListener(v -> {
            sharedPreferences.edit().clear().apply();
            Intent intent = new Intent(mContext, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        initLauncher();
    }

    @Override
    protected void initData() {
        loadUserData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData(); // 从设置页回来后，这里会自动刷新显示最新信息
    }

    private void loadUserData() {
        String nickname = sharedPreferences.getString(AppConfig.SP.USER_NICKNAME, "未设置");
        String bio = sharedPreferences.getString(AppConfig.SP.USER_BIO, "这家伙很懒...");
        String avatarPath = sharedPreferences.getString(AppConfig.SP.USER_AVATAR, "");

        tvNickname.setText(nickname);
        tvBio.setText(bio);
        GlideUtils.loadCircle(mContext, avatarPath, ivAvatar);
    }

    private void initLauncher() {
        pickAvatarLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                String account = sharedPreferences.getString(AppConfig.SP.USER_ACCOUNT, "");
                String path = saveAvatarToInternalStorage(uri, account);
                if (path != null) {
                    sharedPreferences.edit().putString(AppConfig.SP.USER_AVATAR, path).apply();
                    // 这里仅更新头像
                    databaseHelper.updateUserProfile(account, null, null, null, null, new DataCallback<Boolean>() {
                        @Override public void onSuccess(Boolean data) { showToast("头像更新成功"); }
                        @Override public void onFail(String msg) {}
                    });
                    loadUserData();
                }
            }
        });
    }

    private String saveAvatarToInternalStorage(Uri uri, String account) {
        // ... (保持原有的保存图片代码不变) ...
        try {
            File directory = new File(mContext.getFilesDir(), "avatars");
            if (!directory.exists()) directory.mkdirs();
            File file = new File(directory, "avatar_" + account + "_" + System.currentTimeMillis() + ".jpg");
            InputStream inputStream = mContext.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) { outputStream.write(buffer, 0, read); }
            outputStream.flush(); outputStream.close(); inputStream.close();
            return file.getAbsolutePath();
        } catch (Exception e) { return null; }
    }

    private void setupMenu(View menuView, String title, int iconRes) {
        if (menuView == null) return;
        TextView tv = menuView.findViewById(R.id.tv_menu_title);
        ImageView iv = menuView.findViewById(R.id.iv_menu_icon);
        if (tv != null) tv.setText(title);
        if (iv != null) iv.setImageResource(iconRes);
    }
}