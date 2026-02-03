package com.lian.petadoption.base;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * @param <T> 数据实体类型
 * @param <VH> ViewHolder 类型
 */
public abstract class BaseRecyclerAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    protected Context mContext;
    protected List<T> mList;
    protected LayoutInflater mInflater;
    private OnItemClickListener<T> mListener;

    // 点击事件回调接口
    public interface OnItemClickListener<T> {
        void onItemClick(T item, int position);
    }

    public BaseRecyclerAdapter(Context context) {
        this.mContext = context;
        this.mList = new ArrayList<>();
        this.mInflater = LayoutInflater.from(context);
    }

    // 设置点击监听
    public void setOnItemClickListener(OnItemClickListener<T> listener) {
        this.mListener = listener;
    }

    // 刷新数据
    public void setData(List<T> data) {
        this.mList.clear();
        if (data != null) {
            this.mList.addAll(data);
        }
        notifyDataSetChanged();
    }

    // 追加数据
    public void addData(List<T> data) {
        if (data != null && !data.isEmpty()) {
            int startPos = this.mList.size();
            this.mList.addAll(data);
            notifyItemRangeInserted(startPos, data.size());
        }
    }

    // 获取单条数据
    public T getItem(int position) {
        return (mList != null && position >= 0 && position < mList.size()) ? mList.get(position) : null;
    }

    public void remove(int position) {
        if (mList != null && position >= 0 && position < mList.size()) {
            mList.remove(position);
            // 移除 Item 动画
            notifyItemRemoved(position);
            // 刷新受影响的 Item 索引 (防止后续点击错位)
            notifyItemRangeChanged(position, mList.size() - position);
        }
    }

    @Override
    public int getItemCount() {
        return mList != null ? mList.size() : 0;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return onCreateVH(parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        T item = getItem(position);

        // 绑定数据
        onBindVH(holder, item, position);

        // 统一处理点击事件
        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onItemClick(item, position);
            }
        });
    }

    protected abstract VH onCreateVH(ViewGroup parent, int viewType);
    protected abstract void onBindVH(VH holder, T item, int position);
}