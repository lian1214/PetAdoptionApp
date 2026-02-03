package com.lian.petadoption.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.lian.petadoption.R;
import com.lian.petadoption.base.BaseRecyclerAdapter;
import com.lian.petadoption.config.AppConfig;
import com.lian.petadoption.dao.Adopt;
import com.lian.petadoption.database.DataCallback; // 引入回调接口
import com.lian.petadoption.database.DatabaseHelper;
import com.lian.petadoption.utils.GlideUtils;

import java.util.List;

public class FavoriteAdapter extends BaseRecyclerAdapter<Adopt, FavoriteAdapter.ViewHolder> {

    private DatabaseHelper dbHelper;
    private String userName;
    private OnItemRemoveListener removeListener;

    // 删除回调接口，用于通知 Activity 更新空状态
    public interface OnItemRemoveListener {
        void onRemoved(int remainingCount);
    }

    public void setOnItemRemoveListener(OnItemRemoveListener listener) {
        this.removeListener = listener;
    }

    public FavoriteAdapter(Context context) {
        super(context);
        this.dbHelper = new DatabaseHelper(context);
        this.userName = context.getSharedPreferences(AppConfig.SP.NAME, Context.MODE_PRIVATE)
                .getString(AppConfig.SP.USER_ACCOUNT, "");
    }

    // 兼容旧构造函数
    public FavoriteAdapter(Context context, List<Adopt> list, String userName) {
        this(context);
        if (userName != null && !userName.isEmpty()) {
            this.userName = userName;
        }
        setData(list);
    }

    @Override
    protected ViewHolder onCreateVH(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_my_favorite, parent, false);
        return new ViewHolder(view);
    }

    @Override
    protected void onBindVH(ViewHolder holder, Adopt pet, int position) {
        holder.tvName.setText(pet.getPetName());

        String breed = TextUtils.isEmpty(pet.getBreed()) ? "未知" : pet.getBreed();
        String gender = TextUtils.isEmpty(pet.getGender()) ? "未知" : pet.getGender();
        String age = TextUtils.isEmpty(pet.getAge()) ? "未知" : pet.getAge();
        holder.tvInfo.setText(String.format("%s | %s | %s", breed, gender, age));

        holder.tvTag.setText(pet.getState());

        GlideUtils.loadRound(mContext, pet.getCoverImage(), holder.ivPic, 4);

        // 取消收藏逻辑
        holder.ivFavHeart.setOnClickListener(v -> {
            // 获取当前位置（放在点击事件里获取是安全的）
            int currentPos = holder.getBindingAdapterPosition();

            // 安全检查：如果 Item 已经不在列表中，不执行操作
            if (currentPos == RecyclerView.NO_POSITION) return;

            // 异步删除数据库记录
            dbHelper.deleteFavorite(userName, pet.getId(), new DataCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean data) {
                    // 再次获取位置（因为异步操作期间列表可能发生了变化）
                    int pos = holder.getBindingAdapterPosition();

                    if (pos != RecyclerView.NO_POSITION) {
                        // 调用基类的 remove 方法
                        remove(pos);
                        Toast.makeText(mContext, "已取消收藏", Toast.LENGTH_SHORT).show();

                        // 通知外部更新空状态
                        if (removeListener != null) {
                            removeListener.onRemoved(getItemCount());
                        }
                    }
                }

                @Override
                public void onFail(String msg) {
                    Toast.makeText(mContext, "操作失败: " + msg, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPic, ivFavHeart;
        TextView tvName, tvInfo, tvTag;

        public ViewHolder(View itemView) {
            super(itemView);
            ivPic = itemView.findViewById(R.id.iv_pet_pic);
            ivFavHeart = itemView.findViewById(R.id.iv_fav_heart);
            tvName = itemView.findViewById(R.id.tv_pet_name);
            tvInfo = itemView.findViewById(R.id.tv_pet_info);
            tvTag = itemView.findViewById(R.id.tv_tag);
        }
    }
}