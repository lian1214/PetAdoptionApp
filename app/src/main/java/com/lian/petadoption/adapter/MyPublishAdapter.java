package com.lian.petadoption.adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.lian.petadoption.R;
import com.lian.petadoption.base.BaseRecyclerAdapter;
import com.lian.petadoption.config.AppConfig;
import com.lian.petadoption.dao.Adopt;
import com.lian.petadoption.utils.GlideUtils;

import java.util.List;

public class MyPublishAdapter extends BaseRecyclerAdapter<Adopt, MyPublishAdapter.ViewHolder> {

    public MyPublishAdapter(Context context) {
        super(context);
    }

    public MyPublishAdapter(Context context, List<Adopt> list) {
        super(context);
        setData(list);
    }

    @Override
    protected ViewHolder onCreateVH(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_my_publish, parent, false);
        return new ViewHolder(view);
    }

    @Override
    protected void onBindVH(ViewHolder holder, Adopt adopt, int position) {
        holder.tvPetName.setText(adopt.getPetName());

        String breed = TextUtils.isEmpty(adopt.getBreed()) ? "未知" : adopt.getBreed();
        String gender = TextUtils.isEmpty(adopt.getGender()) ? "未知" : adopt.getGender();
        String age = TextUtils.isEmpty(adopt.getAge()) ? "未知" : adopt.getAge();
        holder.tvPetInfo.setText(String.format("%s | %s | %s", breed, gender, age));

        String state = adopt.getState();
        if (TextUtils.isEmpty(state)) state = AppConfig.State.PET_WAITING;
        holder.tvStatusTag.setText(state);

        if (AppConfig.State.PET_ADOPTED.equals(state)) {
            holder.tvStatusTag.setTextColor(Color.parseColor("#999999"));
        } else {
            holder.tvStatusTag.setTextColor(Color.parseColor("#FFD90E"));
        }

        GlideUtils.loadRound(mContext, adopt.getCoverImage(), holder.ivPetPic, 4);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPetName, tvPetInfo, tvStatusTag;
        ImageView ivPetPic;

        public ViewHolder(View itemView) {
            super(itemView);
            tvPetName = itemView.findViewById(R.id.tv_pet_name);
            tvPetInfo = itemView.findViewById(R.id.tv_pet_info);
            tvStatusTag = itemView.findViewById(R.id.tv_status_tag);
            ivPetPic = itemView.findViewById(R.id.iv_pet_pic);
        }
    }
}