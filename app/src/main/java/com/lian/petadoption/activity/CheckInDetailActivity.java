package com.lian.petadoption.activity;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.lian.petadoption.R;
import com.lian.petadoption.base.BaseActivity;
import com.lian.petadoption.base.BaseRecyclerAdapter;
import com.lian.petadoption.config.AppConfig;
import com.lian.petadoption.dao.CheckIn;
import com.lian.petadoption.dao.Comment;
import com.lian.petadoption.database.DataCallback;
import com.lian.petadoption.utils.GlideUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CheckInDetailActivity extends BaseActivity {

    private CheckIn punch;
    private String currentUserName;

    // UI 控件
    private TextView tvLikeCount, tvImageIndex;
    private ImageView ivLike, ivAvatar;
    private RecyclerView rvComments;
    private EditText etComment;
    private View flBannerContainer;
    private ViewPager2 vpImageBanner;

    private CommentAdapter commentAdapter;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_check_in_detail;
    }

    @Override
    protected void initView() {
        setToolbarTitle("打卡详情");

        tvLikeCount = findViewById(R.id.tv_like_count);
        ivLike = findViewById(R.id.iv_like);
        vpImageBanner = findViewById(R.id.vp_image_banner);
        tvImageIndex = findViewById(R.id.tv_image_index);
        ivAvatar = findViewById(R.id.iv_detail_avatar);
        flBannerContainer = findViewById(R.id.fl_banner_container);
        etComment = findViewById(R.id.et_comment);
        rvComments = findViewById(R.id.rv_comments);

        // 初始化评论列表
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter(this);
        rvComments.setAdapter(commentAdapter);

        // 点击事件
        findViewById(R.id.ll_like_btn).setOnClickListener(v -> toggleLike());
        findViewById(R.id.btn_send_comment).setOnClickListener(v -> sendComment());
    }

    @Override
    protected void initData() {
        currentUserName = sharedPreferences.getString(AppConfig.SP.USER_ACCOUNT, "");
        punch = (CheckIn) getIntent().getSerializableExtra("punch_data");

        if (punch == null) {
            showToast("数据异常");
            finish();
            return;
        }

        bindDataToUI();
        loadComments();
    }

    private void bindDataToUI() {
        setText(R.id.tv_detail_content, punch.getContent());
        setText(R.id.tv_detail_time, punch.getTime());

        // 获取发布者信息
        Map<String, String> userInfo = databaseHelper.getUserInfo(punch.getUsername());
        String nickname = punch.getUsername();
        String avatarPath = "";

        if (userInfo != null) {
            String nick = userInfo.get("u_nickname");
            if (!TextUtils.isEmpty(nick)) nickname = nick;
            avatarPath = userInfo.get("u_head");
        }

        setText(R.id.tv_detail_username, nickname);
        GlideUtils.loadCircle(this, avatarPath, ivAvatar);

        setupImageBanner();
        refreshLikeUI();
    }

    private void setupImageBanner() {
        String picPaths = punch.getPic();
        if (!TextUtils.isEmpty(picPaths)) {
            flBannerContainer.setVisibility(View.VISIBLE);
            List<String> imageList = new ArrayList<>();
            if (picPaths.contains(",")) {
                imageList.addAll(Arrays.asList(picPaths.split(",")));
            } else {
                imageList.add(picPaths);
            }

            ImageBannerAdapter bannerAdapter = new ImageBannerAdapter(imageList);
            vpImageBanner.setAdapter(bannerAdapter);

            if (imageList.size() > 1) {
                tvImageIndex.setVisibility(View.VISIBLE);
                tvImageIndex.setText("1 / " + imageList.size());
                vpImageBanner.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        tvImageIndex.setText((position + 1) + " / " + imageList.size());
                    }
                });
            } else {
                tvImageIndex.setVisibility(View.GONE);
            }
        } else {
            flBannerContainer.setVisibility(View.GONE);
        }
    }

    private void refreshLikeUI() {
        // 同步获取点赞数和状态
        int count = databaseHelper.getCheckInLikeCount(punch.getId());
        boolean isLiked = databaseHelper.isCheckInLiked(punch.getId(), currentUserName);

        tvLikeCount.setText(String.valueOf(count));
        ivLike.setImageResource(isLiked ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
        if (isLiked) ivLike.setColorFilter(Color.parseColor("#FFD90E"));
        else ivLike.clearColorFilter();
    }

    private void toggleLike() {
        if (TextUtils.isEmpty(currentUserName)) {
            showToast("请先登录");
            return;
        }
        databaseHelper.toggleCheckInLike(punch.getId(), currentUserName, new DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                refreshLikeUI();
            }
            @Override
            public void onFail(String msg) {}
        });
    }

    private void loadComments() {
        // 异步获取评论
        databaseHelper.getCheckInComments(punch.getId(), new DataCallback<List<Comment>>() {
            @Override
            public void onSuccess(List<Comment> data) {
                commentAdapter.setData(data);
            }
            @Override
            public void onFail(String msg) {
                // 失败可不处理或 toast
            }
        });
    }

    private void sendComment() {
        String content = etComment.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            showToast("请输入评论内容");
            return;
        }
        if (TextUtils.isEmpty(currentUserName)) {
            showToast("请先登录");
            return;
        }

        // 异步提交评论
        databaseHelper.addCheckInComment(currentUserName, punch.getId(), content, new DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                showToast("评论成功");
                etComment.setText("");
                loadComments(); // 刷新列表
            }

            @Override
            public void onFail(String msg) {
                showToast("评论失败: " + msg);
            }
        });
    }

    private void setText(int id, String text) {
        TextView tv = findViewById(id);
        if (tv != null) tv.setText(text == null ? "" : text);
    }

    // --- 内部适配器类 ---

    // 1. 图片轮播适配器
    private class ImageBannerAdapter extends RecyclerView.Adapter<ImageBannerAdapter.VH> {
        private List<String> list;
        public ImageBannerAdapter(List<String> list) { this.list = list; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView iv = new ImageView(parent.getContext());
            iv.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new VH(iv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            GlideUtils.load(CheckInDetailActivity.this, list.get(position), (ImageView)holder.itemView);
        }

        @Override public int getItemCount() { return list.size(); }
        class VH extends RecyclerView.ViewHolder { public VH(View v) { super(v); } }
    }

    // 2. 评论列表适配器 (基于 BaseRecyclerAdapter)
    private class CommentAdapter extends BaseRecyclerAdapter<Comment, CommentAdapter.VH> {
        public CommentAdapter(android.content.Context context) { super(context); }

        @Override
        protected VH onCreateVH(ViewGroup parent, int viewType) {
            // 确保你的 layout 目录下有 item_comment.xml
            View view = mInflater.inflate(R.layout.item_comment, parent, false);
            return new VH(view);
        }

        @Override
        protected void onBindVH(VH holder, Comment data, int position) {
            // DatabaseHelper 已经在查询时联表获取了 nickname 和 avatar
            holder.tvName.setText(data.getNickname());
            holder.tvContent.setText(data.getContent());
            holder.tvTime.setText(data.getTime());
            GlideUtils.loadCircle(mContext, data.getAvatar(), holder.ivAvatar);
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvContent, tvTime;
            ImageView ivAvatar;

            public VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tv_comment_name);
                tvContent = v.findViewById(R.id.tv_comment_content);
                tvTime = v.findViewById(R.id.tv_comment_time);
                ivAvatar = v.findViewById(R.id.iv_comment_avatar);
            }
        }
    }
}