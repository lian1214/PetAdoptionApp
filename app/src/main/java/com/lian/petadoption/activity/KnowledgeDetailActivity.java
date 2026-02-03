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

import com.bumptech.glide.Glide;
import com.lian.petadoption.R;
import com.lian.petadoption.base.BaseActivity;
import com.lian.petadoption.base.BaseRecyclerAdapter;
import com.lian.petadoption.config.AppConfig;
import com.lian.petadoption.dao.Comment;
import com.lian.petadoption.dao.Knowledge;
import com.lian.petadoption.database.DataCallback;
import com.lian.petadoption.utils.GlideUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class KnowledgeDetailActivity extends BaseActivity {

    private Knowledge knowledge;
    private String currentUserName;

    // UI 控件
    private TextView tvLikeCount, tvImageIndex;
    private ImageView ivLike;
    private RecyclerView rvComments;
    private EditText etComment;
    private View flBannerContainer;
    private ViewPager2 vpImageBanner;

    private CommentAdapter commentAdapter;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_knowledge_detail;
    }

    @Override
    protected void initView() {
        setToolbarTitle("知识详情");

        tvLikeCount = findViewById(R.id.tv_like_count);
        ivLike = findViewById(R.id.iv_like);
        vpImageBanner = findViewById(R.id.vp_image_banner);
        tvImageIndex = findViewById(R.id.tv_image_index);
        flBannerContainer = findViewById(R.id.fl_banner_container);
        etComment = findViewById(R.id.et_comment);
        rvComments = findViewById(R.id.rv_comments);

        // 初始化评论列表
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter(this);
        rvComments.setAdapter(commentAdapter);

        // 点击事件
        findViewById(R.id.ll_like).setOnClickListener(v -> toggleLike());
        findViewById(R.id.btn_send_comment).setOnClickListener(v -> sendComment());
    }

    @Override
    protected void initData() {
        currentUserName = sharedPreferences.getString(AppConfig.SP.USER_ACCOUNT, "");
        knowledge = (Knowledge) getIntent().getSerializableExtra(AppConfig.Extra.KNOWLEDGE_DATA);

        if (knowledge == null) {
            showToast("数据异常");
            finish();
            return;
        }

        bindDataToUI();
        loadComments();
    }

    private void bindDataToUI() {
        setText(R.id.tv_knowledge_title, knowledge.getTitle());
        setText(R.id.tv_knowledge_content, knowledge.getContent());
        setText(R.id.tv_author_info, "发布者：" + knowledge.getUsername() + "  |  " + knowledge.getTime());

        // 初始化图片轮播
        setupImageBanner();

        // 刷新点赞状态
        refreshLikeUI();
    }

    private void setupImageBanner() {
        String pics = knowledge.getPics();
        if (!TextUtils.isEmpty(pics)) {
            flBannerContainer.setVisibility(View.VISIBLE);
            // 兼容逗号分割
            List<String> imgList = new ArrayList<>();
            if (pics.contains(",")) {
                imgList.addAll(Arrays.asList(pics.split(",")));
            } else {
                imgList.add(pics);
            }

            ImageBannerAdapter bannerAdapter = new ImageBannerAdapter(imgList);
            vpImageBanner.setAdapter(bannerAdapter);

            if (imgList.size() > 1) {
                tvImageIndex.setVisibility(View.VISIBLE);
                tvImageIndex.setText("1 / " + imgList.size());
                vpImageBanner.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        tvImageIndex.setText((position + 1) + " / " + imgList.size());
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
        int count = databaseHelper.getKnowledgeLikeCount(knowledge.getId());
        boolean isLiked = databaseHelper.isKnowledgeLiked(knowledge.getId(), currentUserName);

        tvLikeCount.setText(String.valueOf(count));

        // 【修改点】使用 Selector 切换图片
        ivLike.setSelected(isLiked);

        // 更新文字颜色 (假设您的选中图是亮色，未选中是深色)
        if (isLiked) {
            tvLikeCount.setTextColor(Color.parseColor("#FFD90E"));
            ivLike.clearColorFilter();
        } else {
            tvLikeCount.setTextColor(Color.parseColor("#333333"));
            ivLike.clearColorFilter();
        }
    }

    private void toggleLike() {
        if (TextUtils.isEmpty(currentUserName)) {
            showToast("请先登录");
            return;
        }
        // 异步点赞
        databaseHelper.toggleKnowledgeLike(knowledge.getId(), currentUserName, new DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                refreshLikeUI();
            }
            @Override
            public void onFail(String msg) {}
        });
    }

    private void loadComments() {
        // 异步加载评论
        databaseHelper.getKnowledgeComments(knowledge.getId(), new DataCallback<List<Comment>>() {
            @Override
            public void onSuccess(List<Comment> data) {
                commentAdapter.setData(data);
            }
            @Override
            public void onFail(String msg) {}
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

        // 异步发送评论
        databaseHelper.addKnowledgeComment(knowledge.getId(), currentUserName, content, new DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                showToast("评论成功");
                etComment.setText("");
                loadComments();
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

    // --- 内部适配器 ---

    // 1. 图片轮播适配器
    private class ImageBannerAdapter extends RecyclerView.Adapter<ImageBannerAdapter.VH> {
        private List<String> list;
        public ImageBannerAdapter(List<String> list) { this.list = list; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView iv = new ImageView(parent.getContext());
            iv.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new VH(iv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            GlideUtils.load(KnowledgeDetailActivity.this, list.get(position), (ImageView) holder.itemView);
        }

        @Override public int getItemCount() { return list.size(); }
        class VH extends RecyclerView.ViewHolder { public VH(View v) { super(v); } }
    }

    // 2. 评论适配器
    private class CommentAdapter extends BaseRecyclerAdapter<Comment, CommentAdapter.VH> {
        public CommentAdapter(android.content.Context context) { super(context); }

        @Override
        protected VH onCreateVH(ViewGroup parent, int viewType) {
            // 复用之前的 item_comment.xml
            View view = mInflater.inflate(R.layout.item_comment, parent, false);
            return new VH(view);
        }

        @Override
        protected void onBindVH(VH holder, Comment data, int position) {
            // 假设 DatabaseHelper 在查询时已经联表填充了 nickname 和 avatar
            String name = TextUtils.isEmpty(data.getNickname()) ? data.getUsername() : data.getNickname();
            holder.tvName.setText(name);
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