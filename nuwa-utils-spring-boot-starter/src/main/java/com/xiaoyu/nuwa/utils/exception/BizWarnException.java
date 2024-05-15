package com.xiaoyu.nuwa.utils.exception;

import com.xiaoyu.nuwa.utils.enums.LogLevelEnum;
import com.xiaoyu.nuwa.utils.response.BaseResponseCode;

import lombok.Getter;

@Getter
public class BizWarnException extends CommonException {

    private static final long serialVersionUID = 1L;

    public BizWarnException(BaseResponseCode resp, String msg) {
        super(resp, msg);
    }

    public BizWarnException(BaseResponseCode resp) {
        super(resp);
    }

    public BizWarnException(String code, String msg) {
        super(code, msg);
    }

    @Override
    public LogLevelEnum level() {
        return LogLevelEnum.WARN;
    }
}
