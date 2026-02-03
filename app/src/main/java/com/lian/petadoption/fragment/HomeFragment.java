package com.lian.petadoption.fragment;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.lian.petadoption.R;
import com.lian.petadoption.activity.AdoptKnowledgeActivity;
import com.lian.petadoption.activity.AdoptionCheckInActivity;
import com.lian.petadoption.activity.KnowledgeDetailActivity;
import com.lian.petadoption.activity.PetDetailActivity;
import com.lian.petadoption.activity.PetKnowledgeActivity;
import com.lian.petadoption.adapter.KnowledgeAdapter;
import com.lian.petadoption.adapter.RecommendAdapter;
import com.lian.petadoption.base.BaseFragment;
import com.lian.petadoption.config.AppConfig;
import com.lian.petadoption.dao.Adopt;
import com.lian.petadoption.dao.Knowledge;
import com.lian.petadoption.database.DataCallback;

import java.util.Collections;
import java.util.List;

public class HomeFragment extends BaseFragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvRecommend, rvKnowledge;
    private RecommendAdapter recommendAdapter;
    private KnowledgeAdapter knowledgeAdapter;

    // 轮播图相关
    private ViewPager2 bannerViewPager;
    private LinearLayout llIndicator;
    private final Handler bannerHandler = new Handler(Looper.getMainLooper());
    private Runnable bannerRunnable;
    private ImageView[] indicators;
    // 轮播图资源 (建议换成你自己的 Banner 图片)
    private final int[] bannerImages = {R.drawable.tab_icon1_2, R.drawable.app_logo_bf, R.drawable.tab_icon1_1};

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_home;
    }

    @Override
    protected void initView(View root) {
        swipeRefreshLayout = root.findViewById(R.id.swipe_refresh);
        rvRecommend = root.findViewById(R.id.rv_recommend);
        rvKnowledge = root.findViewById(R.id.rv_knowledge_recommend);
        bannerViewPager = root.findViewById(R.id.banner_viewpager);
        llIndicator = root.findViewById(R.id.ll_indicator);

        // 1. 初始化宠物推荐列表 (横向或纵向，根据 Item 布局决定，这里假设是列表)
        rvRecommend.setLayoutManager(new LinearLayoutManager(mContext));
        recommendAdapter = new RecommendAdapter(mContext);
        rvRecommend.setAdapter(recommendAdapter);

        // 2. 初始化知识百科列表
        rvKnowledge.setLayoutManager(new LinearLayoutManager(mContext));
        knowledgeAdapter = new KnowledgeAdapter(mContext);
        rvKnowledge.setAdapter(knowledgeAdapter);

        // --- 点击事件 ---

        // 宠物点击 -> 详情
        recommendAdapter.setOnItemClickListener((adopt, pos) -> {
            Intent intent = new Intent(mContext, PetDetailActivity.class);
            intent.putExtra(AppConfig.Extra.ADOPT_DATA, adopt);
            startActivity(intent);
        });

        // 知识点击 -> 详情 (这里补充了跳转逻辑)
        knowledgeAdapter.setOnItemClickListener((knowledge, pos) -> {
            Intent intent = new Intent(mContext, KnowledgeDetailActivity.class);
            // 假设 KnowledgeDetail 接收 "k_data" 或使用 AppConfig 定义的 Key
            intent.putExtra("k_data", knowledge);
            startActivity(intent);
        });

        // --- 功能入口点击 ---
        root.findViewById(R.id.ll_adopt_knowledge).setOnClickListener(v -> navigateTo(AdoptKnowledgeActivity.class));
        root.findViewById(R.id.ll_pet_knowledge).setOnClickListener(v -> navigateTo(PetKnowledgeActivity.class));
        root.findViewById(R.id.ll_punch_card).setOnClickListener(v -> navigateTo(AdoptionCheckInActivity.class));

        // 下拉刷新
        swipeRefreshLayout.setOnRefreshListener(this::refreshAllData);

        // 初始化轮播图
        initBanner();
    }

    @Override
    protected void initData() {
        // 首次进入自动刷新数据
        refreshAllData();
    }

    /**
     * 刷新页面所有数据
     */
    private void refreshAllData() {
        swipeRefreshLayout.setRefreshing(true);

        // 1. 获取宠物推荐 (待领养状态)
        databaseHelper.getPendingAdopts(new DataCallback<List<Adopt>>() {
            @Override
            public void onSuccess(List<Adopt> data) {
                if (data != null && !data.isEmpty()) {
                    Collections.shuffle(data); // 随机打乱，每次进来看到的都不一样
                    // 只取前 3 条展示
                    recommendAdapter.setData(data.subList(0, Math.min(data.size(), 3)));
                } else {
                    recommendAdapter.setData(null);
                }
                // 这里不关闭 refreshing，等知识库也加载完 (或者简单点，谁后回来谁关)
            }

            @Override
            public void onFail(String msg) {
                showToast("推荐获取失败: " + msg);
            }
        });

        // 2. 获取随机知识推荐 (5条)
        databaseHelper.getRandomMixedKnowledge(5, new DataCallback<List<Knowledge>>() {
            @Override
            public void onSuccess(List<Knowledge> data) {
                knowledgeAdapter.setData(data);
                swipeRefreshLayout.setRefreshing(false); // 加载完成
            }

            @Override
            public void onFail(String msg) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    // ================== 轮播图逻辑 (保持原样) ==================

    private void initBanner() {
        if (mContext == null) return;

        bannerViewPager.setAdapter(new RecyclerView.Adapter<BannerViewHolder>() {
            @NonNull
            @Override
            public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                ImageView imageView = new ImageView(parent.getContext());
                imageView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                return new BannerViewHolder(imageView);
            }

            @Override
            public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
                holder.imageView.setImageResource(bannerImages[position]);
            }

            @Override
            public int getItemCount() {
                return bannerImages.length;
            }
        });

        initIndicators();

        bannerViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateIndicators(position);
            }
        });

        bannerRunnable = new Runnable() {
            @Override
            public void run() {
                if (bannerViewPager != null && bannerImages.length > 0) {
                    int next = (bannerViewPager.getCurrentItem() + 1) % bannerImages.length;
                    bannerViewPager.setCurrentItem(next, true);
                    bannerHandler.postDelayed(this, 3000);
                }
            }
        };
    }

    private void initIndicators() {
        if (mContext == null) return;
        indicators = new ImageView[bannerImages.length];
        llIndicator.removeAllViews();

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(20, 20); //稍微调大一点点点
        params.setMargins(10, 0, 10, 0);

        for (int i = 0; i < bannerImages.length; i++) {
            indicators[i] = new ImageView(mContext);
            // 这里使用了你之前的 drawable 资源，确保资源存在
            indicators[i].setBackgroundResource(i == 0 ? R.drawable.shape_dot_selected : R.drawable.shape_dot_normal);
            indicators[i].setLayoutParams(params);
            llIndicator.addView(indicators[i]);
        }
    }

    private void updateIndicators(int position) {
        if (indicators == null) return;
        for (int i = 0; i < indicators.length; i++) {
            indicators[i].setBackgroundResource(i == position ? R.drawable.shape_dot_selected : R.drawable.shape_dot_normal);
        }
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        BannerViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 页面可见时开始轮播
        bannerHandler.postDelayed(bannerRunnable, 3000);
    }

    @Override
    public void onPause() {
        super.onPause();
        // 页面不可见时停止轮播，省电
        bannerHandler.removeCallbacks(bannerRunnable);
    }
}