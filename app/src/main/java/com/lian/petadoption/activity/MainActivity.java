package com.lian.petadoption.activity;

import android.os.Handler;
import android.os.Looper;
import android.widget.Button;

import com.lian.petadoption.R;
import com.lian.petadoption.base.BaseActivity;

public class MainActivity extends BaseActivity {

    private Button btnSkip;
    private int countDown = 3;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        btnSkip = findViewById(R.id.btn_skip);
        btnSkip.setOnClickListener(v -> jumpToNext());
    }

    @Override
    protected void initData() {
        // 定义倒计时逻辑
        runnable = new Runnable() {
            @Override
            public void run() {
                if (btnSkip != null) {
                    btnSkip.setText(String.format("%ds 跳过", countDown));
                }

                if (countDown <= 0) {
                    jumpToNext();
                } else {
                    countDown--;
                    handler.postDelayed(this, 1000);
                }
            }
        };
        // 启动倒计时
        handler.post(runnable);
    }

    private void jumpToNext() {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
        // 修正：跳转到 LoginActivity
        navigateTo(LoginActivity.class);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }
}