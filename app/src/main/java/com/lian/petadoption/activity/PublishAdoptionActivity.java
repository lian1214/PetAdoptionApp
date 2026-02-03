package com.lian.petadoption.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.lian.petadoption.R;
import com.lian.petadoption.base.BaseActivity;
import com.lian.petadoption.config.AppConfig;
import com.lian.petadoption.dao.Adopt;
import com.lian.petadoption.database.DataCallback;
import com.lian.petadoption.utils.ImagePickerHelper;

import java.util.ArrayList;
import java.util.List;

public class PublishAdoptionActivity extends BaseActivity {

    private EditText etName, etBreed, etAge, etRemark, etAddress, etCycle, etPhone;
    private RadioGroup rgGender, rgSterilization, rgVaccine, rgDeworming;
    private RecyclerView rvSelectedImages;

    private ImageAdapter imageAdapter;
    private List<Uri> mSelectedUris = new ArrayList<>();
    private ImagePickerHelper imagePickerHelper;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_publish_adoption;
    }

    @Override
    protected void initView() {
        setToolbarTitle("发布送养信息");

        etName = findViewById(R.id.et_name);
        etBreed = findViewById(R.id.et_breed);
        etAge = findViewById(R.id.et_age);
        etRemark = findViewById(R.id.et_remark);
        etAddress = findViewById(R.id.et_address);
        etCycle = findViewById(R.id.et_cycle);
        etPhone = findViewById(R.id.et_phone);

        rgGender = findViewById(R.id.rg_gender);
        rgSterilization = findViewById(R.id.rg_sterilization);
        rgVaccine = findViewById(R.id.rg_vaccine);
        rgDeworming = findViewById(R.id.rg_deworming);

        // 初始化图片列表
        rvSelectedImages = findViewById(R.id.rv_selected_images);
        rvSelectedImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageAdapter = new ImageAdapter(this, mSelectedUris);
        rvSelectedImages.setAdapter(imageAdapter);

        // --- 初始化 ImagePickerHelper ---
        imagePickerHelper = new ImagePickerHelper(this, 9, uris -> {
            mSelectedUris.addAll(uris);
            imageAdapter.notifyDataSetChanged();
            if (!mSelectedUris.isEmpty()) {
                rvSelectedImages.smoothScrollToPosition(mSelectedUris.size() - 1);
            }
        });

        findViewById(R.id.iv_add_pic_container).setOnClickListener(v -> imagePickerHelper.pick());
        findViewById(R.id.btn_publish).setOnClickListener(v -> handlePublish());
        findViewById(R.id.iv_cycle_help).setOnClickListener(v -> showHelpDialog());
    }

    @Override
    protected void initData() {
    }

    private void handlePublish() {
        String name = etName.getText().toString().trim();
        String phoneInput = etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            showToast("请输入宠物昵称");
            return;
        }
        if (TextUtils.isEmpty(phoneInput)) {
            showToast("请输入联系电话");
            return;
        }
        if (mSelectedUris.isEmpty()) {
            showToast("请至少上传一张宠物图片");
            return;
        }

        Adopt adopt = new Adopt();
        adopt.setPetName(name);
        adopt.setPhone(phoneInput);
        adopt.setBreed(etBreed.getText().toString().trim());
        adopt.setAge(etAge.getText().toString().trim());
        adopt.setAddress(etAddress.getText().toString().trim());
        adopt.setCycle(etCycle.getText().toString().trim());
        adopt.setRemark(etRemark.getText().toString().trim());

        adopt.setGender(getRgStatus(rgGender));
        adopt.setSterilization(getRgStatus(rgSterilization));
        adopt.setVaccine(getRgStatus(rgVaccine));
        adopt.setDeworming(getRgStatus(rgDeworming));

        // 拼接图片路径
        StringBuilder sb = new StringBuilder();
        for (Uri uri : mSelectedUris) {
            sb.append(uri.toString()).append(AppConfig.IMAGE_SPLIT_SYMBOL);
        }
        if (sb.length() > 0) sb.setLength(sb.length() - 1);
        adopt.setPic(sb.toString());

        String currentAccount = sharedPreferences.getString(AppConfig.SP.USER_ACCOUNT, "");
        adopt.setSendName(currentAccount);

        databaseHelper.publishPet(adopt, new DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                showToast("发布成功！");
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onFail(String msg) {
                showToast("发布失败：" + msg);
            }
        });
    }

    private String getRgStatus(RadioGroup rg) {
        int id = rg.getCheckedRadioButtonId();
        if (id == -1) return "未知";
        return ((RadioButton) findViewById(id)).getText().toString();
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(this)
                .setTitle("打卡周期")
                .setMessage("指领养后定期上传宠物近况的频率，例如每周一次。")
                .setPositiveButton("确定", null)
                .show();
    }

    private static class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.VH> {
        private Context context;
        private List<Uri> uriList;

        public ImageAdapter(Context context, List<Uri> uriList) {
            this.context = context;
            this.uriList = uriList;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(context);
            int size = (int) (100 * context.getResources().getDisplayMetrics().density);
            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(size, size);
            lp.setMargins(0, 0, 20, 0);
            imageView.setLayoutParams(lp);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new VH(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Uri uri = uriList.get(position);
            Glide.with(context).load(uri).into((ImageView) holder.itemView);

            holder.itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("移除图片")
                        .setMessage("确定要移除这张图片吗？")
                        .setPositiveButton("移除", (dialog, which) -> {
                            uriList.remove(position);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, uriList.size());
                        })
                        .setNegativeButton("取消", null).show();
                return true;
            });
        }

        @Override
        public int getItemCount() { return uriList.size(); }

        static class VH extends RecyclerView.ViewHolder {
            public VH(@NonNull View itemView) { super(itemView); }
        }
    }
}