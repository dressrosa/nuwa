package com.xiaoyu.nuwa.utils.exception;

import com.xiaoyu.nuwa.utils.response.BaseResponseCode;

import lombok.Getter;

@Getter
public class CommonException extends RuntimeException implements LogLevel {

    private static final long serialVersionUID = 1L;
    private String code;

    public CommonException(BaseResponseCode resp) {
        super(resp.msg());
        this.code = resp.code();
    }

    public CommonException(BaseResponseCode resp, String msg) {
        super(msg);
        this.code = resp.code();
    }

    public CommonException(String code, String msg) {
        super(msg);
        this.code = code;
    }
}
