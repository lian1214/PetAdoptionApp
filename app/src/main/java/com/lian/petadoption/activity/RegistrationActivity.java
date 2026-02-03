package com.lian.petadoption.activity;

import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.lian.petadoption.R;
import com.lian.petadoption.base.BaseActivity;
import com.lian.petadoption.database.DataCallback;

public class RegistrationActivity extends BaseActivity {

    private EditText etUsername, etPassword, etConfirm;
    private Button btnRegister;
    private TextView tvLogin;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_registration;
    }

    @Override
    protected void initView() {
        etUsername = findViewById(R.id.username);
        etPassword = findViewById(R.id.password);
        etConfirm = findViewById(R.id.confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        tvLogin = findViewById(R.id.text2);

        // 修正：跳转到 LoginActivity
        tvLogin.setOnClickListener(v -> {
            navigateTo(LoginActivity.class);
            finish();
        });

        btnRegister.setOnClickListener(v -> performRegister());
    }

    @Override
    protected void initData() {
        // 无需预加载数据
    }

    private void performRegister() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm = etConfirm.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            showToast("请输入用户名");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            showToast("请输入密码");
            return;
        }
        if (!password.equals(confirm)) {
            showToast("两次输入的密码不一致");
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("注册中...");

        // 修正：使用 databaseHelper (BaseActivity 中的变量)
        databaseHelper.register(username, password, new DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                showToast("注册成功，请登录");
                // 修正：跳转到 LoginActivity
                navigateTo(LoginActivity.class);
                finish();
            }

            @Override
            public void onFail(String msg) {
                btnRegister.setEnabled(true);
                btnRegister.setText("注册");
                showToast(msg);
            }
        });
    }
}