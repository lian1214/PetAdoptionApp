package com.lian.petadoption.activity;

import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
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

public class ApplicationActivity extends BaseActivity {

    private Adopt mAdopt;
    private EditText etName, etAge, etIdCard, etPhone, etAddress, etIncomeSource, etIntent;
    private RadioGroup rgGender;
    private CheckBox cbPromise, cbRequest;

    // 图片列表相关
    private RecyclerView rvLivePics, rvIncomePics;
    private ImageAdapter liveAdapter, incomeAdapter;
    private List<Uri> liveUris = new ArrayList<>();
    private List<Uri> incomeUris = new ArrayList<>();

    // 图片选择器助手
    private ImagePickerHelper livePickerHelper;
    private ImagePickerHelper incomePickerHelper;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_application;
    }

    @Override
    protected void initView() {
        setToolbarTitle("填写领养申请");

        etName = findViewById(R.id.et_apply_name);
        etAge = findViewById(R.id.et_apply_age);
        rgGender = findViewById(R.id.rg_apply_gender);
        etIdCard = findViewById(R.id.et_apply_idcard);
        etPhone = findViewById(R.id.et_apply_phone);
        etAddress = findViewById(R.id.et_apply_address);
        etIncomeSource = findViewById(R.id.et_income_source);
        etIntent = findViewById(R.id.et_apply_intent);

        rvLivePics = findViewById(R.id.rv_live_pics);
        rvIncomePics = findViewById(R.id.rv_income_pics);

        cbPromise = findViewById(R.id.cb_promise);
        cbRequest = findViewById(R.id.cb_request);

        setupImageLists();
        setupClickListeners();
    }

    @Override
    protected void initData() {
        mAdopt = (Adopt) getIntent().getSerializableExtra(AppConfig.Extra.ADOPT_DATA);
        if (mAdopt == null) {
            showToast("数据异常");
            finish();
        }
    }

    private void setupImageLists() {
        // 1. 居住环境图片列表
        rvLivePics.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        liveAdapter = new ImageAdapter(this, liveUris);
        rvLivePics.setAdapter(liveAdapter);

        // 2. 收入证明图片列表
        rvIncomePics.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        incomeAdapter = new ImageAdapter(this, incomeUris);
        rvIncomePics.setAdapter(incomeAdapter);

        // 3. 初始化选择器
        livePickerHelper = new ImagePickerHelper(this, 9, uris -> {
            liveUris.addAll(uris);
            liveAdapter.notifyDataSetChanged();
        });

        incomePickerHelper = new ImagePickerHelper(this, 9, uris -> {
            incomeUris.addAll(uris);
            incomeAdapter.notifyDataSetChanged();
        });
    }

    private void setupClickListeners() {
        // 上传按钮
        findViewById(R.id.btn_add_live_pic).setOnClickListener(v -> livePickerHelper.pick());
        findViewById(R.id.btn_add_income_pic).setOnClickListener(v -> incomePickerHelper.pick());

        // 提交按钮
        findViewById(R.id.btn_submit_final).setOnClickListener(v -> handleSubmit());
    }

    private void handleSubmit() {
        if (!cbPromise.isChecked() || !cbRequest.isChecked()) {
            showToast("请先勾选承诺协议");
            return;
        }

        String name = etName.getText().toString().trim();
        String age = etAge.getText().toString().trim();
        String idCard = etIdCard.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String income = etIncomeSource.getText().toString().trim();
        String intent = etIntent.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
            showToast("请填写完整基本信息");
            return;
        }

        String loginAccount = sharedPreferences.getString(AppConfig.SP.USER_ACCOUNT, "");
        String gender = rgGender.getCheckedRadioButtonId() == R.id.rb_male ? "男" : "女";

        // 转换图片路径
        String livePicsStr = urisToString(liveUris);
        String incomePicsStr = urisToString(incomeUris);

        // 异步提交
        databaseHelper.submitApplication(
                mAdopt.getId(),
                mAdopt.getSendName(),
                loginAccount,
                name,
                age,
                gender,
                idCard,
                phone,
                address,
                livePicsStr,
                incomePicsStr,
                income,
                intent,
                new DataCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean data) {
                        showToast("申请已提交，请等待审核");
                        setResult(RESULT_OK);
                        finish();
                    }

                    @Override
                    public void onFail(String msg) {
                        showToast("提交失败: " + msg);
                    }
                }
        );
    }

    private String urisToString(List<Uri> uris) {
        if (uris == null || uris.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Uri uri : uris) {
            sb.append(uri.toString()).append(AppConfig.IMAGE_SPLIT_SYMBOL);
        }
        if (sb.length() > 0) sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    // 简单的图片适配器 (支持长按删除)
    private static class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.VH> {
        private final BaseActivity context;
        private final List<Uri> list;

        public ImageAdapter(BaseActivity context, List<Uri> list) {
            this.context = context;
            this.list = list;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView iv = new ImageView(context);
            int size = (int) (80 * context.getResources().getDisplayMetrics().density);
            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(size, size);
            lp.setMargins(0, 0, 16, 0);
            iv.setLayoutParams(lp);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new VH(iv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Glide.with(context).load(list.get(position)).into((ImageView) holder.itemView);

            holder.itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setMessage("移除这张图片？")
                        .setPositiveButton("移除", (d, w) -> {
                            list.remove(position);
                            notifyDataSetChanged();
                        })
                        .setNegativeButton("取消", null)
                        .show();
                return true;
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            public VH(@NonNull View itemView) { super(itemView); }
        }
    }
}