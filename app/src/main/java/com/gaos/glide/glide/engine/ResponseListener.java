package com.gaos.glide.glide.engine;


import com.gaos.glide.glide.resource.Value;

/**
 * 加载外部资源 成功 和 失败 回调
 */
public interface ResponseListener {

    public void responseSuccess(String key, Value value); // 成功 Value

    public void responseException(Exception e); // 错误详情

}
