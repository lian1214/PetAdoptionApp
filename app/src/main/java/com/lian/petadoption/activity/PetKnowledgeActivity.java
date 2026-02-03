package com.lian.petadoption.activity;

import com.lian.petadoption.database.DataCallback;

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
        // 检查是否为空，如果为空则插入一条默认数据
        databaseHelper.getKnowledgeList(getKnowledgeType(), new DataCallback<java.util.List<com.lian.petadoption.dao.Knowledge>>() {
            @Override
            public void onSuccess(java.util.List<com.lian.petadoption.dao.Knowledge> data) {
                if (data.isEmpty()) {
                    databaseHelper.addKnowledge("pet:医疗", "系统医生", "狗狗换季感冒预防",
                            "注意早晚温差，不要让狗狗直接睡地板...", "", true, null);
                    // 重新加载显示出来
                    loadData("");
                }
            }
            @Override
            public void onFail(String msg) {}
        });
    }
}