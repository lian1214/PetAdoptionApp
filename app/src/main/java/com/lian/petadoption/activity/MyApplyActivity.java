package com.lian.petadoption.activity;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lian.petadoption.R;
import com.lian.petadoption.adapter.MyApplyAdapter;
import com.lian.petadoption.base.BaseActivity;
import com.lian.petadoption.config.AppConfig;
import com.lian.petadoption.dao.ApplyInfo;
import com.lian.petadoption.database.DataCallback;

import java.util.List;

public class MyApplyActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private View layoutEmpty;
    private MyApplyAdapter adapter;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_my_apply;
    }

    @Override
    protected void initView() {
        setToolbarTitle("我的申请进度"); // BaseActivity 方法

        recyclerView = findViewById(R.id.recycler_view_my);
        layoutEmpty = findViewById(R.id.tv_empty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyApplyAdapter(this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void initData() {
        String username = sharedPreferences.getString(AppConfig.SP.USER_ACCOUNT, "");

        databaseHelper.getMyApplyList(username, new DataCallback<List<ApplyInfo>>() {
            @Override
            public void onSuccess(List<ApplyInfo> data) {
                if (data != null && !data.isEmpty()) {
                    adapter.setData(data);
                    updateEmptyState(false);
                } else {
                    updateEmptyState(true);
                }
            }

            @Override
            public void onFail(String msg) {
                updateEmptyState(true);
            }
        });
    }

    private void updateEmptyState(boolean isEmpty) {
        layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}