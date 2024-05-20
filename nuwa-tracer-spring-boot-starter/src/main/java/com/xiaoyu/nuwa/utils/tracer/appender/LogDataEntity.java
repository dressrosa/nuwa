package com.xiaoyu.nuwa.utils.tracer.appender;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogDataEntity {

    private String level;
    private Long timestamp;
    private String logger;
    private String message;
    private String stacktrace;

    private String pspanid;
    private String spanid;
    private String traceid;

    private Map<String, String> attach;
}
