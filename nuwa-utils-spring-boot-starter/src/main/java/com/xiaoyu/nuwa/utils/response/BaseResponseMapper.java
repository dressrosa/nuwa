package com.xiaoyu.nuwa.utils.response;

/**
 * 
 * 返回mapper <br>
 * 返回包装结构需实现此类.
 * 
 * @author hongy
 * 
 */
public interface BaseResponseMapper {

    /**
     * 子类需实现次方法,返回子类自定义的返回码
     * 
     * @return
     */
    public String returnCode();

}
