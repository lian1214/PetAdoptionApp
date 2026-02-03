package com.lian.petadoption.activity;

import android.content.Intent;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lian.petadoption.R;
import com.lian.petadoption.adapter.FavoriteAdapter;
import com.lian.petadoption.base.BaseActivity;
import com.lian.petadoption.config.AppConfig;
import com.lian.petadoption.dao.Adopt;
import com.lian.petadoption.database.DataCallback;

import java.util.List;

public class MyFavoriteActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private View layoutEmpty;
    private FavoriteAdapter adapter;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_my_favorite;
    }

    @Override
    protected void initView() {
        setToolbarTitle("我的收藏");

        recyclerView = findViewById(R.id.recycler_view_fav);
        layoutEmpty = findViewById(R.id.tv_empty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FavoriteAdapter(this);
        recyclerView.setAdapter(adapter);

        // 点击跳转
        adapter.setOnItemClickListener((adopt, pos) -> {
            Intent intent = new Intent(this, PetDetailActivity.class);
            intent.putExtra(AppConfig.Extra.ADOPT_DATA, adopt);
            startActivity(intent);
        });

        // 监听删除事件 (如果在 Adapter 内部处理了取消收藏)
        adapter.setOnItemRemoveListener(count -> {
            if (count == 0) updateEmptyState(true);
        });
    }

    @Override
    protected void initData() {
        loadData();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        loadData(); // 详情页可能取消收藏
    }

    private void loadData() {
        String username = sharedPreferences.getString(AppConfig.SP.USER_ACCOUNT, "");

        databaseHelper.getMyFavoriteList(username, new DataCallback<List<Adopt>>() {
            @Override
            public void onSuccess(List<Adopt> data) {
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