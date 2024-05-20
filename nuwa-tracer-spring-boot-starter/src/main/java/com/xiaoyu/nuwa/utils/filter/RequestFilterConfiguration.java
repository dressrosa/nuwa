package com.xiaoyu.nuwa.utils.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import com.xiaoyu.nuwa.promethus.MetricUtils;
import com.xiaoyu.nuwa.utils.ContentTypeUtil;
import com.xiaoyu.nuwa.utils.LogUtils;
import com.xiaoyu.nuwa.utils.context.ContextUtil;
import com.xiaoyu.nuwa.utils.enums.LogLabelEnum;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "nuwa.request")
@Getter
@Setter
@Order(0)
public class RequestFilterConfiguration {

    private PathMatcher pathMatcher = new AntPathMatcher();
    /**
     * 匹配的路径
     */
    private List<String> patterns = new ArrayList<>();

    /**
     * 剔除的路径
     */
    private List<String> excludes = new ArrayList<>();

    /**
     * 请求体类型
     */
    private List<String> contentTypes = new ArrayList<>();

    /**
     * 是否支持跨域
     */
    private boolean cors;

    @Bean
    public FilterRegistrationBean<CustomFilter> registerFilter() {
        FilterRegistrationBean<CustomFilter> filter = new FilterRegistrationBean<CustomFilter>();
        if (patterns.isEmpty()) {
            filter.addUrlPatterns("/*");
        } else {
            filter.addUrlPatterns(patterns.toArray(new String[0]));
        }
        filter.setFilter(new CustomFilter());
        return filter;
    }

    private boolean containsContentType(String ctype) {
        if (contentTypes.isEmpty()) {
            return false;
        }
        for (String contentType : contentTypes) {
            if (contentType == null) {
                continue;
            }
            if (contentType.startsWith(ctype)) {
                return true;
            }
        }
        return false;
    }

    private class CustomFilter implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            if (!(request instanceof HttpServletRequest)) {
                chain.doFilter(request, response);
                return;
            }
            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse resp = (HttpServletResponse) response;
            resp.setCharacterEncoding("UTF-8");
            if (cors) {
                resp.setHeader("Access-Control-Allow-Origin", "*");
                resp.setHeader("Access-Control-Allow-Methods", "*");
                resp.setHeader("Access-Control-Allow-Headers", "*");
                resp.setHeader("Access-Control-Allow-Credentials", "true");
            }
            // promethus
            MetricUtils.comRequestIn(req.getRequestURI());
            // 不拦截的请求
            if (!excludes.isEmpty() && StringUtils.isNotBlank(req.getServletPath())) {
                for (String pattern : excludes) {
                    if (pathMatcher.match(pattern, req.getServletPath())) {
                        chain.doFilter(request, response);
                        return;
                    }
                }
            }
            // 默认支持get请求 application/json text/plain
            if (ContentTypeUtil.isApplicationJson(req.getContentType())
                    || "GET".equals(req.getMethod())
                    || ContentTypeUtil.isTextPlain(req.getContentType())
                    || containsContentType(req.getContentType())) {
                req = new ReadableRequestWrapper(req);
                resp = new ReadableResponseWrapper(resp);
            }
            preHandler(req);
            chain.doFilter(req, resp);
            postHandler(req, resp);
            destroyHandler();
        }

        private void preHandler(HttpServletRequest request) {
            // 设置上下文
            ContextUtil.setStandardContext(request);
            if (ContentTypeUtil.isProtoRequest(request.getContentType())) {
                return;
            }
            requestIn(request);
        }

        private void requestIn(HttpServletRequest request) {
            String body = null;
            if (request instanceof ReadableRequestWrapper) {
                ReadableRequestWrapper wrapper = (ReadableRequestWrapper) request;
                body = wrapper.getBody();
            }
            LogUtils.requestIn(request, body);
        }

        private void postHandler(HttpServletRequest req, HttpServletResponse resp) {
            // 不打印返回体
            if (LogUtils.notPrintBodyLog(req.getServletPath())) {
                LogUtils.requestOut("");
                return;
            }
            if (ContextUtil.containsKey(LogLabelEnum.RETURNBODY.getLabel())) {
                LogUtils.requestOut(ContextUtil.getStandardContext(LogLabelEnum.RETURNBODY.getLabel()));
            } else {
                requestOut(resp);
            }
        }

        private void requestOut(HttpServletResponse resp) {
            String body = "";
            if (resp instanceof ReadableResponseWrapper) {
                ReadableResponseWrapper wrapper = (ReadableResponseWrapper) resp;
                body = wrapper.getBody();
            }
            LogUtils.requestOut(body);
        }

        private void destroyHandler() {
            // 去除上下文
            ContextUtil.removeContext();
        }

    }
}
