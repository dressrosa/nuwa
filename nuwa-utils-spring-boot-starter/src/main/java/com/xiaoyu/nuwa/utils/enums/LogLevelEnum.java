package com.xiaoyu.nuwa.utils.enums;

import lombok.Getter;

@Getter
public enum LogLevelEnum {

    ERROR("ERROR"), WARN("WARN"), INFO("INFO"), DEBUG("DEBUG"), TRACE("TRACE"), NO_LOG("noLog"), NO_STACK("noStack"),;

    private String level;

    LogLevelEnum(String s) {
        level = s;
    }

}