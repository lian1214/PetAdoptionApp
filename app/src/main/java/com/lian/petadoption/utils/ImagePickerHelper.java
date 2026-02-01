package com.lian.petadoption.utils;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class ImagePickerHelper {
    private final AppCompatActivity appCompatActivity;
    private final Fragment fragment;
    private final int maxSelection;
    private ActivityResultLauncher<Intent> launcher;
    private OnImageSelectedListener listener;

    public interface OnImageSelectedListener{
        void onImageSelected(List<Uri> uris);
    }

    // Activity
    public ImagePickerHelper(AppCompatActivity activity,int maxSelection,OnImageSelectedListener listener){
        appCompatActivity=activity;
        fragment=null;
        this.maxSelection=maxSelection;
        this.listener=listener;
        initLauncher();
    }

    // Fragment
    public ImagePickerHelper(Fragment fragment,int maxSelection,OnImageSelectedListener listener){
        appCompatActivity=null;
        this.fragment=fragment;
        this.maxSelection=maxSelection;
        this.listener=listener;
        initLauncher();
    }

    private void initLauncher() {
        ActivityResultContracts.StartActivityForResult contract=new ActivityResultContracts.StartActivityForResult();

        // 定义回调处理逻辑
        androidx.activity.result.ActivityResultCallback<androidx.activity.result.ActivityResult> callback=result->{
            if (result.getResultCode()==AppCompatActivity.RESULT_OK && result.getData()!=null){
                List<Uri> selectedUris=new ArrayList<>();
                Intent data=result.getData();

                // 多选
                if (data.getClipData()!=null){
                    ClipData clipData = data.getClipData();
                    int count = Math.min(clipData.getItemCount(),maxSelection); // 限制数量
                    for (int i=0;i<count;i++) {
                        Uri uri=clipData.getItemAt(i).getUri();
                        persistUriPermission(uri);
                        selectedUris.add(uri);
                    }
                }
                // 单选
                else if (data.getData()!=null){
                    Uri uri=data.getData();
                    persistUriPermission(uri);
                    selectedUris.add(uri);
                }

                // 回调结果
                if (listener!=null && !selectedUris.isEmpty()){
                    listener.onImageSelected(selectedUris);
                }
            }
        };

        // 注册 Launcher
        if (appCompatActivity!=null){
            launcher=appCompatActivity.registerForActivityResult(contract,callback);
        }else if (fragment!=null){
            launcher=fragment.registerForActivityResult(contract,callback);
        }
    }

    // 申请 URI 永久读写权限
    private void persistUriPermission(Uri uri) {
        try {
            if (appCompatActivity!=null) {
                appCompatActivity.getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }else if (fragment!=null && fragment.getContext()!=null) {
                fragment.getContext().getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (appCompatActivity!=null) {
                Toast.makeText(appCompatActivity,"权限获取失败",Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 启动系统相册选择器
    public void pick(){
        if (launcher==null) return;

        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        if (maxSelection>1){
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
        }
        launcher.launch(intent);
    }
}
