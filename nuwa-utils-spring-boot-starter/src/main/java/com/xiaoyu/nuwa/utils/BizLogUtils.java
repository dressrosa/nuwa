package com.xiaoyu.nuwa.utils;

import com.xiaoyu.nuwa.utils.context.ContextUtil;
import com.xiaoyu.nuwa.utils.enums.LogLabelEnum;
import com.xiaoyu.nuwa.utils.enums.LogLevelEnum;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BizLogUtils {

    /**
     * 打印普通info格式化日志
     * 
     * @param event
     * @param msg
     */
    public static void bizInfoLog(String event, String msg) {
        bizLog(event, msg, LogLevelEnum.INFO);
    }

    public static void bizInfoLog(String event, Throwable e) {
        bizLog(event, e, LogLevelEnum.INFO);
    }

    /**
     * 打印普通warn格式化日志
     * 
     * @param event
     * @param msg
     */
    public static void bizWarnLog(String event, String msg) {
        bizLog(event, msg, LogLevelEnum.WARN);
    }

    public static void bizWarnLog(String event, Throwable e) {
        bizLog(event, e, LogLevelEnum.WARN);
    }

    /**
     * 打印普通error格式化日志
     * 
     * @param event
     * @param msg
     */
    public static void bizErrorLog(String event, String msg) {
        bizLog(event, msg, LogLevelEnum.ERROR);
    }

    public static void bizErrorLog(String event, Throwable e) {
        bizLog(event, e, LogLevelEnum.ERROR);
    }

    private static void bizLog(String event, Throwable e, LogLevelEnum logLevel) {
        StringBuilder sb = new StringBuilder();
        sb.append("event").append(LogLabelEnum.CONCAT.getLabel()).append(event)
                .append(LogLabelEnum.SEPERATOR.getLabel());

        sb.append("msg").append(LogLabelEnum.CONCAT.getLabel())
                .append(ContextUtil.getStandardContext(LogLabelEnum.TRACEID.getLabel()));
        switch (logLevel) {
        case ERROR:
            log.error(sb.toString(), e);
            break;
        case INFO:
            log.info(sb.toString(), e);
            break;
        case WARN:
            log.warn(sb.toString(), e);
            break;
        default:
            break;
        }
    }

    private static void bizLog(String event, String msg, LogLevelEnum logLevel) {
        StringBuilder sb = new StringBuilder();
        sb.append("event").append(LogLabelEnum.CONCAT.getLabel()).append(event)
                .append(LogLabelEnum.SEPERATOR.getLabel());

        sb.append("msg").append(LogLabelEnum.CONCAT.getLabel()).append(msg);
        switch (logLevel) {
        case ERROR:
            log.error(sb.toString());
            break;
        case INFO:
            log.info(sb.toString());
            break;
        case WARN:
            log.warn(sb.toString());
            break;
        default:
            break;
        }
    }

}
