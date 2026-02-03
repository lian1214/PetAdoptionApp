package com.lian.petadoption.activity;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.lian.petadoption.R;
import com.lian.petadoption.adapter.KnowledgeAdapter;
import com.lian.petadoption.base.BaseActivity;
import com.lian.petadoption.config.AppConfig;
import com.lian.petadoption.dao.Knowledge;
import com.lian.petadoption.database.DataCallback;
import com.lian.petadoption.utils.ImagePickerHelper;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseKnowledgeActivity extends BaseActivity {

    protected KnowledgeAdapter adapter;
    protected List<Knowledge> mList = new ArrayList<>();

    // 图片选择相关
    protected List<Uri> selectedUris = new ArrayList<>();
    protected ImagePreviewAdapter previewAdapter;
    protected ImagePickerHelper imagePickerHelper;

    // --- 抽象方法：由子类定义 ---
    protected abstract String getKnowledgeType(); // "pet" 或 "adopt"
    protected abstract String getTitleText();     // 标题栏文字
    protected abstract void initDefaultDataIfNeeded(); // 初始化默认数据

    @Override
    protected int getLayoutId() {
        return R.layout.activity_knowledge_base;
    }

    @Override
    protected void initView() {
        setToolbarTitle(getTitleText());

        // 设置右上角搜索按钮
        ImageView ivSearch = findViewById(R.id.iv_search);
        ivSearch.setVisibility(View.VISIBLE);
        ivSearch.setOnClickListener(v -> showSearchDialog());

        // 初始化列表
        RecyclerView rv = findViewById(R.id.rv_knowledge_list);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new KnowledgeAdapter(this);

        adapter.setOnItemClickListener((knowledge, position) -> {
            Intent intent = new Intent(this, KnowledgeDetailActivity.class);
            // 传入数据对象，Key 建议使用 AppConfig 中定义的常量，或者保持与详情页接收时一致
            intent.putExtra(AppConfig.Extra.KNOWLEDGE_DATA, knowledge);
            startActivity(intent);
        });

        rv.setAdapter(adapter);

        // 发布按钮
        findViewById(R.id.fab_add).setOnClickListener(v -> showPublishDialog());

        // 初始化图片选择器 (支持多选，最多9张)
        imagePickerHelper = new ImagePickerHelper(this, 9, uris -> {
            selectedUris.addAll(uris);
            if (previewAdapter != null) previewAdapter.notifyDataSetChanged();
        });
    }

    @Override
    protected void initData() {
        initDefaultDataIfNeeded(); // 子类决定是否插入初始数据
        loadData("");
    }

    protected void loadData(String keyword) {
        // 注意：这里先不要清空 mList，等数据回来再说

        DataCallback<List<Knowledge>> callback = new DataCallback<List<Knowledge>>() {
            @Override
            public void onSuccess(List<Knowledge> data) {
                // ==========================================
                // 【核心修改】 必须调用 adapter.setData(data) !!!
                // 这样 BaseRecyclerAdapter 内部的数据才会更新
                // ==========================================
                if (adapter != null) {
                    adapter.setData(data);
                }

                // (可选) 同步更新本地 mList 方便调试，但显示主要靠 adapter
                mList.clear();
                if (data != null) mList.addAll(data);

                if ((data == null || data.isEmpty()) && !TextUtils.isEmpty(keyword)) {
                    showToast("未找到相关内容");
                }
            }
            @Override
            public void onFail(String msg) {
                showToast(msg);
            }
        };

        if (TextUtils.isEmpty(keyword)) {
            databaseHelper.getKnowledgeList(getKnowledgeType(), callback);
        } else {
            databaseHelper.searchKnowledge(getKnowledgeType(), keyword, callback);
        }
    }

    private void showSearchDialog() {
        EditText et = new EditText(this);
        et.setHint("输入标签或标题关键字");
        // 简单的样式设置
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        et.setPadding(padding, padding, padding, padding);
        et.setBackground(null);

        new AlertDialog.Builder(this)
                .setTitle("搜索")
                .setView(et)
                .setPositiveButton("搜索", (d, w) -> loadData(et.getText().toString().trim()))
                .setNeutralButton("重置", (d, w) -> loadData(""))
                .show();
    }

    private void showPublishDialog() {
        String currentUser = sharedPreferences.getString(AppConfig.SP.USER_ACCOUNT, "");
        if (TextUtils.isEmpty(currentUser)) {
            showToast("请先登录");
            return;
        }

        selectedUris.clear();
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_publish_knowledge, null);
        EditText etTag = v.findViewById(R.id.et_tag);
        EditText etTitle = v.findViewById(R.id.et_title);
        EditText etContent = v.findViewById(R.id.et_content);
        RecyclerView rvPreview = v.findViewById(R.id.rv_publish_images);

        // 初始化弹窗内的图片预览列表
        rvPreview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        previewAdapter = new ImagePreviewAdapter(selectedUris);
        rvPreview.setAdapter(previewAdapter);

        // 绑定上传点击
        v.findViewById(R.id.iv_add_image).setOnClickListener(view -> imagePickerHelper.pick());

        new AlertDialog.Builder(this)
                .setTitle("分享知识")
                .setView(v)
                .setPositiveButton("发布", (d, w) -> {
                    String tag = etTag.getText().toString().trim();
                    String title = etTitle.getText().toString().trim();
                    String content = etContent.getText().toString().trim();

                    if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
                        showToast("标题和内容不能为空");
                        return;
                    }

                    // 构造类型字符串 (baseType:tag)
                    String finalType = tag.isEmpty() ? getKnowledgeType() : getKnowledgeType() + ":" + tag;

                    // 拼接图片路径
                    StringBuilder sb = new StringBuilder();
                    for (Uri uri : selectedUris) {
                        sb.append(uri.toString()).append(AppConfig.IMAGE_SPLIT_SYMBOL);
                    }
                    if (sb.length() > 0) sb.setLength(sb.length() - 1);

                    // 异步发布
                    databaseHelper.addKnowledge(finalType, currentUser, title, content, sb.toString(), false, new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean data) {
                            showToast("发布成功");
                            loadData("");
                        }
                        @Override
                        public void onFail(String msg) {
                            showToast("发布失败");
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 内部图片预览适配器
    private static class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.VH> {
        private List<Uri> uris;
        public ImagePreviewAdapter(List<Uri> uris) { this.uris = uris; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            ImageView iv = new ImageView(p.getContext());
            int size = (int) (80 * p.getContext().getResources().getDisplayMetrics().density);
            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(size, size);
            lp.setMargins(0, 0, 16, 0);
            iv.setLayoutParams(lp);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new VH(iv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int p) {
            Glide.with(h.itemView.getContext()).load(uris.get(p)).into((ImageView) h.itemView);
            h.itemView.setOnLongClickListener(v -> {
                uris.remove(p);
                notifyDataSetChanged();
                return true;
            });
        }

        @Override public int getItemCount() { return uris.size(); }
        static class VH extends RecyclerView.ViewHolder { VH(View v) { super(v); } }
    }
}