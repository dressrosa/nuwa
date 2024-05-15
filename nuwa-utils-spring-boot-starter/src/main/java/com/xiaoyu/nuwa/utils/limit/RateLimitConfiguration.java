package com.xiaoyu.nuwa.utils.limit;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import com.xiaoyu.nuwa.utils.exception.BizNoLogException;
import com.xiaoyu.nuwa.utils.response.BaseResponseCode;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Configuration
@ConfigurationProperties(prefix = "nuwa.rate")
@Getter
@Setter
@Slf4j
public class RateLimitConfiguration {

    /**
     * 是否开启 默认不开启
     */
    private boolean open = false;
    /**
     * 单机总限流 默认1k
     */
    private int total = 1000;

    /**
     * guava切面限流
     * 
     * @return
     */
    @Bean
    @ConditionalOnProperty(prefix = "nuwa.rate", name = "open", havingValue = "true")
    public DefaultPointcutAdvisor defaultPointcutAdvisor() {
        Cache<Object, Object> Limit_Cacher = CacheBuilder.newBuilder().maximumSize(total)
                .expireAfterWrite(1, TimeUnit.HOURS).build();
        DefaultPointcutAdvisor defaultPointcutAdvisor = new DefaultPointcutAdvisor();
        AspectJExpressionPointcut point = new AspectJExpressionPointcut();
        point.setExpression("@annotation(com.xiaoyu.nuwa.utils.limit.RateLimit)");
        defaultPointcutAdvisor.setPointcut(point);
        defaultPointcutAdvisor.setAdvice(new MethodBeforeAdvice() {
            @Override
            public void before(Method method, Object[] args, Object target) throws Throwable {
                HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                        .getRequest();
                RateLimit rateLimit = method.getAnnotation(RateLimit.class);
                String limitHeader = request.getHeader(rateLimit.limitHeader());
                if (StringUtils.isBlank(limitHeader)) {
                    return;
                }
                String limitKey = limitHeader + ":" + method.getDeclaringClass().getSimpleName() + "_"
                        + method.getName();
                RateLimiter limiter = null;
                try {
                    double limitPerSecond = Double.valueOf(rateLimit.limit()) / Double.valueOf(rateLimit.period());
                    limiter = (RateLimiter) Limit_Cacher.get(limitKey, () -> {
                        return RateLimiter.create(limitPerSecond);
                    });
                } catch (ExecutionException e) {
                    log.error("", e);
                }
                if (limiter != null && !limiter.tryAcquire()) {
                    throw new BizNoLogException(BaseResponseCode.Req_Limit, "request limit");
                }

            }
        });
        return defaultPointcutAdvisor;
    }
}