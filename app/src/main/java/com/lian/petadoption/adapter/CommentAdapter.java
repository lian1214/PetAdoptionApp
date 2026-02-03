package com.lian.petadoption.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.lian.petadoption.R;
import com.lian.petadoption.base.BaseRecyclerAdapter;
import com.lian.petadoption.dao.Comment;
import com.lian.petadoption.utils.GlideUtils;

public class CommentAdapter extends BaseRecyclerAdapter<Comment, CommentAdapter.ViewHolder> {

    public CommentAdapter(Context context) {
        super(context);
    }

    @Override
    protected ViewHolder onCreateVH(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    protected void onBindVH(ViewHolder holder, Comment data, int position) {
        // 名字：优先显示昵称，没有则显示账号 (逻辑已在 DatabaseHelper 处理)
        holder.tvName.setText(data.getNickname());

        // 内容和时间
        holder.tvContent.setText(data.getContent());
        holder.tvTime.setText(data.getTime());

        // 头像：直接加载，无需再查库
        GlideUtils.loadCircle(mContext, data.getAvatar(), holder.ivAvatar);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName, tvContent, tvTime;

        public ViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_comment_avatar);
            tvName = itemView.findViewById(R.id.tv_comment_name);
            tvContent = itemView.findViewById(R.id.tv_comment_content);
            tvTime = itemView.findViewById(R.id.tv_comment_time);
        }
    }
}