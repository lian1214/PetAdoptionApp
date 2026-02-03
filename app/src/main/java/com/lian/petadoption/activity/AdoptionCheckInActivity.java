package com.lian.petadoption.activity;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.lian.petadoption.R;
import com.lian.petadoption.adapter.CheckInAdapter;
import com.lian.petadoption.base.BaseActivity;
import com.lian.petadoption.config.AppConfig;
import com.lian.petadoption.dao.ApplyInfo;
import com.lian.petadoption.dao.CheckIn;
import com.lian.petadoption.database.DataCallback;
import com.lian.petadoption.utils.ImagePickerHelper;

import java.util.ArrayList;
import java.util.List;

public class AdoptionCheckInActivity extends BaseActivity {

    private RecyclerView rvPunchList;
    private CheckInAdapter adapter;
    private List<CheckIn> punchDataList = new ArrayList<>();

    // 搜索与发布相关
    private String currentUsername;
    private String lastSearchKeyword = "";

    // 发布弹窗相关 (成员变量以便在回调中更新)
    private List<Uri> dialogSelectedUris = new ArrayList<>();
    private PreviewImageAdapter dialogPreviewAdapter;
    private ImagePickerHelper imagePickerHelper;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_adoption_check_in;
    }

    @Override
    protected void initView() {
        setToolbarTitle("领养打卡广场");

        // 设置右上角搜索按钮
        ImageView ivSearch = findViewById(R.id.iv_search);
        ivSearch.setVisibility(View.VISIBLE);
        ivSearch.setOnClickListener(v -> showSearchDialog());

        rvPunchList = findViewById(R.id.rv_punch_list);
        rvPunchList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CheckInAdapter(this, punchDataList, CheckInAdapter.TYPE_PUBLIC_SQUARE);
        rvPunchList.setAdapter(adapter);

        findViewById(R.id.fab_add_punch).setOnClickListener(v -> handleFabClick());

        // 初始化图片选择器 (回调中更新弹窗内的列表)
        imagePickerHelper = new ImagePickerHelper(this, 9, uris -> {
            dialogSelectedUris.addAll(uris);
            if (dialogPreviewAdapter != null) {
                dialogPreviewAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void initData() {
        currentUsername = sharedPreferences.getString(AppConfig.SP.USER_ACCOUNT, "");
        loadData();
    }

    private void loadData() {
        punchDataList.clear();
        DataCallback<List<CheckIn>> callback = new DataCallback<List<CheckIn>>() {
            @Override
            public void onSuccess(List<CheckIn> data) {
                if (data != null) {
                    punchDataList.addAll(data);
                    adapter.notifyDataSetChanged();
                }
                if (data == null || data.isEmpty()) {
                    showToast(TextUtils.isEmpty(lastSearchKeyword) ? "暂无打卡动态" : "未找到相关内容");
                }
            }

            @Override
            public void onFail(String msg) {
                showToast(msg);
            }
        };

        // 判断是否是搜索模式
        databaseHelper.getAllCheckIns(callback);
    }

    private void performSearch(String keyword) {
        lastSearchKeyword = keyword;
        databaseHelper.getAllCheckIns(new DataCallback<List<CheckIn>>() {
            @Override
            public void onSuccess(List<CheckIn> data) {
                punchDataList.clear();
                if (data != null) {
                    for (CheckIn item : data) {
                        // 简单模糊匹配
                        if (item.getContent().contains(keyword) || item.getPetName().contains(keyword)) {
                            punchDataList.add(item);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
                if (punchDataList.isEmpty()) showToast("未找到相关内容");
            }
            @Override
            public void onFail(String msg) {}
        });
    }

    private void handleFabClick() {
        if (TextUtils.isEmpty(currentUsername)) {
            showToast("请先登录");
            return;
        }

        // 异步查询我的领养记录，筛选出"已通过"的
        databaseHelper.getMyApplyList(currentUsername, new DataCallback<List<ApplyInfo>>() {
            @Override
            public void onSuccess(List<ApplyInfo> data) {
                List<ApplyInfo> myApprovedPets = new ArrayList<>();
                for (ApplyInfo info : data) {
                    if (AppConfig.State.APPLY_PASSED.equals(info.getState())) {
                        myApprovedPets.add(info);
                    }
                }

                if (myApprovedPets.isEmpty()) {
                    showNoApprovedPetDialog();
                } else {
                    showPublishDialog(myApprovedPets);
                }
            }

            @Override
            public void onFail(String msg) {
                showToast("获取数据失败");
            }
        });
    }

    private void showPublishDialog(List<ApplyInfo> myPets) {
        dialogSelectedUris.clear();
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_check_in, null);

        Spinner spinner = dialogView.findViewById(R.id.dialog_spinner_pets);
        EditText etContent = dialogView.findViewById(R.id.dialog_et_content);
        RecyclerView rvPreview = dialogView.findViewById(R.id.dialog_rv_preview);
        Button btnCancel = dialogView.findViewById(R.id.dialog_btn_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.dialog_btn_confirm);
        View layoutUpload = dialogView.findViewById(R.id.layout_upload_pic); // 确保 XML 中有这个 ID

        // 设置 Spinner 数据
        List<String> names = new ArrayList<>();
        for (ApplyInfo info : myPets) {
            names.add(info.getPetName());
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names);
        spinner.setAdapter(spinnerAdapter);

        // 设置图片预览
        rvPreview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        dialogPreviewAdapter = new PreviewImageAdapter(dialogSelectedUris);
        rvPreview.setAdapter(dialogPreviewAdapter);

        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setView(dialogView);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // 点击上传，调用 Activity 的 Helper
        layoutUpload.setOnClickListener(v -> imagePickerHelper.pick());

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String content = etContent.getText().toString().trim();
            if (TextUtils.isEmpty(content)) {
                showToast("请输入打卡内容");
                return;
            }
            if (dialogSelectedUris.isEmpty()) {
                showToast("请至少上传一张照片");
                return;
            }

            // 获取选中的宠物信息
            int pos = spinner.getSelectedItemPosition();
            if (pos >= 0 && pos < myPets.size()) {
                ApplyInfo selectedPet = myPets.get(pos);

                // 拼接图片
                StringBuilder sb = new StringBuilder();
                for (Uri uri : dialogSelectedUris) {
                    sb.append(uri.toString()).append(AppConfig.IMAGE_SPLIT_SYMBOL);
                }
                if (sb.length() > 0) sb.setLength(sb.length() - 1);

                // 异步发布
                databaseHelper.publishCheckIn(currentUsername, 0, selectedPet.getPetName(), content, sb.toString(), new DataCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean data) {
                        showToast("打卡成功！");
                        dialog.dismiss();
                        loadData(); // 刷新列表
                    }

                    @Override
                    public void onFail(String msg) {
                        showToast("发布失败: " + msg);
                    }
                });
            }
        });
        dialog.show();
    }

    private void showSearchDialog() {
        EditText et = new EditText(this);
        et.setHint("搜索内容或宠物名");
        et.setText(lastSearchKeyword);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        et.setPadding(padding, padding, padding, padding);
        et.setBackground(null);

        new AlertDialog.Builder(this)
                .setTitle("搜索打卡")
                .setView(et)
                .setPositiveButton("搜索", (d, w) -> performSearch(et.getText().toString().trim()))
                .setNeutralButton("显示全部", (d, w) -> {
                    lastSearchKeyword = "";
                    loadData();
                })
                .show();
    }

    private void showNoApprovedPetDialog() {
        new AlertDialog.Builder(this)
                .setTitle("温馨提示")
                .setMessage("您还没有成功领养的宠物，无法发布打卡哦。\n只有审核通过的领养记录才可以进行打卡。")
                .setPositiveButton("我知道了", null)
                .show();
    }

    // --- 修复后的 PreviewImageAdapter ---
    private static class PreviewImageAdapter extends RecyclerView.Adapter<PreviewImageAdapter.VH> {
        private final List<Uri> list;

        public PreviewImageAdapter(List<Uri> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView iv = new ImageView(parent.getContext()); // 使用 parent.getContext()
            int size = (int) (70 * parent.getContext().getResources().getDisplayMetrics().density);
            // 明确使用 ViewGroup.MarginLayoutParams
            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(size, size);
            lp.setMargins(0, 0, 16, 0); // 现在 setMargins 可以正常使用了
            iv.setLayoutParams(lp);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new VH(iv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Glide.with(holder.itemView).load(list.get(position)).into((ImageView) holder.itemView);
            holder.itemView.setOnLongClickListener(v -> {
                list.remove(position);
                notifyDataSetChanged();
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            public VH(View v) {
                super(v);
            }
        }
    }
}