package com.lian.petadoption.fragment;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lian.petadoption.R;
import com.lian.petadoption.activity.PetDetailActivity;
import com.lian.petadoption.activity.PublishAdoptionActivity;
import com.lian.petadoption.adapter.AdoptListAdapter;
import com.lian.petadoption.base.BaseFragment;
import com.lian.petadoption.config.AppConfig;
import com.lian.petadoption.dao.Adopt;
import com.lian.petadoption.database.DataCallback;

import java.util.ArrayList;
import java.util.List;

public class AdoptionListFragment extends BaseFragment {

    private FloatingActionButton fabAdd;
    private RecyclerView recyclerView;
    private AdoptListAdapter adapter;
    private TextView titleText;
    private EditText searchBar;
    private View searchButton; // 兼容布局中的 TextView/Button

    private ActivityResultLauncher<Intent> publishLauncher;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_adoption_list;
    }

    @Override
    protected void initView(View root) {
        titleText = root.findViewById(R.id.title_text);
        recyclerView = root.findViewById(R.id.recycler_view);
        fabAdd = root.findViewById(R.id.fab_add_pet);
        searchBar = root.findViewById(R.id.search_bar);
        searchButton = root.findViewById(R.id.search_button);

        // 1. 初始化列表 (网格布局，2列)
        recyclerView.setLayoutManager(new GridLayoutManager(mContext, 2));
        adapter = new AdoptListAdapter(mContext);
        recyclerView.setAdapter(adapter);

        // 2. 点击进入详情
        adapter.setOnItemClickListener((adopt, pos) -> {
            Intent intent = new Intent(mContext, PetDetailActivity.class);
            intent.putExtra(AppConfig.Extra.ADOPT_DATA, adopt);
            publishLauncher.launch(intent);
        });

        // 3. 搜索点击
        if (searchButton != null) {
            searchButton.setOnClickListener(v -> {
                String keyword = searchBar.getText().toString().trim();
                performSearch(keyword);
            });
        }

        // 4. 发布按钮点击
        fabAdd.setOnClickListener(v -> showPublishDialog());

        // 5. 初始化结果回调
        initActivityResultLauncher();
    }

    @Override
    protected void initData() {
        performSearch(""); // 默认加载全部
    }

    private void performSearch(String keyword) {
        // 1. 关键字为空，获取所有
        if (TextUtils.isEmpty(keyword)) {
            databaseHelper.getPendingAdopts(new DataCallback<List<Adopt>>() {
                @Override
                public void onSuccess(List<Adopt> data) {
                    updateList(data);
                }

                @Override
                public void onFail(String msg) {
                    showToast(msg);
                }
            });
            return;
        }

        // 2. 如果是纯数字，尝试按 ID 查，如果查不到再按名字模糊查
        if (isNumeric(keyword)) {
            try {
                int id = Integer.parseInt(keyword);
                // 异步查询单只宠物
                databaseHelper.getAdoptById(id, new DataCallback<Adopt>() {
                    @Override
                    public void onSuccess(Adopt adopt) {
                        // 查到了，显示单个
                        List<Adopt> list = new ArrayList<>();
                        list.add(adopt);
                        updateList(list);
                    }

                    @Override
                    public void onFail(String msg) {
                        // ID 没查到，降级为模糊搜索
                        searchFuzzy(keyword);
                    }
                });
            } catch (Exception e) {
                searchFuzzy(keyword);
            }
        } else {
            // 3. 非数字，直接模糊搜索
            searchFuzzy(keyword);
        }
    }

    private void searchFuzzy(String keyword) {
        databaseHelper.searchPets(keyword, new DataCallback<List<Adopt>>() {
            @Override
            public void onSuccess(List<Adopt> data) {
                updateList(data);
                if (data.isEmpty()) showToast("未找到相关宠物");
            }

            @Override
            public void onFail(String msg) {
                showToast(msg);
            }
        });
    }

    private void updateList(List<Adopt> data) {
        if (data == null) data = new ArrayList<>();
        adapter.setData(data);
        if (titleText != null) {
            titleText.setText(String.format("共有 %d 只宠物可领养", data.size()));
        }
    }

    private boolean isNumeric(String str) {
        if (TextUtils.isEmpty(str)) return false;
        return str.matches("\\d+");
    }

    private void initActivityResultLauncher() {
        publishLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        performSearch(""); // 刷新列表
                    }
                }
        );
    }

    private void showPublishDialog() {
        new AlertDialog.Builder(mContext)
                .setTitle("温馨提示")
                .setMessage("发布领养信息请确保内容真实有效。\n让我们一起为毛孩子寻找温暖的家！")
                .setPositiveButton("确认识别", (dialog, which) -> {
                    Intent intent = new Intent(mContext, PublishAdoptionActivity.class);
                    publishLauncher.launch(intent);
                })
                .setNegativeButton("再想想", null)
                .show();
    }
}