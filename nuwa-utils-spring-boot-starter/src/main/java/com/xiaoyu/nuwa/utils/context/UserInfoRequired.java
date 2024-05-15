package com.xiaoyu.nuwa.utils.context;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注controller方法 表示用户必须登录
 * 
 * @author xiaoyu
 *
 */
@Target({ ElementType.METHOD })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface UserInfoRequired {

    /**
     * 是否强制返回<br>
     * true 如果不存在登录参数或者未登录,直接拦截返回未登录<br>
     * false 如果存在登录参数,则正常拦截处理,如果不存在,则不处理.提供ContextUtils.loginCheck检查参数
     * 
     * @return
     */
    boolean force() default true;
}
