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
import com.lian.petadoption.config.AppConfig;
import com.lian.petadoption.dao.ApplyInfo;
import com.lian.petadoption.utils.GlideUtils;

import java.util.List;

public class MyApplyAdapter extends BaseRecyclerAdapter<ApplyInfo, MyApplyAdapter.ViewHolder> {

    public MyApplyAdapter(Context context) {
        super(context);
    }

    public MyApplyAdapter(Context context, List<ApplyInfo> list) {
        super(context);
        setData(list);
    }

    @Override
    protected ViewHolder onCreateVH(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_my_apply, parent, false);
        return new ViewHolder(view);
    }

    @Override
    protected void onBindVH(ViewHolder holder, ApplyInfo info, int position) {
        holder.tvName.setText(info.getPetName());
        holder.tvTime.setText("申请时间：" + info.getTime());

        String state = info.getState();
        if (AppConfig.State.APPLY_PENDING.equals(state)) {
            holder.tvStatus.setText("状态：申请中");
            holder.tvStatus.setTextColor(Color.parseColor("#FF9800"));
        } else if (AppConfig.State.APPLY_PASSED.equals(state)) {
            holder.tvStatus.setText("状态：领养成功");
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
        } else if (AppConfig.State.APPLY_REJECTED.equals(state)) {
            holder.tvStatus.setText("状态：拒绝领养");
            holder.tvStatus.setTextColor(Color.parseColor("#F44336"));
        } else {
            holder.tvStatus.setText("状态：" + state);
            holder.tvStatus.setTextColor(Color.GRAY);
        }

        GlideUtils.loadRound(mContext, info.getCoverImage(), holder.ivPic, 4);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPic;
        TextView tvName, tvTime, tvStatus;

        public ViewHolder(View itemView) {
            super(itemView);
            ivPic = itemView.findViewById(R.id.iv_apply_pet_pic);
            tvName = itemView.findViewById(R.id.tv_apply_pet_name);
            tvTime = itemView.findViewById(R.id.tv_apply_time);
            tvStatus = itemView.findViewById(R.id.tv_apply_status);
        }
    }
}