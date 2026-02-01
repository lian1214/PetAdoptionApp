package com.lian.petadoption.utils;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

public class GlideUtils {
    // 默认占位图
    private static final int PLACEHOLDER_RES=android.R.drawable.ic_menu_gallery;
    private static final int ERROR_RES=android.R.drawable.ic_menu_report_image;

    // 图片加载
    public static void load(Context context, Object url, ImageView imageView){
        Glide.with(context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL) // 全缓存
                .placeholder(PLACEHOLDER_RES)
                .error(ERROR_RES)
                .into(imageView);
    }

    // 圆形图片加载
    public static void loadCircle(Context context,Object url,ImageView imageView){
        Glide.with(context)
                .load(url)
                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(PLACEHOLDER_RES)
                .error(ERROR_RES)
                .into(imageView);
    }

    // 圆角图片加载
    public static void loadRound(Context context,Object url,ImageView imageView,int radius){
        Glide.with(context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(PLACEHOLDER_RES)
                .error(ERROR_RES)
                .transform(new CenterCrop(),new RoundedCorners(dp2px(context,radius)))
                .into(imageView);
    }

    // dp 转 px
    private static int dp2px(Context context, float dpValue) {
        final float scale=context.getResources().getDisplayMetrics().density;
        return (int)(dpValue*scale+0.5f);
    }
}
