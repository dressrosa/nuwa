package com.xiaoyu.nuwa.utils.tracer.appender;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

import com.xiaoyu.nuwa.utils.context.ContextUtil;
import com.xiaoyu.nuwa.utils.enums.LogLabelEnum;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import io.fury.Fury;
import io.fury.ThreadLocalFury;
import io.fury.ThreadSafeFury;
import io.fury.config.Language;

/**
 * 自定义logAppender
 * 
 * @author xiaoyu
 *
 */
public class CustomAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private DatagramSocket socket = null;

    private static InetSocketAddress socketAddress = new InetSocketAddress("127.0.0.1", 12345);

    private static final int MaxErrorStack = 30;

    private static final ThreadSafeFury FuryInstance = new ThreadLocalFury(classLoader -> {
        Fury f = Fury.builder().withLanguage(Language.JAVA)
                .withClassLoader(classLoader).build();
        f.register(LogDataEntity.class);
        return f;
    });

    @Override
    public void start() {
        super.start();
        try {
            socket = new DatagramSocket();
        } catch (SocketException ignore) {

        }
    }

    @Override
    public void stop() {
        if (socket != null) {
            socket.close();
        }
        super.stop();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (socket == null) {
            return;
        }
        subAppend(event);
    }

    private void subAppend(ILoggingEvent event) {
        LogDataEntity en = new LogDataEntity();
        en.setTimestamp(event.getTimeStamp());
        en.setLogger(event.getLoggerName());
        String level = event.getLevel().toString();
        en.setLevel(level);
        if (level.charAt(0) == 'I' && event.hasCallerData()) {
            StackTraceElement[] ste = event.getCallerData();
            for (int i = 0; i < ste.length; i++) {
                if (ste[i].getClassName().equals(event.getLoggerName())) {
                    en.setLogger(event.getLoggerName() + ":" + ste[i].getLineNumber());
                    break;
                }
            }
        }
        en.setMessage(event.getFormattedMessage());
        en.setStacktrace(handleErrorMsg(event));
        en.setPspanid(ContextUtil.getStandardContext(LogLabelEnum.PSPANID.getLabel()));
        en.setSpanid(ContextUtil.getStandardContext(LogLabelEnum.SPANID.getLabel()));
        en.setTraceid(ContextUtil.getStandardContext(LogLabelEnum.TRACEID.getLabel()));
        byte[] byteArray = FuryInstance.serializeJavaObject(en);
        DatagramPacket packet = new DatagramPacket(byteArray, byteArray.length, socketAddress);
        try {
            socket.send(packet);
        } catch (Throwable ignore) {
        }
    }

    /**
     * 异常堆栈信息
     */
    private String handleErrorMsg(ILoggingEvent event) {
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(" :\n ").append(throwableProxy.getClassName()).append(" : ").append(throwableProxy.getMessage())
                .append("\n");
        StackTraceElementProxy[] arr = throwableProxy.getStackTraceElementProxyArray();
        // 最多取n层
        int len = arr.length > MaxErrorStack ? MaxErrorStack : arr.length;
        for (int i = 0; i < len; i++) {
            sb.append("at " + arr[i].getStackTraceElement().toString()).append("\n");
        }
        return sb.toString();
    }

}
