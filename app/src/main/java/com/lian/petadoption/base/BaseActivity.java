package com.lian.petadoption.base;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.lian.petadoption.R;
import com.lian.petadoption.config.AppConfig;
import com.lian.petadoption.database.DatabaseHelper;

public abstract class BaseActivity extends AppCompatActivity {
    protected Context context;
    protected DatabaseHelper databaseHelper;
    protected SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        context=this;

        // 设置布局
        setContentView(getLayoutId());
        // 初始化数据库
        databaseHelper=new DatabaseHelper(this);
        sharedPreferences=getSharedPreferences(AppConfig.SP.NAME,Context.MODE_PRIVATE);
        // 设置统一状态栏
        setStatusBar();
        // 初始化 View 和 Data
        initView();
        initData();
    }

    protected abstract void initData();

    protected abstract void initView();

    protected void setStatusBar() {
        Window window=getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.primary_color));
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

    protected abstract int getLayoutId();

    // 显示 Toast
    protected void showToast(String msg){
        runOnUiThread(()-> Toast.makeText(context,msg,Toast.LENGTH_SHORT).show());
    }

    // 界面跳转
    protected void navigateTo(Class<?> cls){
        startActivity(new Intent(context,cls));
    }
}
