package com.lian.petadoption.fragment;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lian.petadoption.R;
import com.lian.petadoption.base.BaseFragment;
import com.lian.petadoption.base.BaseRecyclerAdapter;
import com.lian.petadoption.dao.User; // 需确保有 User 实体类
import com.lian.petadoption.database.DataCallback;
import com.lian.petadoption.utils.GlideUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserManageFragment extends BaseFragment {

    private RecyclerView recyclerView;
    private EditText etSearch;
    private TextView tvBatchDelete;

    private UserManageAdapter adapter;
    private List<User> userList = new ArrayList<>();
    private Set<Integer> selectedIds = new HashSet<>(); // 存储选中的用户 ID

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_user_manage;
    }

    @Override
    protected void initView(View root) {
        etSearch = root.findViewById(R.id.et_search_user);
        tvBatchDelete = root.findViewById(R.id.tv_batch_delete);
        recyclerView = root.findViewById(R.id.user_recycler_view); // XML ID 需对应修改

        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        adapter = new UserManageAdapter(mContext);
        recyclerView.setAdapter(adapter);

        // 添加用户按钮
        root.findViewById(R.id.fab_add_user).setOnClickListener(v -> showCreateUserDialog());

        // 搜索监听
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadData(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // 批量删除
        tvBatchDelete.setOnClickListener(v -> handleBatchDelete());
    }

    @Override
    protected void initData() {
        loadData("");
    }

    private void loadData(String keyword) {
        // 需要在 DatabaseHelper 补充 getAllUsers 和 searchUsers 的异步方法
        // 这里假设已经存在
        DataCallback<List<User>> callback = new DataCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> data) {
                userList.clear();
                userList.addAll(data);
                adapter.setData(userList);
                selectedIds.clear(); // 刷新后清空选中
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFail(String msg) {
                showToast(msg);
            }
        };

        if (TextUtils.isEmpty(keyword)) {
            databaseHelper.getAllUsers(callback);
        } else {
            databaseHelper.searchUsers(keyword, callback);
        }
    }

    private void handleBatchDelete() {
        if (selectedIds.isEmpty()) {
            showToast("请先勾选用户");
            return;
        }
        new AlertDialog.Builder(mContext)
                .setTitle("确认删除")
                .setMessage("删除选中的 " + selectedIds.size() + " 个用户？")
                .setPositiveButton("确定", (d, w) -> {
                    for (Integer id : selectedIds) {
                        databaseHelper.deleteUser(id); // 按ID删除
                    }
                    showToast("删除完成");
                    loadData(etSearch.getText().toString());
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showCreateUserDialog() {
        View dv = LayoutInflater.from(mContext).inflate(R.layout.dialog_create_user, null);
        EditText en = dv.findViewById(R.id.u_et_username);
        EditText ep = dv.findViewById(R.id.u_et_password);

        new AlertDialog.Builder(mContext)
                .setTitle("创建用户")
                .setView(dv)
                .setPositiveButton("确定", (d, w) -> {
                    String name = en.getText().toString().trim();
                    String pwd = ep.getText().toString().trim();
                    if(TextUtils.isEmpty(name) || TextUtils.isEmpty(pwd)) {
                        showToast("请填写完整");
                        return;
                    }
                    databaseHelper.register(name, pwd, new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean data) {
                            showToast("创建成功");
                            loadData("");
                        }
                        @Override
                        public void onFail(String msg) {
                            showToast("创建失败: " + msg);
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 内部 Adapter 类
    private class UserManageAdapter extends BaseRecyclerAdapter<User, UserManageAdapter.VH> {

        public UserManageAdapter(Context context) { super(context); }

        @Override
        protected VH onCreateVH(ViewGroup parent, int viewType) {
            // 请确保 layout/item_user_manage.xml 存在且适配 RecyclerView
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_user_manage_list, parent, false);
            return new VH(view);
        }

        @Override
        protected void onBindVH(VH holder, User user, int position) {
            holder.tvName.setText(user.getUsername());
            GlideUtils.loadCircle(mContext, user.getHead(), holder.ivAvatar);

            boolean isNormal = "1".equals(user.getState()); // 假设 "1" 是正常
            holder.tvStatus.setText(isNormal ? "状态：正常" : "状态：封禁");
            holder.tvStatus.setTextColor(isNormal ? Color.parseColor("#4CAF50") : Color.RED);

            // 选中状态
            holder.cbSelect.setOnCheckedChangeListener(null);
            holder.cbSelect.setChecked(selectedIds.contains(user.getId()));
            holder.cbSelect.setOnCheckedChangeListener((v, isChecked) -> {
                if (isChecked) selectedIds.add(user.getId());
                else selectedIds.remove(user.getId());
            });

            // 长按管理
            holder.itemView.setOnLongClickListener(v -> {
                showActionDialog(user);
                return true;
            });
        }

        class VH extends RecyclerView.ViewHolder {
            CheckBox cbSelect;
            ImageView ivAvatar;
            TextView tvName, tvStatus;

            public VH(View v) {
                super(v);
                cbSelect = v.findViewById(R.id.item_cb_select);
                ivAvatar = v.findViewById(R.id.item_user_avatar);
                tvName = v.findViewById(R.id.item_tv_username);
                tvStatus = v.findViewById(R.id.item_tv_status);
            }
        }
    }

    private void showActionDialog(User user) {
        String[] actions = {"重置密码", user.getState().equals("1") ? "封禁账号" : "解封账号", "删除用户"};
        new AlertDialog.Builder(mContext)
                .setTitle("管理: " + user.getUsername())
                .setItems(actions, (d, which) -> {
                    if (which == 0) { // 重置密码
                        databaseHelper.updateUserInfo(user.getUsername(), user.getUsername(), "123456", null); // 需适配异步
                        showToast("密码重置为 123456");
                    } else if (which == 1) { // 封禁/解封
                        String newState = user.getState().equals("1") ? "0" : "1";
                        databaseHelper.updateUserStatus(user.getId(), newState); // 需实现此方法
                        loadData(etSearch.getText().toString());
                    } else if (which == 2) { // 删除
                        databaseHelper.deleteUser(user.getId());
                        loadData(etSearch.getText().toString());
                    }
                })
                .show();
    }
}