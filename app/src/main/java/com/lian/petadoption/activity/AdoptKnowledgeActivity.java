package com.lian.petadoption.activity;

import com.lian.petadoption.dao.Knowledge;
import com.lian.petadoption.database.DataCallback;
import java.util.List;

public class AdoptKnowledgeActivity extends BaseKnowledgeActivity {

    @Override
    protected String getKnowledgeType() {
        return "adopt";
    }

    @Override
    protected String getTitleText() {
        return "领养知识交流";
    }

    @Override
    protected void initDefaultDataIfNeeded() {
        databaseHelper.getKnowledgeList(getKnowledgeType(), new DataCallback<List<Knowledge>>() {
            @Override
            public void onSuccess(List<Knowledge> data) {
                if (data == null || data.isEmpty()) {
                    insertDefaultData();
                }
            }
            @Override
            public void onFail(String msg) {
                insertDefaultData();
            }
        });
    }

    private void insertDefaultData() {
        databaseHelper.addKnowledge("adopt:避雷", "系统助手", "领养协议签署注意",
                "请仔细核对回访条款...", "", true, new DataCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean data) {
                        // 【重点】插入成功后，手动刷新列表
                        loadData("");
                    }
                    @Override
                    public void onFail(String msg) {}
                });
    }
}