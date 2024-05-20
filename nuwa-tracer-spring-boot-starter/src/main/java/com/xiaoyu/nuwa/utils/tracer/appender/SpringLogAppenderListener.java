package com.xiaoyu.nuwa.utils.tracer.appender;

import javax.annotation.PreDestroy;

import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.context.ApplicationListener;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

public class SpringLogAppenderListener implements ApplicationListener<ApplicationContextInitializedEvent> {

    private static boolean isStart = false;

    private static final CustomAppender appender = new CustomAppender();

    @Override
    public void onApplicationEvent(ApplicationContextInitializedEvent event) {
        if (isStart) {
            return;
        }
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = loggerContext.getLogger("ROOT");

        appender.setContext(loggerContext);
        appender.setName("CUSTOM");
        appender.start();
        if (!appender.isStarted()) {
            throw new RuntimeException("nuwa custom log appender start failed");
        }
        isStart = true;
        logger.addAppender(appender);
    }

    @PreDestroy
    public final void close() {
        appender.stop();
    }

}
