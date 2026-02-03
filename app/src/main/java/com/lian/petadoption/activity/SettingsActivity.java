package com.lian.petadoption.activity;

import android.content.SharedPreferences;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;

import com.lian.petadoption.R;
import com.lian.petadoption.base.BaseActivity;
import com.lian.petadoption.config.AppConfig;
import com.lian.petadoption.database.DataCallback;

public class SettingsActivity extends BaseActivity {

    private EditText etAccount, etNickname, etBio, etPassword;
    private Button btnSave;
    private String currentAccount;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_settings;
    }

    @Override
    protected void initView() {
        // 使用 BaseActivity 的通用方法设置标题
        setToolbarTitle("个人设置");

        etAccount = findViewById(R.id.et_settings_account);
        etNickname = findViewById(R.id.et_settings_nickname);
        etBio = findViewById(R.id.et_settings_bio);
        etPassword = findViewById(R.id.et_settings_password);
        btnSave = findViewById(R.id.btn_save_settings);

        btnSave.setOnClickListener(v -> saveSettings());
    }

    @Override
    protected void initData() {
        // 回显当前数据
        currentAccount = sharedPreferences.getString(AppConfig.SP.USER_ACCOUNT, "");
        String nickname = sharedPreferences.getString(AppConfig.SP.USER_NICKNAME, "");
        String bio = sharedPreferences.getString(AppConfig.SP.USER_BIO, "");

        etAccount.setText(currentAccount);
        etNickname.setText(nickname);
        etBio.setText(bio);
    }

    private void saveSettings() {
        String newAccount = etAccount.getText().toString().trim();
        String newNick = etNickname.getText().toString().trim();
        String newBio = etBio.getText().toString().trim();
        String newPass = etPassword.getText().toString().trim(); // 密码为空则不修改

        if (TextUtils.isEmpty(newAccount)) {
            showToast("账号不能为空");
            return;
        }

        if (TextUtils.isEmpty(newNick)) {
            showToast("昵称不能为空");
            return;
        }

        // 调用 DB 更新
        databaseHelper.updateUserProfile(currentAccount, newAccount, newNick, newBio, newPass, new DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                // 更新 SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(AppConfig.SP.USER_ACCOUNT, newAccount); // 更新账号
                editor.putString(AppConfig.SP.USER_NICKNAME, newNick);   // 更新昵称
                editor.putString(AppConfig.SP.USER_BIO, newBio);         // 更新简介
                editor.apply();

                showToast("保存成功");
                finish(); // 返回个人中心
            }

            @Override
            public void onFail(String msg) {
                showToast(msg);
            }
        });
    }
}