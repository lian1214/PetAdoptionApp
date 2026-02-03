package com.lian.petadoption.fragment;

import android.content.Context;
import android.net.Uri;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.lian.petadoption.R;
import com.lian.petadoption.base.BaseFragment;
import com.lian.petadoption.base.BaseRecyclerAdapter;
import com.lian.petadoption.config.AppConfig;
import com.lian.petadoption.dao.Knowledge;
import com.lian.petadoption.database.DataCallback;
import com.lian.petadoption.utils.ImagePickerHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KnowledgeManageFragment extends BaseFragment {

    private RecyclerView recyclerView;
    private EditText etSearch;
    private TextView tvBatchDelete;

    private KnowledgeManageAdapter adapter;
    private List<Knowledge> knowledgeList = new ArrayList<>();
    private Set<Integer> selectedIds = new HashSet<>();

    // 发布相关
    private ImagePickerHelper imagePickerHelper;
    private List<Uri> dialogSelectedUris = new ArrayList<>();
    private DialogImageAdapter dialogImageAdapter;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_knowledge_manage;
    }

    @Override
    protected void initView(View root) {
        recyclerView = root.findViewById(R.id.rv_knowledge_manage);
        etSearch = root.findViewById(R.id.et_search_knowledge);
        tvBatchDelete = root.findViewById(R.id.tv_batch_delete);

        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        adapter = new KnowledgeManageAdapter(mContext);
        recyclerView.setAdapter(adapter);

        root.findViewById(R.id.fab_add_knowledge).setOnClickListener(v -> showPublishDialog());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadData(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        tvBatchDelete.setOnClickListener(v -> handleBatchDelete());

        // 初始化图片选择器
        imagePickerHelper = new ImagePickerHelper(this, 6, uris -> {
            dialogSelectedUris.addAll(uris);
            if (dialogImageAdapter != null) dialogImageAdapter.notifyDataSetChanged();
        });
    }

    @Override
    protected void initData() {
        loadData("");
    }

    private void loadData(String keyword) {
        DataCallback<List<Knowledge>> callback = new DataCallback<List<Knowledge>>() {
            @Override
            public void onSuccess(List<Knowledge> data) {
                knowledgeList.clear();
                knowledgeList.addAll(data);
                adapter.setData(knowledgeList);
                selectedIds.clear();
                adapter.notifyDataSetChanged();
            }
            @Override public void onFail(String msg) { showToast(msg); }
        };

        if (TextUtils.isEmpty(keyword)) {
            // 需要在 DatabaseHelper 中实现 getAllKnowledge(callback)
            // 如果没有，可以复用 searchKnowledge("", "", callback)
            databaseHelper.getKnowledgeList("pet", callback); // 暂时只查 pet，建议实现 getAll
        } else {
            databaseHelper.searchKnowledge("", keyword, callback);
        }
    }

    private void showPublishDialog() {
        dialogSelectedUris.clear();
        View v = LayoutInflater.from(mContext).inflate(R.layout.dialog_admin_publish_knowledge, null);
        EditText etTitle = v.findViewById(R.id.et_dialog_title);
        EditText etContent = v.findViewById(R.id.et_dialog_content);
        EditText etTag = v.findViewById(R.id.et_dialog_tag);
        Spinner spType = v.findViewById(R.id.sp_dialog_type);
        RecyclerView rvPreview = v.findViewById(R.id.rv_picker_preview);
        View btnAddImg = v.findViewById(R.id.btn_picker_add);

        // 图片预览配置
        rvPreview.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false));
        dialogImageAdapter = new DialogImageAdapter();
        rvPreview.setAdapter(dialogImageAdapter);

        btnAddImg.setOnClickListener(view -> imagePickerHelper.pick());

        String[] types = {"领养知识", "宠物知识"};
        spType.setAdapter(new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_dropdown_item, types));

        new AlertDialog.Builder(mContext)
                .setTitle("发布官方知识")
                .setView(v)
                .setPositiveButton("发布", (d, w) -> {
                    String title = etTitle.getText().toString().trim();
                    String content = etContent.getText().toString().trim();
                    String tag = etTag.getText().toString().trim();

                    if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
                        showToast("请填写完整");
                        return;
                    }

                    String typeStr = spType.getSelectedItem().toString();
                    String baseType = typeStr.equals("领养知识") ? "adopt" : "pet";
                    String finalType = TextUtils.isEmpty(tag) ? baseType : baseType + ":" + tag;

                    StringBuilder sb = new StringBuilder();
                    for (Uri uri : dialogSelectedUris) sb.append(uri.toString()).append(AppConfig.IMAGE_SPLIT_SYMBOL);

                    databaseHelper.addKnowledge(finalType, "管理员", title, content, sb.toString(), true, new DataCallback<Boolean>() {
                        @Override public void onSuccess(Boolean data) { showToast("发布成功"); loadData(""); }
                        @Override public void onFail(String msg) { showToast("发布失败"); }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void handleBatchDelete() {
        // 需在 DatabaseHelper 中实现 deleteKnowledge
        // 暂时演示逻辑
        showToast("批量删除功能需完善 DatabaseHelper");
    }

    // --- Adapter ---
    private class KnowledgeManageAdapter extends BaseRecyclerAdapter<Knowledge, KnowledgeManageAdapter.VH> {
        public KnowledgeManageAdapter(Context context) { super(context); }

        @Override
        protected VH onCreateVH(ViewGroup parent, int viewType) {
            // 复用 item_knowledge_manage.xml (需自行创建或根据之前代码)
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_pet_manage_list, parent, false); // 暂时复用宠物布局样式
            return new VH(view);
        }

        @Override
        protected void onBindVH(VH holder, Knowledge item, int position) {
            holder.tvTitle.setText(item.getTitle());
            holder.tvInfo.setText(item.getType() + " | " + item.getUsername());
            holder.tvTime.setText(item.getTime());

            holder.cbSelect.setOnCheckedChangeListener(null);
            holder.cbSelect.setChecked(selectedIds.contains(item.getId()));
            holder.cbSelect.setOnCheckedChangeListener((v, isChecked) -> {
                if(isChecked) selectedIds.add(item.getId()); else selectedIds.remove(item.getId());
            });
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvInfo, tvTime;
            CheckBox cbSelect;
            public VH(View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tv_pet_name); // 复用ID
                tvInfo = v.findViewById(R.id.tv_pet_info);
                tvTime = v.findViewById(R.id.tv_publish_time);
                cbSelect = v.findViewById(R.id.item_cb_select);
                v.findViewById(R.id.tv_status_tag).setVisibility(View.GONE); // 隐藏不需要的
                v.findViewById(R.id.iv_pet_pic).setVisibility(View.GONE);
            }
        }
    }

    private class DialogImageAdapter extends RecyclerView.Adapter<DialogImageAdapter.VH> {
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            ImageView iv = new ImageView(mContext);
            int s = (int) (80 * getResources().getDisplayMetrics().density);
            iv.setLayoutParams(new ViewGroup.MarginLayoutParams(s, s));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new VH(iv);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int p) {
            Glide.with(mContext).load(dialogSelectedUris.get(p)).into((ImageView) h.itemView);
        }
        @Override public int getItemCount() { return dialogSelectedUris.size(); }
        class VH extends RecyclerView.ViewHolder { public VH(View v) { super(v); } }
    }
}