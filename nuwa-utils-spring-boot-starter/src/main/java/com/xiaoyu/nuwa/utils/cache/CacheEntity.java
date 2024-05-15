package com.xiaoyu.nuwa.utils.cache;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CacheEntity {

    private Long expireTime;
    private Object value;

    public CacheEntity(Long expireTime, Object value) {
        this.expireTime = expireTime;
        this.value = value;
    }

    public boolean isExpire() {
        return System.currentTimeMillis() > expireTime;
    }
}
