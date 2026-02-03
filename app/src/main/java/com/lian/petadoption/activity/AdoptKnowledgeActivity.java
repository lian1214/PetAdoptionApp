package com.lian.petadoption.activity;

import com.lian.petadoption.database.DataCallback;

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
        databaseHelper.getKnowledgeList(getKnowledgeType(), new DataCallback<java.util.List<com.lian.petadoption.dao.Knowledge>>() {
            @Override
            public void onSuccess(java.util.List<com.lian.petadoption.dao.Knowledge> data) {
                if (data.isEmpty()) {
                    databaseHelper.addKnowledge("adopt:避雷", "系统助手", "领养协议签署注意",
                            "请仔细核对回访条款，避免霸王条款...", "", true, null);
                    loadData("");
                }
            }
            @Override
            public void onFail(String msg) {}
        });
    }
}