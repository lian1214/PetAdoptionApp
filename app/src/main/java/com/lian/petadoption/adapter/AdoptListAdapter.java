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

/**
 * 领养列表适配器 (网格展示)
 */
public class AdoptListAdapter extends BaseRecyclerAdapter<Adopt, AdoptListAdapter.ViewHolder> {

    public AdoptListAdapter(Context context) {
        super(context);
    }

    // 兼容旧调用
    public AdoptListAdapter(Context context, List<Adopt> list) {
        super(context);
        setData(list);
    }

    @Override
    protected ViewHolder onCreateVH(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_adoptionlist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    protected void onBindVH(ViewHolder holder, Adopt adopt, int position) {
        holder.tvContent.setText(adopt.getPetName());

        // 拼接信息: 性别 | 年龄
        String gender = TextUtils.isEmpty(adopt.getGender()) ? "未知" : adopt.getGender();
        String age = TextUtils.isEmpty(adopt.getAge()) ? "未知" : adopt.getAge();
        holder.tvInfoTag.setText(String.format("%s | %s", gender, age));

        holder.tvAddress.setText(adopt.getAddress());

        // 使用 DAO 优化后的 getCoverImage() 方法获取第一张图
        String coverUrl = adopt.getCoverImage();

        // 加载图片 (CenterCrop)
        GlideUtils.load(mContext, coverUrl, holder.ivPetImage);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvInfoTag, tvAddress;
        ImageView ivPetImage;

        ViewHolder(View v) {
            super(v);
            tvContent = v.findViewById(R.id.tv_content);
            tvInfoTag = v.findViewById(R.id.tv_info_tag);
            tvAddress = v.findViewById(R.id.tv_address_small);
            ivPetImage = v.findViewById(R.id.iv_pet_image);
        }
    }
}