package com.lian.petadoption.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.lian.petadoption.R;
import com.lian.petadoption.base.BaseRecyclerAdapter;
import com.lian.petadoption.dao.ApplyInfo;
import com.lian.petadoption.utils.GlideUtils;

public class MessageAdapter extends BaseRecyclerAdapter<ApplyInfo, MessageAdapter.ViewHolder> {

    private String currentUsername;

    public MessageAdapter(Context context, String currentUsername) {
        super(context);
        this.currentUsername = currentUsername;
    }

    @Override
    protected ViewHolder onCreateVH(ViewGroup parent, int viewType) {
        // 确保布局文件名是 item_message.xml
        View view = mInflater.inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    protected void onBindVH(ViewHolder holder, ApplyInfo info, int position) {
        // 判断当前用户是“申请人”还是“发布者”
        boolean isMeApplicant = info.getName().equals(currentUsername);

        if (isMeApplicant) {
            // 我是申请人：查看申请进度
            holder.tvTitle.setText("申请进度更新");
            holder.tvContent.setText("您申请领养【" + info.getPetName() + "】");
        } else {
            // 我是发布者：有人申请我的宠物
            holder.tvTitle.setText("收到新申请");
            holder.tvContent.setText("用户 " + info.getName() + " 申请领养您的【" + info.getPetName() + "】");
        }

        // 设置状态标签 (复用 XML 中的 tv_msg_status_text)
        holder.tvStatus.setText(info.getState());

        // 根据状态改变标签颜色 (可选优化)
        if ("待审核".equals(info.getState())) {
            holder.tvStatus.setTextColor(Color.parseColor("#FF9800")); // 橙色
            holder.tvStatus.setBackgroundResource(R.drawable.shape_label_bg); // 假设黄色背景
        } else if ("已通过".equals(info.getState())) {
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")); // 绿色
        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#9E9E9E")); // 灰色
        }

        // 时间
        holder.tvTime.setText(info.getTime());

        // 红点状态 (0未读)
        if (info.getReadState() == 0) {
            holder.vDot.setVisibility(View.VISIBLE);
        } else {
            holder.vDot.setVisibility(View.GONE);
        }

        // 头像加载 (申请人的头像)
        // 注意：如果是发布者看消息，显示申请人头像；如果是申请人看消息，显示发布者头像或者宠物头像会更好
        // 这里暂时统一加载 info.getApplicantAvatar() 或 info.getPic()
        String avatarUrl = isMeApplicant ? info.getPic() : info.getApplicantAvatar();
        GlideUtils.loadCircle(mContext, avatarUrl, holder.ivAvatar);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvTime, tvStatus;
        ImageView ivAvatar;
        View vDot;

        ViewHolder(View v) {
            super(v);
            // === 关键修改：这里的 ID 必须与 XML 一致 ===
            tvTitle = v.findViewById(R.id.tv_msg_sender);      // XML中叫 tv_msg_sender
            tvContent = v.findViewById(R.id.tv_msg_pet_name);  // XML中叫 tv_msg_pet_name
            tvStatus = v.findViewById(R.id.tv_msg_status_text);// XML中叫 tv_msg_status_text
            tvTime = v.findViewById(R.id.tv_msg_time);
            ivAvatar = v.findViewById(R.id.iv_msg_avatar);
            vDot = v.findViewById(R.id.v_unread_dot);          // XML中叫 v_unread_dot
        }
    }
}