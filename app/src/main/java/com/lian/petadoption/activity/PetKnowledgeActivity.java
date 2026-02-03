package com.lian.petadoption.activity;

import com.lian.petadoption.dao.Knowledge;
import com.lian.petadoption.database.DataCallback;
import java.util.List;

public class PetKnowledgeActivity extends BaseKnowledgeActivity {

    @Override
    protected String getKnowledgeType() {
        return "pet";
    }

    @Override
    protected String getTitleText() {
        return "宠物健康常识";
    }

    @Override
    protected void initDefaultDataIfNeeded() {
        // 1. 先查
        databaseHelper.getKnowledgeList(getKnowledgeType(), new DataCallback<List<Knowledge>>() {
            @Override
            public void onSuccess(List<Knowledge> data) {
                // 2. 如果没数据，就插入
                if (data == null || data.isEmpty()) {
                    insertDefaultData();
                }
                // 如果有数据，loadData会自动处理显示，这里不用管
            }
            @Override
            public void onFail(String msg) {
                insertDefaultData();
            }
        });
    }

    private void insertDefaultData() {
        databaseHelper.addKnowledge("pet:医疗", "系统医生", "狗狗换季感冒预防",
                "注意早晚温差...", "", true, new DataCallback<Boolean>() {
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