package com.xiaoyu.nuwa.utils.exception;

import com.xiaoyu.nuwa.utils.enums.LogLevelEnum;

public interface LogLevel {

    default LogLevelEnum level() {
        return LogLevelEnum.ERROR;
    }

}
