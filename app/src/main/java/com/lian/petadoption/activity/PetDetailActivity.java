package com.lian.petadoption.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.lian.petadoption.R;
import com.lian.petadoption.base.BaseActivity;
import com.lian.petadoption.config.AppConfig;
import com.lian.petadoption.dao.Adopt;
import com.lian.petadoption.database.DataCallback;
import com.lian.petadoption.utils.GlideUtils;

import java.util.List;
import java.util.Map;

public class PetDetailActivity extends BaseActivity {

    private Adopt mAdopt;
    private ImageButton btnFav;
    private Button btnApply;
    private ViewPager2 vpPetImages;
    private TextView tvImageIndex;
    private boolean isFav = false; // 当前收藏状态

    @Override
    protected int getLayoutId() {
        return R.layout.activity_pet_detail;
    }

    /**
     * 重写状态栏设置，实现全屏沉浸式（图片顶到状态栏后面）
     */
    @Override
    protected void setStatusBar() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        // 如果想要完全透明且去黑边，可以使用如下代码：
        // Window window = getWindow();
        // window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        // window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        // window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        // window.setStatusBarColor(Color.TRANSPARENT);
    }

    @Override
    protected void initView() {
        btnFav = findViewById(R.id.btn_fav);
        btnApply = findViewById(R.id.btn_apply);
        vpPetImages = findViewById(R.id.vp_pet_images);
        tvImageIndex = findViewById(R.id.tv_image_index);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        // 收藏按钮点击
        btnFav.setOnClickListener(v -> toggleFavorite());

        // 申请按钮点击
        btnApply.setOnClickListener(v -> handleApply());
    }

    @Override
    protected void initData() {
        // 1. 先获取 Intent 传递的数据 (快速显示)
        Adopt intentAdopt = (Adopt) getIntent().getSerializableExtra(AppConfig.Extra.ADOPT_DATA);
        if (intentAdopt != null) {
            mAdopt = intentAdopt;
            bindDataToUI(); // 立即渲染界面
            checkFavoriteStatus(); // 检查收藏状态

            // 2. 异步获取最新数据 (防止列表页数据陈旧)
            databaseHelper.getAdoptById(intentAdopt.getId(), new DataCallback<Adopt>() {
                @Override
                public void onSuccess(Adopt data) {
                    if (data != null) {
                        mAdopt = data;
                        bindDataToUI(); // 刷新界面
                    }
                }

                @Override
                public void onFail(String msg) {
                    // 获取失败则保持 Intent 数据不变
                }
            });
        } else {
            showToast("数据加载失败");
            finish();
        }
    }

    private void bindDataToUI() {
        if (mAdopt == null) return;

        setText(R.id.tv_detail_name, mAdopt.getPetName());
        setText(R.id.tv_detail_id, "编号: " + mAdopt.getId());
        setText(R.id.tv_detail_breed, mAdopt.getBreed());
        setText(R.id.tv_detail_gender, mAdopt.getGender());
        setText(R.id.tv_detail_age, mAdopt.getAge());
        setText(R.id.tv_detail_address, mAdopt.getAddress());
        setText(R.id.tv_detail_remark, mAdopt.getRemark());

        // 周期
        TextView tvCycle = findViewById(R.id.tv_detail_cycle);
        tvCycle.setText(TextUtils.isEmpty(mAdopt.getCycle()) ? "暂无要求" : "每 " + mAdopt.getCycle() + " 打卡一次");

        // 医疗信息
        setText(R.id.tv_detail_dew, mAdopt.getDeworming());
        setText(R.id.tv_detail_ste, mAdopt.getSterilization());
        setText(R.id.tv_detail_vac, mAdopt.getVaccine());

        // 图片轮播
        setupImageBanner();

        // 发布者信息
        setupPublisherInfo();

        // 按钮状态控制
        checkIdentityAndStatus();
    }

    private void setupImageBanner() {
        String picStr = mAdopt.getPic();
        if (TextUtils.isEmpty(picStr)) {
            tvImageIndex.setVisibility(View.GONE);
            return;
        }

        // 兼容处理
        String[] images = picStr.contains(AppConfig.IMAGE_SPLIT_SYMBOL)
                ? picStr.split(AppConfig.IMAGE_SPLIT_SYMBOL)
                : picStr.split(",");

        ImageBannerAdapter bannerAdapter = new ImageBannerAdapter(images);
        vpPetImages.setAdapter(bannerAdapter);

        tvImageIndex.setText("1/" + images.length);
        vpPetImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                tvImageIndex.setText((position + 1) + "/" + images.length);
            }
        });
    }

    private void setupPublisherInfo() {
        String publisherAccount = mAdopt.getSendName();
        if (TextUtils.isEmpty(publisherAccount)) publisherAccount = "未知用户";

        setText(R.id.tv_publisher_name, publisherAccount); // 默认显示账号
        setText(R.id.tv_publisher_contact, "联系电话: " + mAdopt.getPhone());

        // 尝试获取发布者详细信息 (头像/昵称)
        // 注意：DatabaseHelper 需要有 getUserInfo 且最好是异步的，这里沿用你之前的同步逻辑或放在子线程
        // 为保持 BaseActivity 风格，这里简单处理，实际建议 DatabaseHelper 增加 getPublisherInfoAsync
        final String finalAccount = publisherAccount;
        new Thread(() -> {
            Map<String, String> info = databaseHelper.getUserInfo(finalAccount);
            if (info != null) {
                runOnUiThread(() -> {
                    String nick = info.get("u_nickname");
                    String head = info.get("u_head");
                    if (!TextUtils.isEmpty(nick)) setText(R.id.tv_publisher_name, nick);
                    GlideUtils.loadCircle(this, head, findViewById(R.id.iv_publisher_head));
                });
            }
        }).start();
    }

    private void checkIdentityAndStatus() {
        String currentAccount = sharedPreferences.getString(AppConfig.SP.USER_ACCOUNT, "");
        String petState = mAdopt.getState();

        if (AppConfig.State.PET_ADOPTED.equals(petState)) {
            btnApply.setText("已被领养");
            btnApply.setEnabled(false);
            btnApply.setBackgroundColor(Color.LTGRAY);
        } else if (currentAccount.equals(mAdopt.getSendName())) {
            btnApply.setText("这是您发布的宠物");
            btnApply.setEnabled(false);
            btnApply.setBackgroundColor(Color.LTGRAY);
        } else {
            btnApply.setText("申请领养");
            btnApply.setEnabled(true);
            // btnApply.setBackgroundTintList(...) // 如果需要恢复颜色
        }
    }

    private void checkFavoriteStatus() {
        String currentAccount = sharedPreferences.getString(AppConfig.SP.USER_ACCOUNT, "");
        if (TextUtils.isEmpty(currentAccount)) return;

        databaseHelper.getMyFavoriteList(currentAccount, new DataCallback<List<Adopt>>() {
            @Override
            public void onSuccess(List<Adopt> data) {
                isFav = false;
                for (Adopt a : data) {
                    if (a.getId() == mAdopt.getId()) {
                        isFav = true;
                        break;
                    }
                }
                updateFavIcon(isFav);
            }

            @Override
            public void onFail(String msg) {
            }
        });
    }

    private void toggleFavorite() {
        String currentAccount = sharedPreferences.getString(AppConfig.SP.USER_ACCOUNT, "");
        if (TextUtils.isEmpty(currentAccount)) {
            showToast("请先登录");
            return;
        }

        databaseHelper.toggleFavorite(currentAccount, mAdopt.getId(), new DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isNowFavorite) {
                isFav = isNowFavorite;
                updateFavIcon(isFav);
                showToast(isFav ? "收藏成功" : "已取消收藏");
            }

            @Override
            public void onFail(String msg) {
                showToast("操作失败");
            }
        });
    }

    private void updateFavIcon(boolean isFavorite) {
        if (isFavorite) {
            btnFav.setImageResource(android.R.drawable.btn_star_big_on);
            btnFav.setColorFilter(Color.parseColor("#FFD90E"));
        } else {
            btnFav.setImageResource(android.R.drawable.btn_star_big_off);
            btnFav.setColorFilter(Color.parseColor("#A4B0BE"));
        }
    }

    private void handleApply() {
        // 使用统一的 Key 获取账号
        String currentAccount = sharedPreferences.getString(AppConfig.SP.USER_ACCOUNT, "");

        if (TextUtils.isEmpty(currentAccount)) {
            showToast("请先登录后再申请");
            return;
        }

        // 跳转到申请填写页
        Intent intent = new Intent(this, ApplicationActivity.class);
        intent.putExtra(AppConfig.Extra.ADOPT_DATA, mAdopt);
        startActivity(intent);
    }

    /**
     * 辅助设置文本的方法，防止空指针
     */
    private void setText(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null) {
            tv.setText(text == null ? "" : text);
        }
    }

    // 图片轮播适配器
    private class ImageBannerAdapter extends RecyclerView.Adapter<ImageBannerAdapter.BannerViewHolder> {
        private String[] imagePaths;

        public ImageBannerAdapter(String[] imagePaths) {
            this.imagePaths = imagePaths;
        }

        @NonNull
        @Override
        public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new BannerViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
            // 这里直接使用 Glide 加载，确保图片能显示
            com.bumptech.glide.Glide.with(PetDetailActivity.this)
                    .load(imagePaths[position])
                    .placeholder(R.drawable.tab_icon1_2) // 占位图
                    .into((ImageView) holder.itemView);
        }

        @Override
        public int getItemCount() {
            return imagePaths.length;
        }

        class BannerViewHolder extends RecyclerView.ViewHolder {
            public BannerViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}