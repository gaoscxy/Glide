package com.gaos.glide.glide.cache;

import com.gaos.glide.glide.resource.Value;
import com.gaos.glide.glide.util.Tool;
import com.gaos.glide.glide.resource.ValueCallback;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 活动缓存 就是一个map
 * get
 * put
 * recycle
 */
public class ActiveCache {
    private Map<String, Value> mapList = new HashMap<>();
    private com.gaos.glide.glide.resource.ValueCallback valueCallBack;

    public ActiveCache(ValueCallback valueCallBack) {
        this.valueCallBack = valueCallBack;
    }

    /**
     * TODO 添加 活动缓存
     */
    public void put(String key, Value value) {
        Tool.checkNotEmpty(key); // key 不能为空

        // 每次put的时候 put进来的Value 绑定到 valueCallback
        value.setCallBack(this.valueCallBack);

        // 存储 --> 容器
        mapList.put(key, value);
    }

    /**
     * TODO 给外界获取Value
     */
    public Value get(String key) {
        Value value = mapList.get(key);
        if (null != value) {
            return value; // 返回容器里面的 Value
        }
        return null;
    }

    public void deleteActive(String key){
        if(mapList!=null) {
            mapList.remove(key);
        }
    }

    /**
     * 释放缓存方法，错误的，在一边遍历的时候一边删除，是不对的
     */
    /*public void recycleActive() {

        for (Map.Entry<String, Value> valueEntry : mapList.entrySet()) {
            valueEntry.getValue().recycle(); // 移除后 加入到  LRU内存缓存
            mapList.remove(valueEntry.getKey()); // 移除
        }
    }
*/
    public void recycleActive() {
        //mapList.clear();
        Iterator<Map.Entry<String, Value>> it = mapList.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<String, Value> entry = it.next();
            it.remove();//使用迭代器的remove()方法删除元素
        }
    }



}
