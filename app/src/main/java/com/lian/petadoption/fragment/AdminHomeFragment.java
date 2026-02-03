package com.lian.petadoption.fragment;

import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.lian.petadoption.R;
import com.lian.petadoption.base.BaseFragment;
import com.lian.petadoption.widget.SimpleLineChartView;

import java.util.List;
import java.util.Map;

public class AdminHomeFragment extends BaseFragment {

    private SimpleLineChartView lineChartView;
    private View cardUser, cardPet, cardApply, cardSuccess;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_admin_home;
    }

    @Override
    protected void initView(View root) {
        lineChartView = root.findViewById(R.id.line_chart);
        initCards(root);
    }

    @Override
    protected void initData() {
        refreshData();
    }

    private void initCards(View root) {
        cardUser = root.findViewById(R.id.card_user);
        setupCard(cardUser, "注册用户", android.R.drawable.ic_menu_myplaces, Color.parseColor("#E3F2FD"), Color.parseColor("#2196F3"));

        cardPet = root.findViewById(R.id.card_pet);
        setupCard(cardPet, "在库宠物", android.R.drawable.ic_menu_gallery, Color.parseColor("#E8F5E9"), Color.parseColor("#4CAF50"));

        cardApply = root.findViewById(R.id.card_apply);
        setupCard(cardApply, "领养申请", android.R.drawable.ic_menu_edit, Color.parseColor("#FFF3E0"), Color.parseColor("#FF9800"));

        cardSuccess = root.findViewById(R.id.card_success);
        setupCard(cardSuccess, "成功领养", android.R.drawable.ic_menu_save, Color.parseColor("#FCE4EC"), Color.parseColor("#E91E63"));
    }

    private void setupCard(View cardView, String label, int iconRes, int bgColor, int tintColor) {
        ImageView icon = cardView.findViewById(R.id.iv_card_icon);
        TextView tvLabel = cardView.findViewById(R.id.tv_card_label);

        icon.setBackgroundColor(bgColor);
        icon.setColorFilter(tintColor);
        icon.setImageResource(iconRes);
        tvLabel.setText(label);
    }

    private void updateCardValue(View cardView, String value) {
        // 确保在主线程更新 UI
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                TextView tvValue = cardView.findViewById(R.id.tv_card_value);
                if (tvValue != null) tvValue.setText(value);
            });
        }
    }

    private void refreshData() {
        // 使用新线程进行数据库统计，避免阻塞 UI
        new Thread(() -> {
            // 1. 获取统计数字
            long userCount = databaseHelper.getUserCount();
            long petCount = databaseHelper.getPetCount();
            long applyCount = databaseHelper.getTotalApplicationCount();
            long successCount = databaseHelper.getSuccessfulAdoptionCount();

            // 2. 获取图表数据
            List<Map<String, Object>> chartData = databaseHelper.getLast7DaysStats();

            // 3. 回到主线程更新 UI
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    updateCardValue(cardUser, String.valueOf(userCount));
                    updateCardValue(cardPet, String.valueOf(petCount));
                    updateCardValue(cardApply, String.valueOf(applyCount));
                    updateCardValue(cardSuccess, String.valueOf(successCount));
                    if (lineChartView != null) {
                        lineChartView.setData(chartData);
                    }
                });
            }
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }
}