package com.lian.petadoption.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.lian.petadoption.R;
import com.lian.petadoption.activity.CheckInDetailActivity;
import com.lian.petadoption.base.BaseRecyclerAdapter;
import com.lian.petadoption.dao.CheckIn;
import com.lian.petadoption.database.DataCallback;
import com.lian.petadoption.database.DatabaseHelper;
import com.lian.petadoption.utils.GlideUtils;

import java.util.List;
import java.util.Map;

public class CheckInAdapter extends BaseRecyclerAdapter<CheckIn, CheckInAdapter.ViewHolder> {

    public static final int TYPE_PUBLIC_SQUARE = 0;
    public static final int TYPE_MY_OWN = 1;

    private int mode;
    private DatabaseHelper dbHelper;
    private String currentUserName;

    public CheckInAdapter(Context context, List<CheckIn> list, int mode) {
        super(context);
        this.mode = mode;
        this.dbHelper = new DatabaseHelper(context);
        this.currentUserName = context.getSharedPreferences("User", Context.MODE_PRIVATE).getString("name", "");
        setData(list);
    }

    // 兼容只传 context 的构造
    public CheckInAdapter(Context context) {
        this(context, null, TYPE_MY_OWN);
    }

    @Override
    protected ViewHolder onCreateVH(ViewGroup parent, int viewType) {
        int layoutId = (mode == TYPE_MY_OWN) ? R.layout.item_my_checkin : R.layout.item_check_in;
        View view = mInflater.inflate(layoutId, parent, false);
        return new ViewHolder(view, mode);
    }

    @Override
    protected void onBindVH(ViewHolder holder, CheckIn item, int position) {
        // 获取发布者信息
        Map<String, String> publisherInfo = dbHelper.getUserInfo(item.getUsername());
        String showNickname = (publisherInfo != null) ? publisherInfo.get("u_nickname") : item.getUsername();
        String showAvatarPath = (publisherInfo != null) ? publisherInfo.get("u_head") : "";

        if (mode == TYPE_MY_OWN) {
            holder.tvMyPetName.setText(item.getPetName() + " (已领养)");
            holder.tvMyContent.setText(item.getContent());
            holder.tvMyTime.setText(item.getTime());
            holder.tvMyDays.setText("打卡记录");
            GlideUtils.loadCircle(mContext, showAvatarPath, holder.ivMyAvatar);
        } else {
            holder.tvUser.setText(showNickname);
            holder.tvContent.setText(item.getContent());
            holder.tvPetTag.setText("#" + item.getPetName());
            holder.tvTime.setText(item.getTime());

            if (holder.ivListAvatar != null) {
                GlideUtils.loadCircle(mContext, showAvatarPath, holder.ivListAvatar);
            }

            refreshLikeUI(holder, item.getId());

            // 图片加载 (使用 getCoverImage 简化)
            GlideUtils.load(mContext, item.getCoverImage(), holder.ivPic);

            if (holder.llLikeBtn != null) {
                holder.llLikeBtn.setOnClickListener(v -> {
                    dbHelper.toggleCheckInLike(item.getId(), currentUserName, new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean isLiked) {
                            // 点赞成功后，刷新 UI
                            refreshLikeUI(holder, item.getId());
                        }

                        @Override
                        public void onFail(String msg) {
                            // 可选：提示失败
                            // Toast.makeText(mContext, "操作失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }
        }

        // 跳转详情 (BaseRecyclerAdapter 也可以设置 OnItemClickListener，这里保留你的原有逻辑)
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, CheckInDetailActivity.class);
            intent.putExtra("punch_data", item);
            mContext.startActivity(intent);
        });
    }

    private void refreshLikeUI(ViewHolder holder, int punchId) {
        if (holder.ivLike == null || holder.tvLikeCount == null) return;
        int count = dbHelper.getCheckInLikeCount(punchId); // 需确保 Helper 有此方法
        boolean liked = dbHelper.isCheckInLiked(punchId, currentUserName); // 需确保 Helper 有此方法

        holder.tvLikeCount.setText(String.valueOf(count));
        if (liked) {
            holder.ivLike.setImageResource(android.R.drawable.btn_star_big_on);
            holder.ivLike.setColorFilter(Color.parseColor("#FFD90E"));
            holder.tvLikeCount.setTextColor(Color.parseColor("#FFD90E"));
        } else {
            holder.ivLike.setImageResource(android.R.drawable.btn_star_big_off);
            holder.ivLike.clearColorFilter();
            holder.tvLikeCount.setTextColor(Color.parseColor("#333333"));
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUser, tvContent, tvPetTag, tvTime, tvLikeCount, tvMyPetName, tvMyTime, tvMyDays, tvMyContent;
        ImageView ivPic, ivLike, ivMyAvatar, ivListAvatar;
        View llLikeBtn;

        public ViewHolder(View itemView, int mode) {
            super(itemView);
            if (mode == TYPE_MY_OWN) {
                ivMyAvatar = itemView.findViewById(R.id.iv_pet_avatar);
                tvMyPetName = itemView.findViewById(R.id.tv_pet_name);
                tvMyTime = itemView.findViewById(R.id.tv_checkin_time);
                tvMyDays = itemView.findViewById(R.id.tv_checkin_days);
                tvMyContent = itemView.findViewById(R.id.tv_checkin_content);
            } else {
                tvUser = itemView.findViewById(R.id.tv_username);
                ivListAvatar = itemView.findViewById(R.id.iv_list_avatar);
                tvContent = itemView.findViewById(R.id.tv_content);
                tvPetTag = itemView.findViewById(R.id.tv_pet_tag);
                tvTime = itemView.findViewById(R.id.tv_time);
                ivPic = itemView.findViewById(R.id.iv_checkin_pic);
                ivLike = itemView.findViewById(R.id.iv_like);
                tvLikeCount = itemView.findViewById(R.id.tv_like_count);
                llLikeBtn = itemView.findViewById(R.id.ll_like_btn);
            }
        }
    }
}