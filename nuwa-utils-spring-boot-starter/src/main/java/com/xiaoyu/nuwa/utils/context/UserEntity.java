/**
 * copyright com.xiaoyu
 */
package com.xiaoyu.nuwa.utils.context;

import lombok.Getter;
import lombok.Setter;

/**
 * 基本实体类参数配置
 * 
 */
@Getter
@Setter
public class UserEntity {

    /**
     * 用户角色
     */
    private String role;
    /**
     * 会话id
     */
    private String sid;

    /**
     * 用户id
     */
    private String userId;

    /**
     * 过期的时刻
     */
    protected Long expiredTime;

    public Long getLongUserId() {
        return Long.valueOf(userId);
    }

    public Integer getIntRole() {
        return Integer.valueOf(role);
    }
}
