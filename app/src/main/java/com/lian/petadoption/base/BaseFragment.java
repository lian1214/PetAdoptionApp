package com.lian.petadoption.base;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.lian.petadoption.config.AppConfig;
import com.lian.petadoption.database.DatabaseHelper;

public abstract class BaseFragment extends Fragment {
    protected Context context;
    protected View view;
    protected DatabaseHelper databaseHelper;
    protected SharedPreferences sharedPreferences;

    @Override
    public void onAttach(@Nullable Context context){
        super.onAttach(context);
        this.context=context;
    }

    @Nullable
    @Override
    public View onCreateView(@Nullable LayoutInflater layoutInflater, @Nullable ViewGroup viewGroup, @Nullable Bundle savedInsatanceState){
        if (view==null)
            view=layoutInflater.inflate(getLayoutId(),viewGroup,false);
        // 初始化工具类
        if (context!=null){
            databaseHelper=new DatabaseHelper(context);
            sharedPreferences=context.getSharedPreferences(AppConfig.SP.NAME,Context.MODE_PRIVATE);
        }

        initView(view);
        return view;
    }

    @Override
    public void onViewCreated(@Nullable View view,@Nullable Bundle savedInstanceState){
        super.onViewCreated(view,savedInstanceState);
        // 视图创建完毕后加载数据
        initData();
    }

    protected abstract void initData();

    protected void initView(View view) {
    }

    protected abstract int getLayoutId();

    protected void showToast(String msg){
        if (context!=null)
            Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
    }

    protected void navigateTo(Class<?> cls){
        if (context!=null)
            startActivity(new Intent(context,cls));
    }
}
