package com.xiaoyu.nuwa.utils.exception;

import com.xiaoyu.nuwa.utils.enums.LogLevelEnum;
import com.xiaoyu.nuwa.utils.response.BaseResponseCode;

import lombok.Getter;

@Getter
public class BizNoLogException extends CommonException {

    private static final long serialVersionUID = 1L;

    public BizNoLogException(BaseResponseCode resp, String msg) {
        super(resp, msg);
    }

    public BizNoLogException(BaseResponseCode resp) {
        super(resp);
    }

    public BizNoLogException(String code, String msg) {
        super(code, msg);
    }

    @Override
    public LogLevelEnum level() {
        return LogLevelEnum.NO_LOG;
    }
}
