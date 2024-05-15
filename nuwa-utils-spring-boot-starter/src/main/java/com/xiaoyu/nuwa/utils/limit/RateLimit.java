package com.xiaoyu.nuwa.utils.limit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注controller方法 表示限流.<br>
 * 根据limitHeader,在period内限制请求limit次. <br>
 * 如根据limitHeader="userId",在period=10秒内限制请求limit=3次
 * 
 * @author xiaoyu
 *
 */
@Target({ ElementType.METHOD })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限制的request的Header名
     * 
     * @return
     */
    String limitHeader() ;

    /**
     * 限制区间 单位秒
     * 
     * @return
     */
    int period() default 1;

    /**
     * 限制请求次数
     * 
     * @return
     */
    int limit() default 1;
}
