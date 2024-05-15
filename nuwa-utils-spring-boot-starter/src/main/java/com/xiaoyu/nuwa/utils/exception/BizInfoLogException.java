package com.xiaoyu.nuwa.utils.exception;

import com.xiaoyu.nuwa.utils.enums.LogLevelEnum;
import com.xiaoyu.nuwa.utils.response.BaseResponseCode;

import lombok.Getter;

@Getter
public class BizInfoLogException extends CommonException {

    private static final long serialVersionUID = 1L;

    public BizInfoLogException(String code, String msg) {
        super(code, msg);
    }

    public BizInfoLogException(BaseResponseCode resp, String msg) {
        super(resp, msg);
    }

    public BizInfoLogException(BaseResponseCode resp) {
        super(resp);
    }

    @Override
    public LogLevelEnum level() {
        return LogLevelEnum.INFO;
    }
}
