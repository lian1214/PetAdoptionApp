package com.lian.petadoption.fragment;

import android.content.Intent;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lian.petadoption.R;
import com.lian.petadoption.activity.ApproveDetailActivity;
import com.lian.petadoption.activity.TabBarActivity;
import com.lian.petadoption.adapter.MessageAdapter;
import com.lian.petadoption.base.BaseFragment;
import com.lian.petadoption.config.AppConfig;
import com.lian.petadoption.dao.ApplyInfo;
import com.lian.petadoption.database.DataCallback;

import java.util.List;

public class MessageFragment extends BaseFragment {

    private RecyclerView recyclerView;
    private View emptyView;
    private ImageView ivDeleteAll;
    private MessageAdapter adapter;
    private String currentUsername;

    @Override
    protected int getLayoutId() {
        // 请确保 fragment_message.xml 里是 RecyclerView 而不是 ListView
        return R.layout.fragment_message;
    }

    @Override
    protected void initView(View root) {
        recyclerView = root.findViewById(R.id.rv_messages); // 注意 ID 变化
        emptyView = root.findViewById(R.id.tv_message_empty);
        ivDeleteAll = root.findViewById(R.id.iv_delete_all);

        currentUsername = sharedPreferences.getString(AppConfig.SP.USER_ACCOUNT, "");

        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        adapter = new MessageAdapter(mContext, currentUsername);
        recyclerView.setAdapter(adapter);

        // 清空已读
        ivDeleteAll.setOnClickListener(v -> showDeleteConfirmDialog());

        // 点击事件
        adapter.setOnItemClickListener((applyInfo, pos) -> handleItemClick(applyInfo));
    }

    @Override
    protected void initData() {
        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        if (currentUsername.isEmpty()) return;

        // 获取所有相关消息 (我是申请人 OR 我是发布人)
        // 这里的逻辑比较复杂，建议在 DatabaseHelper 中新增一个专门的方法 getMyAllMessages(username, callback)
        // 这里假设已经有了，或者我们用一种兼容的方式：
        // 暂时演示逻辑，你需要确保 DatabaseHelper 有此方法，否则可以先只调用 getMyApplyList
        databaseHelper.getMyAllMessages(currentUsername, new DataCallback<List<ApplyInfo>>() {
            @Override
            public void onSuccess(List<ApplyInfo> data) {
                if (data != null && !data.isEmpty()) {
                    adapter.setData(data);
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                } else {
                    recyclerView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFail(String msg) {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void handleItemClick(ApplyInfo info) {
        // 1. 标记已读 (异步)
        databaseHelper.markAsRead(info.getId());

        // 2. 刷新 TabBar 红点
        if (getActivity() instanceof TabBarActivity) {
            ((TabBarActivity) getActivity()).updateMessageBadge();
        }

        // 3. 刷新列表状态 (比如把红点去掉)
        info.setReadState(1); // 手动更新本地模型状态，避免重新请求网络
        adapter.notifyDataSetChanged();

        // 4. 跳转分流
        if (info.getName().equals(currentUsername) && !"待审核".equals(info.getState())) {
            // 我是申请人，且有结果了 -> 弹窗
            new AlertDialog.Builder(mContext)
                    .setTitle("领养申请反馈")
                    .setMessage(info.getState().contains("通过") ? "恭喜！您的领养申请已通过。" : "很遗憾，您的申请未通过。")
                    .setPositiveButton("我知道了", null)
                    .show();
        } else {
            // 我是发布者，去审核 -> 审核页
            Intent intent = new Intent(mContext, ApproveDetailActivity.class);
            intent.putExtra("apply_id", info.getId());
            startActivity(intent);
        }
    }

    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(mContext)
                .setTitle("清理提醒")
                .setMessage("确定要删除所有【已阅读】的消息吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    databaseHelper.deleteAllReadMessages(); // 需确保 DB 有此方法
                    loadData();
                })
                .setNegativeButton("取消", null).show();
    }
}