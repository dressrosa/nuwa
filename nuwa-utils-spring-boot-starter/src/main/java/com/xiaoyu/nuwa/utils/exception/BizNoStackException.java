package com.xiaoyu.nuwa.utils.exception;

import com.xiaoyu.nuwa.utils.enums.LogLevelEnum;
import com.xiaoyu.nuwa.utils.response.BaseResponseCode;

import lombok.Getter;

@Getter
public class BizNoStackException extends CommonException {

    private static final long serialVersionUID = 1L;

    public BizNoStackException(BaseResponseCode resp, String msg) {
        super(resp, msg);
    }

    public BizNoStackException(BaseResponseCode resp) {
        super(resp);
    }

    public BizNoStackException(String code, String msg) {
        super(code, msg);
    }

    @Override
    public LogLevelEnum level() {
        return LogLevelEnum.NO_STACK;
    }
}
