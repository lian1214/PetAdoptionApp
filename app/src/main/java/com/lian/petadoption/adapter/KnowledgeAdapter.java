package com.lian.petadoption.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.lian.petadoption.R;
import com.lian.petadoption.base.BaseRecyclerAdapter;
import com.lian.petadoption.dao.Knowledge;
import com.lian.petadoption.utils.GlideUtils;

import java.util.List;

/**
 * 知识列表适配器 (重构版)
 */
public class KnowledgeAdapter extends BaseRecyclerAdapter<Knowledge, KnowledgeAdapter.ViewHolder> {

    public KnowledgeAdapter(Context context) {
        super(context);
    }

    // 兼容旧的调用方式
    public KnowledgeAdapter(Context context, List<Knowledge> list) {
        super(context);
        setData(list);
    }

    @Override
    protected ViewHolder onCreateVH(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_knowledge, parent, false);
        return new ViewHolder(view);
    }

    @Override
    protected void onBindVH(ViewHolder holder, Knowledge k, int position) {
        holder.tvTitle.setText(k.getTitle());
        holder.tvSummary.setText(k.getContent());
        holder.tvDate.setText(k.getTime());

        // --- 1. 标签处理逻辑 ---
        String typeStr = k.getType(); // 数据库存的是 "baseType:Tag"
        if (!TextUtils.isEmpty(typeStr) && typeStr.contains(":")) {
            String[] split = typeStr.split(":");
            holder.tvTag.setText(split.length > 1 ? split[1] : "知识分享");
        } else {
            // 默认分类
            if ("adopt".equals(typeStr)) holder.tvTag.setText("领养指南");
            else if ("pet".equals(typeStr)) holder.tvTag.setText("养宠常识");
            else holder.tvTag.setText("热门推荐");
        }

        // --- 2. 图片加载逻辑 ---
        // 使用 DAO 中新加的 getCoverImage() 方法简化逻辑
        GlideUtils.loadRound(mContext, k.getCoverImage(), holder.ivCover, 8);

        // 点击事件由 BaseRecyclerAdapter 统一处理，这里不需要再 setOnClickListener
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSummary, tvTag, tvDate;
        ImageView ivCover;

        ViewHolder(View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tv_item_title);
            tvSummary = v.findViewById(R.id.tv_item_summary);
            tvTag = v.findViewById(R.id.tv_item_tag);
            tvDate = v.findViewById(R.id.tv_item_date);
            ivCover = v.findViewById(R.id.iv_item_cover);
        }
    }
}