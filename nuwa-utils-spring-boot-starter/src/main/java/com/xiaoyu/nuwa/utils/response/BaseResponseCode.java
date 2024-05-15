package com.xiaoyu.nuwa.utils.response;

/**
 * @author xiaoyu
 * @description 请求返回码<br/>
 *              错误码总6位 ,1xx通用,2xx保留<br/>
 *              子类可以此实现个性化的返回码
 * 
 */
public interface BaseResponseCode {

    public static final String Success = "000000";

    public static final String Args_Error = "110001";

    public static final String No_Data = "100404";

    public static final String Failed = "100500";

    public static final String Outer_Error = "100600";

    public static final String Un_Login = "200001";

    public static final String Req_NoAccess = "200002";

    public static final String Req_Error = "200003";

    public static final String Sign_Error = "200004";

    public static final String Req_Limit = "200005";

    public static final String Req_Repeat = "200006";

    public static final String Connect_Error = "200007";

    public static final String Socket_Error = "200008";

    public static final String Connect_404 = "200009";

    String code();

    String msg();

}
