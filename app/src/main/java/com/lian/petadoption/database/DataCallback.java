package com.lian.petadoption.database;

/**
 * 数据库异步回调接口
 * @param <T> 返回的数据类型
 */
public interface DataCallback<T> {
    // 操作成功 返回数据
    void onSuccess(T data);

    // 操作失败 返回错误信息
    void onFail(String msg);
}