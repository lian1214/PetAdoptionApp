package com.lian.petadoption.activity;

import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lian.petadoption.R;
import com.lian.petadoption.adapter.CheckInAdapter; // 需确认此Adapter存在
import com.lian.petadoption.base.BaseActivity;
import com.lian.petadoption.config.AppConfig;
import com.lian.petadoption.dao.CheckIn;
import com.lian.petadoption.database.DataCallback;

import java.util.List;

public class MyCheckInActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private View layoutEmpty;
    private CheckInAdapter adapter;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_my_check_in;
    }

    @Override
    protected void initView() {
        setToolbarTitle("我的打卡记录");

        recyclerView = findViewById(R.id.recycler_view_checkin);
        layoutEmpty = findViewById(R.id.tv_empty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // 假设 CheckInAdapter 构造函数适配了 BaseAdapter 风格，或者保留你原有的构造
        adapter = new CheckInAdapter(this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void initData() {
        String username = sharedPreferences.getString(AppConfig.SP.USER_ACCOUNT, "");

        // 假设 DatabaseHelper 中有 getCheckInList 方法 (你之前的代码里有调用)
        // 如果没有，请在 DatabaseHelper 中添加类似 getMyPublishList 的逻辑
        databaseHelper.getMyCheckInList(username, new DataCallback<List<CheckIn>>() {
            @Override
            public void onSuccess(List<CheckIn> data) {
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