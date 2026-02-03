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
import com.lian.petadoption.dao.Adopt;
import com.lian.petadoption.utils.GlideUtils;

import java.util.List;

public class RecommendAdapter extends BaseRecyclerAdapter<Adopt, RecommendAdapter.ViewHolder> {

    public RecommendAdapter(Context context) {
        super(context);
    }

    // 兼容旧的调用方式
    public RecommendAdapter(Context context, List<Adopt> list) {
        super(context);
        setData(list);
    }

    @Override
    protected ViewHolder onCreateVH(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_recommend_pets, parent, false);
        return new ViewHolder(view);
    }

    @Override
    protected void onBindVH(ViewHolder holder, Adopt adopt, int position) {
        holder.tvName.setText(adopt.getPetName());

        String gender = TextUtils.isEmpty(adopt.getGender()) ? "未知" : adopt.getGender();
        String age = TextUtils.isEmpty(adopt.getAge()) ? "未知" : adopt.getAge();
        holder.tvInfo.setText(String.format("%s · %s", gender, age));

        holder.tvAddress.setText(adopt.getAddress());

        // 使用 DAO 中新加的 getCoverImage() 方法
        GlideUtils.loadRound(mContext, adopt.getCoverImage(), holder.ivPic, 8);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvInfo, tvAddress;
        ImageView ivPic;

        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_recommend_name);
            tvInfo = v.findViewById(R.id.tv_recommend_info);
            tvAddress = v.findViewById(R.id.tv_recommend_address);
            ivPic = v.findViewById(R.id.iv_recommend_pic);
        }
    }
}