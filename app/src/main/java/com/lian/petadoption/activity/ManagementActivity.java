package com.lian.petadoption.activity;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;
import com.lian.petadoption.R;
import com.lian.petadoption.base.BaseActivity;
import com.lian.petadoption.config.AppConfig;
import com.lian.petadoption.fragment.AdminHomeFragment;
import com.lian.petadoption.fragment.KnowledgeManageFragment;
import com.lian.petadoption.fragment.PetManageFragment;
import com.lian.petadoption.fragment.UserManageFragment;

public class ManagementActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawer;
    private Toolbar toolbar;
    private NavigationView navigationView;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_management;
    }

    @Override
    protected void setStatusBar() {
        // 重写父类方法：侧边栏页面需要全屏透明状态栏，让图片/背景延伸上去
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        // 让内容延伸到状态栏背后
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        // 状态栏设为透明，这样 Toolbar 的黄色背景就能透出来
        window.setStatusBarColor(Color.TRANSPARENT);
    }

    @Override
    protected void initView() {
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(Color.WHITE);

        // --- 核心修改：动态计算状态栏高度并设置 Padding ---
        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }

        // 设置 PaddingTop，保留原有的 Left/Right/Bottom Padding (这里设为0)
        toolbar.setPadding(0, statusBarHeight, 0, 0);

        // 确保高度自适应 (配合 XML 中的 wrap_content)
        // 必须设置 LayoutParams，否则 Padding 加上去后内容会被挤压
        ViewGroup.LayoutParams layoutParams = toolbar.getLayoutParams();
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        toolbar.setLayoutParams(layoutParams);
        // ---------------------------------------

        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        setupNavColors();

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.app_name, R.string.app_name);
        // 设置汉堡菜单图标为白色
        toggle.getDrawerArrowDrawable().setColor(Color.WHITE);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // 监听返回键：侧边栏打开时关闭侧边栏，否则提示退出
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                } else {
                    handleLogout();
                }
            }
        });
    }

    @Override
    protected void initData() {
        // 默认显示系统看板
        replaceFragment(new AdminHomeFragment(), "系统看板");
        navigationView.setCheckedItem(R.id.nav_home);
    }

    private void setupNavColors() {
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        int[] colors = new int[]{
                Color.parseColor("#FFD90E"), // 选中项颜色
                Color.parseColor("#444444")  // 默认颜色
        };
        ColorStateList colorList = new ColorStateList(states, colors);
        navigationView.setItemIconTintList(colorList);
        navigationView.setItemTextColor(colorList);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            replaceFragment(new AdminHomeFragment(), "系统看板");
        } else if (id == R.id.nav_user_manage) {
            replaceFragment(new UserManageFragment(), "用户管理");
        } else if (id == R.id.nav_pet_manage) {
            replaceFragment(new PetManageFragment(), "领养宠物管理");
        } else if (id == R.id.nav_knowledge_manage) {
            replaceFragment(new KnowledgeManageFragment(), "知识库管理");
        } else if (id == R.id.nav_logout) {
            handleLogout();
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void replaceFragment(Fragment fragment, String title) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        transaction.replace(R.id.fag1, fragment);
        transaction.commit();

        if (getSupportActionBar() != null) {
            SpannableString spannableTitle = new SpannableString(title);
            spannableTitle.setSpan(new StyleSpan(Typeface.BOLD), 0, spannableTitle.length(), 0);
            getSupportActionBar().setTitle(spannableTitle);
        }
    }

    private void handleLogout() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("退出确认")
                .setMessage("确定要退出后台管理系统吗？")
                .setPositiveButton("确定", (d, which) -> {
                    // 跳转回登录页
                    navigateTo(LoginActivity.class);
                    finish();
                })
                .setNegativeButton("取消", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#FFD90E"));
    }
}