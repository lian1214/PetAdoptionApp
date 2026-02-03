package com.lian.petadoption.fragment;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
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
import com.lian.petadoption.dao.Adopt;
import com.lian.petadoption.database.DataCallback;
import com.lian.petadoption.utils.ImagePickerHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PetManageFragment extends BaseFragment {

    private RecyclerView recyclerView;
    private EditText etSearch;
    private TextView tvBatchDelete;

    private PetManageAdapter adapter;
    private List<Adopt> petList = new ArrayList<>();
    private Set<Integer> selectedIds = new HashSet<>();

    // 编辑/新增相关
    private ImagePickerHelper imagePickerHelper;
    private List<Uri> dialogSelectedUris = new ArrayList<>();
    private DialogImageAdapter dialogImageAdapter;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_pet_manage;
    }

    @Override
    protected void initView(View root) {
        etSearch = root.findViewById(R.id.et_search_pet);
        tvBatchDelete = root.findViewById(R.id.tv_batch_delete);
        recyclerView = root.findViewById(R.id.pet_recycler_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        adapter = new PetManageAdapter(mContext);
        recyclerView.setAdapter(adapter);

        // 搜索监听
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadData(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        tvBatchDelete.setOnClickListener(v -> handleBatchDelete());

        // 发布按钮 (假设布局中有 fab)
        // root.findViewById(R.id.fab_add_pet).setOnClickListener(v -> showEditDialog(null));

        // 初始化图片选择器 (用于弹窗)
        imagePickerHelper = new ImagePickerHelper(this, 9, uris -> {
            dialogSelectedUris.addAll(uris);
            if (dialogImageAdapter != null) dialogImageAdapter.notifyDataSetChanged();
        });
    }

    @Override
    protected void initData() {
        loadData("");
    }

    private void loadData(String keyword) {
        DataCallback<List<Adopt>> callback = new DataCallback<List<Adopt>>() {
            @Override
            public void onSuccess(List<Adopt> data) {
                petList.clear();
                petList.addAll(data);
                adapter.setData(petList);
                selectedIds.clear();
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onFail(String msg) {
                showToast(msg);
            }
        };

        if (TextUtils.isEmpty(keyword)) {
            databaseHelper.getAllPets(callback);
        } else {
            databaseHelper.searchPets(keyword, callback);
        }
    }

    private void handleBatchDelete() {
        if (selectedIds.isEmpty()) {
            showToast("请先勾选要删除的宠物");
            return;
        }
        new AlertDialog.Builder(mContext)
                .setTitle("确认删除")
                .setMessage("删除选中的 " + selectedIds.size() + " 条记录？")
                .setPositiveButton("确定", (d, w) -> {
                    for (Integer id : selectedIds) {
                        databaseHelper.deletePet(id, null); // 需适配异步
                    }
                    showToast("删除操作已提交");
                    // 延迟刷新或在回调中刷新
                    recyclerView.postDelayed(() -> loadData(etSearch.getText().toString()), 500);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // --- 列表 Adapter ---
    private class PetManageAdapter extends BaseRecyclerAdapter<Adopt, PetManageAdapter.VH> {
        public PetManageAdapter(Context context) { super(context); }

        @Override
        protected VH onCreateVH(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_pet_manage_list, parent, false);
            return new VH(view);
        }

        @Override
        protected void onBindVH(VH holder, Adopt item, int position) {
            holder.tvName.setText(item.getPetName());
            holder.tvInfo.setText(item.getBreed() + " | " + item.getGender() + " | " + item.getAge());
            holder.tvTime.setText("发布于 " + item.getTime());

            // 状态标签
            String state = item.getState();
            holder.tvStatus.setText(state);
            if (AppConfig.State.PET_ADOPTED.equals(state)) {
                holder.tvStatus.setTextColor(Color.parseColor("#999999"));
            } else {
                holder.tvStatus.setTextColor(Color.parseColor("#FFD90E"));
            }

            // 图片
            String picRaw = item.getPic();
            Object imgObj = R.drawable.app_logo_bf;
            if (!TextUtils.isEmpty(picRaw)) {
                String firstPath = picRaw.contains(AppConfig.IMAGE_SPLIT_SYMBOL) ? picRaw.split(AppConfig.IMAGE_SPLIT_SYMBOL)[0] : picRaw;
                imgObj = firstPath;
            }
            Glide.with(mContext).load(imgObj).centerCrop().into(holder.ivPic);

            // 选中状态
            holder.cbSelect.setOnCheckedChangeListener(null);
            holder.cbSelect.setChecked(selectedIds.contains(item.getId()));
            holder.cbSelect.setOnCheckedChangeListener((v, isChecked) -> {
                if (isChecked) selectedIds.add(item.getId());
                else selectedIds.remove(item.getId());
            });

            // 点击编辑
            holder.itemView.setOnClickListener(v -> showActionDialog(item));
        }

        class VH extends RecyclerView.ViewHolder {
            CheckBox cbSelect;
            ImageView ivPic;
            TextView tvName, tvInfo, tvTime, tvStatus;

            public VH(View v) {
                super(v);
                cbSelect = v.findViewById(R.id.item_cb_select);
                ivPic = v.findViewById(R.id.iv_pet_pic);
                tvName = v.findViewById(R.id.tv_pet_name);
                tvInfo = v.findViewById(R.id.tv_pet_info);
                tvTime = v.findViewById(R.id.tv_publish_time);
                tvStatus = v.findViewById(R.id.tv_status_tag);
            }
        }
    }

    private void showActionDialog(Adopt adopt) {
        new AlertDialog.Builder(mContext)
                .setTitle("操作: " + adopt.getPetName())
                .setItems(new String[]{"删除记录"}, (d, which) -> {
                    if (which == 0) {
                        databaseHelper.deletePet(adopt.getId(), new DataCallback<Boolean>() {
                            @Override public void onSuccess(Boolean data) { loadData(etSearch.getText().toString()); }
                            @Override public void onFail(String msg) { showToast("删除失败"); }
                        });
                    }
                }).show();
        // 如果需要编辑功能，可以再加一个 Item 调用 showEditDialog(adopt)
    }

    // 图片预览 Adapter (用于 Dialog)
    private class DialogImageAdapter extends RecyclerView.Adapter<DialogImageAdapter.VH> {
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView iv = new ImageView(mContext);
            int s = (int) (80 * getResources().getDisplayMetrics().density);
            iv.setLayoutParams(new ViewGroup.MarginLayoutParams(s, s));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new VH(iv);
        }
        @Override public void onBindViewHolder(@NonNull VH holder, int position) {
            Glide.with(mContext).load(dialogSelectedUris.get(position)).into((ImageView) holder.itemView);
            holder.itemView.setOnClickListener(v -> {
                dialogSelectedUris.remove(position);
                notifyDataSetChanged();
            });
        }
        @Override public int getItemCount() { return dialogSelectedUris.size(); }
        class VH extends RecyclerView.ViewHolder { public VH(@NonNull View v) { super(v); } }
    }
}