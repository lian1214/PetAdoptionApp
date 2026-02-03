package com.lian.petadoption.activity;

import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.lian.petadoption.R;
import com.lian.petadoption.base.BaseActivity;
import com.lian.petadoption.config.AppConfig;
import com.lian.petadoption.database.DataCallback;

import java.util.Map;

public class LoginActivity extends BaseActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private CheckBox cbAdmin;
    private ProgressBar progressBar;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_login;
    }

    @Override
    protected void initView() {
        etUsername = findViewById(R.id.username);
        etPassword = findViewById(R.id.password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.text2);
        cbAdmin = findViewById(R.id.isAdmin);
        // 如果XML里加了progressBar，请取消注释
        // progressBar = findViewById(R.id.progressBar);

        // 修正：跳转到 RegistrationActivity
        tvRegister.setOnClickListener(v -> navigateTo(RegistrationActivity.class));
        btnLogin.setOnClickListener(v -> performLogin());
    }

    @Override
    protected void initData() {
        // 修正：使用 sharedPreferences (BaseActivity 中的变量)
        String lastUser = sharedPreferences.getString(AppConfig.SP.USER_ACCOUNT, "");
        if (!TextUtils.isEmpty(lastUser)) {
            etUsername.setText(lastUser);
        }
    }

    private void performLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            showToast("请输入用户名");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            showToast("请输入密码");
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("登录中...");
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        // 修正：使用 databaseHelper (BaseActivity 中的变量)
        databaseHelper.login(username, password, new DataCallback<Map<String, String>>() {
            @Override
            public void onSuccess(Map<String, String> userInfo) {
                handleLoginSuccess(username, userInfo);
            }

            @Override
            public void onFail(String msg) {
                resetButtonState();
                showToast(msg);
            }
        });
    }

    private void handleLoginSuccess(String username, Map<String, String> userInfo) {
        boolean isAdminLogin = cbAdmin.isChecked();

        if (isAdminLogin) {
            if (AppConfig.Role.ADMIN.equals(username)) {
                saveLoginInfo(username, AppConfig.Role.ADMIN, userInfo);
                showToast("管理员登录成功");
                // 确保 Management 类存在，如果文件名改了请同步修改这里
                navigateTo(ManagementActivity.class);
                finish();
            } else {
                resetButtonState();
                showToast("该账号没有管理员权限");
            }
        } else {
            if (AppConfig.Role.ADMIN.equals(username)) {
                showToast("管理员请勾选“管理员登录”");
                resetButtonState();
                return;
            }
            saveLoginInfo(username, AppConfig.Role.USER, userInfo);
            showToast("登录成功");
            // 确保 TabBar 类存在，如果文件名改了请同步修改这里
            navigateTo(TabBarActivity.class);
            finish();
        }
    }

    private void saveLoginInfo(String username, String role, Map<String, String> userInfo) {
        // 修正：使用 sharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(AppConfig.SP.USER_ACCOUNT, username);
        editor.putString(AppConfig.SP.USER_ROLE, role);

        if (userInfo != null) {
            String nick = userInfo.get("u_nickname");
            String head = userInfo.get("u_head");
            String info = userInfo.get("u_info");

            editor.putString(AppConfig.SP.USER_NICKNAME, TextUtils.isEmpty(nick) ? "未设置" : nick);
            editor.putString(AppConfig.SP.USER_AVATAR, head);
            editor.putString(AppConfig.SP.USER_BIO, TextUtils.isEmpty(info) ? "这家伙很懒..." : info);
        }

        editor.apply();
    }

    private void resetButtonState() {
        btnLogin.setEnabled(true);
        btnLogin.setText("登录");
        if (progressBar != null) progressBar.setVisibility(View.GONE);
    }
}