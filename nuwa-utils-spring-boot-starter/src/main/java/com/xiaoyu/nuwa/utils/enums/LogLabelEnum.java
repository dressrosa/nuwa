package com.xiaoyu.nuwa.utils.enums;

import lombok.Getter;

@Getter
public enum LogLabelEnum {

    /**
     * 链路id
     */
    TRACEID("traceid"),

    /**
     * 区间链路id
     */
    SPANID("spanid"),

    /**
     * 上游区间链路id
     */
    PSPANID("pspanid"),

    /**
     * 请求时刻
     */
    REQUEST_TIME("requestTime"),

    /**
     * 请求路径
     */
    URI("uri"),

    /**
     * get请求参数
     */
    PARAMS("params"),

    /**
     * 拼在url后面的参数.<br>
     * params的区别:<br>
     * 1.params是通过传参设置的<br>
     * 2.query是直接问号(?)后面的,post请求也可能存在
     */
    QUERY("query"),

    /**
     * post请求体或者请求返回体
     */
    BODY("body"),

    /**
     * 请求头
     */
    HEADERS("headers"),

    /**
     * 请求地址
     */
    REMOTEIP("remoteip"),

    /**
     * 标志位,表示日志的类型
     */
    LABEL("_msg"),

    /**
     * 请求花费时间
     */
    COST("cost"),

    USERID("userid"),

    ROLE("role"),

    /**
     * 请求返回码
     */
    RETURNCODE("return_code"),

    /**
     * 返回体 ,用于上下文里
     */
    RETURNBODY("return_body"),

    /**
     * http状态码
     */
    HTTPSTATUS("http_status"),

    /**
     * 访问服务的请求
     */
    COM_REQUEST_IN("com_request_in"),

    /**
     * 访问服务请求的返回
     */
    COM_REQUEST_OUT("com_request_out"),

    /**
     * http请求
     */
    HTTP_REQUEST_IN("http_request_in"),

    /**
     * http请求的返回
     */
    HTTP_REQUEST_OUT("http_request_out"),

    CONCAT("="),

    SEPERATOR("||"),

    ;

    String label;

    LogLabelEnum(String label) {
        this.label = label;
    }

}
