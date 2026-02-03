package com.lian.petadoption.activity;

import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.lian.petadoption.R;
import com.lian.petadoption.base.BaseActivity;
import com.lian.petadoption.config.AppConfig;
import com.lian.petadoption.dao.ApplyInfo;
import com.lian.petadoption.database.DataCallback;

import java.io.File;

public class ApproveDetailActivity extends BaseActivity {

    private int applyId;
    // UI 控件
    private TextView tvName, tvAge, tvGender, tvIdCard, tvPhone, tvAddress, tvIncome, tvIntent;
    private LinearLayout llLivePics, llIncomePics;
    private View layoutBtns;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_approve_detail;
    }

    @Override
    protected void initView() {
        setToolbarTitle("领养申请审核");

        tvName = findViewById(R.id.tv_app_name);
        tvGender = findViewById(R.id.tv_app_gender);
        tvAge = findViewById(R.id.tv_app_age);
        tvPhone = findViewById(R.id.tv_app_phone);
        tvIdCard = findViewById(R.id.tv_app_idcard);
        tvAddress = findViewById(R.id.tv_app_address);
        tvIncome = findViewById(R.id.tv_app_income);
        tvIntent = findViewById(R.id.tv_app_intent);
        llLivePics = findViewById(R.id.ll_app_live_pics);
        llIncomePics = findViewById(R.id.ll_app_income_pics);
        layoutBtns = findViewById(R.id.layout_buttons_container);

        findViewById(R.id.btn_agree).setOnClickListener(v -> showConfirmDialog(true));
        findViewById(R.id.btn_reject).setOnClickListener(v -> showConfirmDialog(false));
    }

    @Override
    protected void initData() {
        applyId = getIntent().getIntExtra("apply_id", -1);
        if (applyId == -1) {
            showToast("数据传输异常");
            finish();
            return;
        }

        // 使用封装好的异步方法获取详情
        databaseHelper.getApplyDetailById(applyId, new DataCallback<ApplyInfo>() {
            @Override
            public void onSuccess(ApplyInfo info) {
                if (info != null) {
                    fillUI(info);
                }
            }

            @Override
            public void onFail(String msg) {
                showToast(msg);
                finish();
            }
        });
    }

    /**
     * 将实体类数据填充到界面
     */
    private void fillUI(ApplyInfo info) {
        // 使用 ApplyInfo 的 getter 方法
        tvName.setText("姓名：" + (info.getRealName() == null ? "" : info.getRealName()));
        tvAge.setText("年龄：" + info.getAge());
        tvGender.setText("性别：" + info.getGender());
        tvPhone.setText("联系方式：" + info.getPhone());
        tvIdCard.setText("身份证号：" + info.getIdCard());
        tvAddress.setText("详细地址：" + info.getAddress());
        tvIncome.setText("经济来源：" + info.getIncomeSource());
        tvIntent.setText(info.getIntent());

        // 显示图片
        displayPics(info.getLivePics(), llLivePics);
        displayPics(info.getIncomePics(), llIncomePics);

        // 如果状态不是"待审核"，隐藏操作按钮
        if (!AppConfig.State.APPLY_PENDING.equals(info.getState())) {
            layoutBtns.setVisibility(View.GONE);
        }
    }

    /**
     * 将图片路径字符串解析并显示在容器中
     */
    private void displayPics(String paths, LinearLayout container) {
        if (container == null) return;
        container.removeAllViews(); // 清除旧视图

        if (TextUtils.isEmpty(paths)) {
            TextView tvNoPic = new TextView(this);
            tvNoPic.setText("未上传");
            tvNoPic.setTextSize(12);
            tvNoPic.setPadding(10, 10, 10, 10);
            container.addView(tvNoPic);
            return;
        }

        String[] pathArray = paths.split(",");
        for (String path : pathArray) {
            if (TextUtils.isEmpty(path.trim())) continue;

            // 动态创建 ImageView
            ImageView iv = new ImageView(this);
            // 设置 100dp x 100dp
            int size = (int) (100 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(0, 0, 16, 0); // 右边距
            iv.setLayoutParams(params);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setBackgroundColor(0xFFEEEEEE); // 灰色背景占位

            // 智能加载（处理 Uri 或 文件路径）
            Object loadObj = path;
            if (!path.startsWith("content://") && !path.startsWith("file://")) {
                loadObj = new File(path); // 绝对路径转 File
            }

            // 使用 Glide 加载
            Glide.with(this)
                    .load(loadObj)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(iv);

            // 点击查看大图（可扩展）
            iv.setOnClickListener(v -> showToast("点击查看图片"));

            container.addView(iv);
        }
    }

    // 审核对话框
    private void showConfirmDialog(boolean isAgree) {
        String title = isAgree ? "通过申请" : "拒绝申请";
        String msg = isAgree ? "确定要通过该用户的领养申请吗？\n宠物状态将变更为“已领养”。" : "确定要拒绝该申请吗？";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("确定", (d, w) -> {
                    // 调用 DatabaseHelper 的异步审核方法
                    databaseHelper.auditApplication(applyId, isAgree, new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean data) {
                            showToast("操作成功");
                            setResult(RESULT_OK); // 通知上个页面刷新
                            finish();
                        }

                        @Override
                        public void onFail(String msg) {
                            showToast("操作失败: " + msg);
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }
}