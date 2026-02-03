package com.lian.petadoption.activity;

import android.text.TextUtils;
import android.view.View;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.lian.petadoption.R;
import com.lian.petadoption.base.BaseActivity;
import com.lian.petadoption.config.AppConfig;
import com.lian.petadoption.database.DataCallback;
import com.lian.petadoption.fragment.AdoptionListFragment;
import com.lian.petadoption.fragment.HomeFragment;
import com.lian.petadoption.fragment.MessageFragment;
import com.lian.petadoption.fragment.MineFragment;

public class TabBarActivity extends BaseActivity implements View.OnClickListener {

    private Button btnHome, btnAdoption, btnMessage, btnMine;
    private View msgBadge;

    private HomeFragment homeFragment;
    private AdoptionListFragment adoptionFragment;
    private MessageFragment messageFragment;
    private MineFragment mineFragment;
    private Fragment currentFragment;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_tab_bar;
    }

    @Override
    protected void initView() {
        btnHome = findViewById(R.id.btn_tab_home);
        btnAdoption = findViewById(R.id.btn_tab_adoption);
        btnMessage = findViewById(R.id.btn_tab_message);
        btnMine = findViewById(R.id.btn_tab_mine);
        msgBadge = findViewById(R.id.view_msg_badge);

        btnHome.setOnClickListener(this);
        btnAdoption.setOnClickListener(this);
        btnMessage.setOnClickListener(this);
        btnMine.setOnClickListener(this);
    }

    @Override
    protected void initData() {
        initFragments();
    }

    private void initFragments() {
        homeFragment = new HomeFragment();
        adoptionFragment = new AdoptionListFragment();
        messageFragment = new MessageFragment();
        mineFragment = new MineFragment();

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fl_container, homeFragment)
                .commit();

        currentFragment = homeFragment;
        updateTabState(btnHome);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUnreadMessages();
    }

    // 供 Fragment 调用以刷新红点 (例如 MessageFragment 标记已读后)
    public void updateMessageBadge() {
        checkUnreadMessages();
    }

    private void checkUnreadMessages() {
        String username = sharedPreferences.getString(AppConfig.SP.USER_ACCOUNT, "");
        if (TextUtils.isEmpty(username)) return;

        // 使用异步回调
        databaseHelper.getUnreadMessageCount(username, new DataCallback<Integer>() {
            @Override
            public void onSuccess(Integer count) {
                if (msgBadge != null) {
                    msgBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFail(String msg) {
                // 忽略错误，仅仅是不显示红点
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_tab_home) {
            switchFragment(homeFragment);
            updateTabState(btnHome);
        } else if (id == R.id.btn_tab_adoption) {
            switchFragment(adoptionFragment);
            updateTabState(btnAdoption);
        } else if (id == R.id.btn_tab_message) {
            switchFragment(messageFragment);
            updateTabState(btnMessage);
        } else if (id == R.id.btn_tab_mine) {
            switchFragment(mineFragment);
            updateTabState(btnMine);
        }
    }

    private void switchFragment(Fragment target) {
        if (currentFragment == target) return;
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (currentFragment != null) transaction.hide(currentFragment);
        if (!target.isAdded()) transaction.add(R.id.fl_container, target);
        else transaction.show(target);
        transaction.commit();
        currentFragment = target;
    }

    private void updateTabState(Button selected) {
        btnHome.setSelected(false);
        btnAdoption.setSelected(false);
        btnMessage.setSelected(false);
        btnMine.setSelected(false);
        selected.setSelected(true);
    }
}