package com.xiaoyu.nuwa.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpRequest;
import org.slf4j.MDC;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import com.alibaba.fastjson2.JSON;
import com.xiaoyu.nuwa.utils.context.ContextUtil;
import com.xiaoyu.nuwa.utils.enums.LogLabelEnum;
import com.xiaoyu.nuwa.utils.enums.RequestHeaderEnum;

import lombok.extern.slf4j.Slf4j;

/**
 * 日志处理
 * 
 * @author xiaoyu
 *
 */
@Slf4j
public class LogUtils {

    private static final String Label_Start = ":\"";
    private static final String Label_End = "\"";

    private static final int Max_Header_Length = 100;

    private static final String Trace_Format = "[%s]";

    private static final PathMatcher pathMatcher = new AntPathMatcher();
    /**
     * 脱敏词
     */
    private static List<String> desensitizeWords = null;

    /**
     * 不打印返回体的请求
     */
    private static List<String> notPrintBodyLogUriFilters = new ArrayList<>(0);

    public static void requestIn(HttpServletRequest request, String body) {
        StringBuilder logPrefix = new StringBuilder();

        logPrefix.append(LogLabelEnum.LABEL.getLabel()).append(LogLabelEnum.CONCAT.getLabel())
                .append(LogLabelEnum.COM_REQUEST_IN.getLabel()).append(LogLabelEnum.SEPERATOR.getLabel());

        logPrefix.append(LogLabelEnum.URI.getLabel()).append(LogLabelEnum.CONCAT.getLabel())
                .append(ContextUtil.getStandardContext(LogLabelEnum.URI.getLabel()))
                .append(LogLabelEnum.SEPERATOR.getLabel());

        logPrefix.append(LogLabelEnum.REMOTEIP.getLabel()).append(LogLabelEnum.CONCAT.getLabel())
                .append(ContextUtil.getStandardContext(LogLabelEnum.REMOTEIP.getLabel()))
                .append(LogLabelEnum.SEPERATOR.getLabel());

        // 遍历header
        Enumeration<String> headerNames = request.getHeaderNames();
        Map<String, String> headerMap = new HashMap<>();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            // 过长的header不打印 比如cookie啥的
            if (headerValue != null && headerValue.length() < Max_Header_Length) {
                headerMap.put(headerName, headerValue);
            }
        }
        logPrefix.append(LogLabelEnum.HEADERS.getLabel()).append(LogLabelEnum.CONCAT.getLabel())
                .append(JSON.toJSONString(headerMap)).append(LogLabelEnum.SEPERATOR.getLabel());

        Map<String, String[]> paramsMap = request.getParameterMap();
        String paramsStr = null;
        if (paramsMap != null && !paramsMap.isEmpty()) {
            paramsStr = JSON.toJSONString(request.getParameterMap());
        }
        logPrefix.append(LogLabelEnum.PARAMS.getLabel()).append(LogLabelEnum.CONCAT.getLabel())
                .append(paramsStr);

        logPrefix.append(LogLabelEnum.SEPERATOR.getLabel());
        logPrefix.append(LogLabelEnum.BODY.getLabel()).append(LogLabelEnum.CONCAT.getLabel())
                .append(handleLogBody(body));

        String userId = ContextUtil.getStandardContext(RequestHeaderEnum.USERID.getName());
        if (StringUtils.isNotBlank(userId)) {
            logPrefix.append(LogLabelEnum.SEPERATOR.getLabel());
            logPrefix.append(LogLabelEnum.USERID.getLabel()).append(LogLabelEnum.CONCAT.getLabel()).append(userId);
        }

        log.info(logPrefix.toString());
    }

    public static void requestOut(String response) {
        StringBuilder logPrefix = new StringBuilder();
        logPrefix.append(LogLabelEnum.LABEL.getLabel()).append(LogLabelEnum.CONCAT.getLabel())
                .append(LogLabelEnum.COM_REQUEST_OUT.getLabel());

        logPrefix.append(LogLabelEnum.SEPERATOR.getLabel());
        logPrefix.append(LogLabelEnum.URI.getLabel()).append(LogLabelEnum.CONCAT.getLabel())
                .append(ContextUtil.getStandardContext(LogLabelEnum.URI.getLabel()));

        logPrefix.append(LogLabelEnum.SEPERATOR.getLabel());
        logPrefix.append(LogLabelEnum.REMOTEIP.getLabel()).append(LogLabelEnum.CONCAT.getLabel())
                .append(ContextUtil.getStandardContext(LogLabelEnum.REMOTEIP.getLabel()));

        String reqTime = ContextUtil.getStandardContext(LogLabelEnum.REQUEST_TIME.getLabel());
        if (StringUtils.isNotBlank(reqTime)) {
            logPrefix.append(LogLabelEnum.SEPERATOR.getLabel());
            long startTime = Long.valueOf(reqTime);
            logPrefix.append(LogLabelEnum.COST.getLabel()).append(LogLabelEnum.CONCAT.getLabel())
                    .append((System.currentTimeMillis() - startTime));
        }

        logPrefix.append(LogLabelEnum.SEPERATOR.getLabel());
        logPrefix.append(LogLabelEnum.RETURNCODE.getLabel()).append(LogLabelEnum.CONCAT.getLabel())
                .append(ContextUtil.getStandardContext(LogLabelEnum.RETURNCODE.getLabel()));

        logPrefix.append(LogLabelEnum.SEPERATOR.getLabel());
        logPrefix.append(LogLabelEnum.BODY.getLabel()).append(LogLabelEnum.CONCAT.getLabel())
                .append(handleLogBody(response));

        String userId = ContextUtil.getStandardContext(RequestHeaderEnum.USERID.getName());
        if (StringUtils.isNotBlank(userId)) {
            logPrefix.append(LogLabelEnum.SEPERATOR.getLabel());
            logPrefix.append(LogLabelEnum.USERID.getLabel()).append(LogLabelEnum.CONCAT.getLabel()).append(userId);
        }

        log.info(logPrefix.toString());
    }

    public static void httpIn(HttpRequest request, Object paramsObject) {
        StringBuilder logPrefix = new StringBuilder();

        logPrefix.append(LogLabelEnum.LABEL.getLabel()).append(LogLabelEnum.CONCAT.getLabel())
                .append(LogLabelEnum.HTTP_REQUEST_IN.getLabel());

        String path = "";
        String host = "";
        String query = "";
        try {
            URI uri = request.getUri();
            path = uri.getPath();
            host = uri.getHost();
            query = uri.getQuery();
        } catch (URISyntaxException e) {
            log.error("", e);
        }

        logPrefix.append(LogLabelEnum.SEPERATOR.getLabel());
        logPrefix.append(LogLabelEnum.URI.getLabel()).append(LogLabelEnum.CONCAT.getLabel()).append(path);

        logPrefix.append(LogLabelEnum.SEPERATOR.getLabel());
        logPrefix.append(LogLabelEnum.REMOTEIP.getLabel()).append(LogLabelEnum.CONCAT.getLabel()).append(host);

        Header[] headers = request.getHeaders();
        Map<String, String> headerMap = new HashMap<>();
        for (Header h : headers) {
            headerMap.put(h.getName(), h.getValue());
        }
        logPrefix.append(LogLabelEnum.SEPERATOR.getLabel());
        logPrefix.append(LogLabelEnum.HEADERS.getLabel()).append(LogLabelEnum.CONCAT.getLabel())
                .append(JSON.toJSONString(headerMap));

        if (StringUtils.isNotBlank(query)) {
            logPrefix.append(LogLabelEnum.SEPERATOR.getLabel());
            logPrefix.append(LogLabelEnum.QUERY.getLabel()).append(LogLabelEnum.CONCAT.getLabel())
                    .append(query);
        }

        String method = request.getMethod();
        String label = LogLabelEnum.BODY.getLabel();
        if (method == "GET") {
            label = LogLabelEnum.PARAMS.getLabel();
        }
        logPrefix.append(LogLabelEnum.SEPERATOR.getLabel());
        logPrefix.append(label).append(LogLabelEnum.CONCAT.getLabel())
                .append(handleLogBody(JSON.toJSONString(paramsObject)));

        log.info(logPrefix.toString());
    }

    public static void httpOut(HttpRequest request, String response, long cost, int httpStatus) {
        StringBuilder logPrefix = new StringBuilder();
        logPrefix.append(LogLabelEnum.LABEL.getLabel()).append(LogLabelEnum.CONCAT.getLabel())
                .append(LogLabelEnum.HTTP_REQUEST_OUT.getLabel());

        String path = "";
        String host = "";
        try {
            URI uri = request.getUri();
            path = uri.getPath();
            host = uri.getHost();
        } catch (URISyntaxException e) {
            log.error("", e);
        }
        logPrefix.append(LogLabelEnum.SEPERATOR.getLabel());
        logPrefix.append(LogLabelEnum.URI.getLabel()).append(LogLabelEnum.CONCAT.getLabel()).append(path);

        logPrefix.append(LogLabelEnum.SEPERATOR.getLabel());
        logPrefix.append(LogLabelEnum.REMOTEIP.getLabel()).append(LogLabelEnum.CONCAT.getLabel()).append(host);

        logPrefix.append(LogLabelEnum.SEPERATOR.getLabel());
        logPrefix.append(LogLabelEnum.COST.getLabel()).append(LogLabelEnum.CONCAT.getLabel()).append(cost);

        logPrefix.append(LogLabelEnum.SEPERATOR.getLabel());
        logPrefix.append(LogLabelEnum.HTTPSTATUS.getLabel()).append(LogLabelEnum.CONCAT.getLabel()).append(httpStatus);

        logPrefix.append(LogLabelEnum.SEPERATOR.getLabel());
        logPrefix.append(LogLabelEnum.BODY.getLabel()).append(LogLabelEnum.CONCAT.getLabel())
                .append(handleLogBody(response));

        log.info(logPrefix.toString());
    }

    /**
     * 适用于手动初始化一个链路,比如收到mq,收到一个xxljob,这种并没有传pspanid的
     * 
     * @param traceId
     */
    public static void initTrace(String traceId) {
        doInitTrace("0", IdGenerator.spanid(), traceId);
    }

    /**
     * 适用于外部请求初始化链路,比如外部请求,包含了pspanid
     * 
     * @param pspanId
     * @param traceId
     */
    public static void initTrace(String pspanId, String traceId) {
        doInitTrace(pspanId, IdGenerator.spanid(), traceId);
    }

    /**
     * 适用于异步线程.可将现有的链路传到子线程内.
     * 
     * @param pspanId
     * @param spanid
     * @param traceId
     */
    public static void initTrace(String pspanId, String spanid, String traceId) {
        doInitTrace(pspanId, spanid, traceId);
    }

    /**
     * 填充链路信息,包括放入上下文和MDC内
     * 
     * @param pspanId
     * @param spanid
     * @param traceId
     */
    private static void doInitTrace(String pspanId, String spanid, String traceId) {
        // logxml配置trace
        MDC.put(LogLabelEnum.PSPANID.getLabel(), String.format(Trace_Format, pspanId));
        MDC.put(LogLabelEnum.SPANID.getLabel(), String.format(Trace_Format, spanid));
        MDC.put(LogLabelEnum.TRACEID.getLabel(), String.format(Trace_Format, traceId));

        ContextUtil.setStandardContext(LogLabelEnum.PSPANID.getLabel(), pspanId);
        ContextUtil.setStandardContext(LogLabelEnum.SPANID.getLabel(), spanid);
        ContextUtil.setStandardContext(LogLabelEnum.TRACEID.getLabel(), traceId);

    }

    private static String handleLogBody(String originBody) {
        if (desensitizeWords != null) {
            return encryptLog(originBody);
        }
        return originBody;
    }

    /**
     * 添加脱敏信息
     * 
     * @param desensitizeWords 脱敏相关词
     */
    public static void fillDesensitize(List<String> desensitizeWords) {
        if (desensitizeWords != null && !desensitizeWords.isEmpty()) {
            LogUtils.desensitizeWords = desensitizeWords;
        }
    }

    /**
     * 设置不打印返回体的请求路径
     * 
     * @param urlPatterns
     */
    public static void fillNoLogUriFilters(List<String> urlPatterns) {
        if (urlPatterns != null && !urlPatterns.isEmpty()) {
            LogUtils.notPrintBodyLogUriFilters = urlPatterns;
        }
    }

    /**
     * 判断是否需要打印返回体
     * 
     * @param uri
     * @return
     */
    public static boolean notPrintBodyLog(String uri) {
        if (notPrintBodyLogUriFilters.isEmpty()) {
            return false;
        }
        for (String patten : notPrintBodyLogUriFilters) {
            if (pathMatcher.match(patten, uri)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 对关键词进行脱敏处理 脱敏的为字符串 对于非字符串进行了忽略
     * 
     * @param tlog
     * @return
     */
    private static String encryptLog(String tlog) {
        return doCryptLog(tlog, true);
    }

    /**
     * 将脱敏的日志进行解密
     * 
     * @param tlog
     * @return
     */
    public static String decryptLog(String tlog) {
        return doCryptLog(tlog, false);
    }

    private static String doCryptLog(String tlog, boolean encrypt) {
        if (StringUtils.isBlank(tlog)) {
            return tlog;
        }
        List<String> keyWords = desensitizeWords;
        StringBuilder params = new StringBuilder(tlog);
        int loc = -1;
        int labelLocStart = -1;
        int labelLocEnd = -1;
        String sub = null;
        int subLen = 0;
        String en = null;
        for (String w : keyWords) {
            loc = params.indexOf(w, 0);
            // 循环替换字符串
            while (loc != -1) {
                // -1是去掉w的引号
                labelLocStart = params.indexOf(Label_Start, loc + w.length() - 1);
                // Label_Start应该紧随w之后 否则可能找到的是下一个
                if (loc + w.length() != labelLocStart) {
                    // 偏离 寻找下一个
                    loc = params.indexOf(w, loc + w.length());
                    continue;
                }
                // 查找结束符的位置
                labelLocEnd = params.indexOf(Label_End, labelLocStart + Label_Start.length());
                if (labelLocEnd == -1) {
                    break;
                }
                sub = params.substring(labelLocStart + Label_Start.length(), labelLocEnd);
                subLen = sub.length();
                // 替换脱敏后的字符串
                if (StringUtils.isNotBlank(sub)) {
                    if (encrypt) {
                        en = EncryptUtil.xorEncrypt(sub);
                    } else {
                        en = EncryptUtil.xorDecrypt(sub);
                    }
                    params.replace(labelLocStart + Label_Start.length(), labelLocEnd, en);
                    subLen = en.length();
                }
                // 查找下一个位置
                loc = params.indexOf(w, labelLocStart + subLen);
            }
        }
        return params.toString();
    }

}
