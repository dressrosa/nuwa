package com.xiaoyu.nuwa.utils.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import com.xiaoyu.nuwa.utils.IdGenerator;
import com.xiaoyu.nuwa.utils.LogUtils;
import com.xiaoyu.nuwa.utils.enums.LogLabelEnum;
import com.xiaoyu.nuwa.utils.enums.RequestHeaderEnum;

import lombok.extern.slf4j.Slf4j;

/**
 * 用户上下文
 * 
 * @author xiaoyu
 *
 */
@Slf4j
public class ContextUtil {

    /**
     * 上下文的所有信息 只存在一次请求的生命周期
     */
    private static final ConcurrentHashMap<Thread, ThreadLocal<Map<String, Object>>> contextThreadLocalMap = new ConcurrentHashMap<>();

    /**
     * 用户登录信息 cacheKey->user 未过期前都存在<br/>
     * cacheKey = userId+"-"+sid;
     * 
     */
    private static final ConcurrentHashMap<String, UserEntity> userThreadLocalMap = new ConcurrentHashMap<>();

    private static Object cacheCleanLock = new Object();

    private static boolean cleanUserCacheJobStart = false;

    public static void startUserCleanTreadIfNot(int cycleSecond) {
        // 定时清除过期的token-userEntity
        if (cleanUserCacheJobStart) {
            return;
        }
        synchronized (cacheCleanLock) {
            if (cleanUserCacheJobStart) {
                return;
            }
            cleanUserCacheJobStart = true;
            Thread cleanExpireUserThread = new Thread(() -> {
                for (;;) {
                    try {
                        TimeUnit.SECONDS.sleep(cycleSecond);
                    } catch (InterruptedException e) {
                        log.error("", e);
                    }
                    log.debug("start clean userEntity in Context.contextMap Size:{}", userThreadLocalMap.size());
                    List<String> expiredUsers = new ArrayList<>();
                    userThreadLocalMap.entrySet().forEach(en -> {
                        UserEntity user = en.getValue();
                        if (user.getExpiredTime() < System.currentTimeMillis()) {
                            expiredUsers.add(en.getKey());
                        }
                    });
                    expiredUsers.forEach(a -> {
                        userThreadLocalMap.remove(a);
                    });
                    log.debug("end clean userEntity in Context.");
                }
            }, "CleanExpireUserThread");
            cleanExpireUserThread.setDaemon(true);
            log.debug("start CleanExpireUserThread.");
            cleanExpireUserThread.start();
        }

    }

    /**
     * 删除上下文信息
     */
    public static void removeContext() {
        contextThreadLocalMap.remove(Thread.currentThread());
    }

    /**
     * 获取上下文标准的信息
     * 
     * @return
     */
    public static Map<String, Object> getStandard() {
        ThreadLocal<Map<String, Object>> contextMapLocal = contextThreadLocalMap.get(Thread.currentThread());
        if (contextMapLocal == null) {
            return new HashMap<>(0);
        }
        Map<String, Object> contextMap = contextMapLocal.get();
        if (contextMap == null) {
            return new HashMap<>(0);
        }
        Map<String, Object> standardMap = new HashMap<>();
        for (RequestHeaderEnum e : RequestHeaderEnum.values()) {
            standardMap.put(e.getName(), contextMap.get(e.getName()));
        }
        return standardMap;
    }

    /**
     * 将所有的header信息放入上下文
     * 
     * @return
     */
    public static void setStandardContext(HttpServletRequest httpRequest,String ...extraHeaders) {
        Map<String, Object> contextMap = new HashMap<>();
        
        for(String extraHeader: extraHeaders) {
            contextMap.put(extraHeader, httpRequest.getHeader(extraHeader));
        }
        String role = httpRequest.getHeader(RequestHeaderEnum.ROLE.getName());
        if (StringUtils.isNotBlank(role)) {
            contextMap.put(RequestHeaderEnum.ROLE.getName(), role);
        }
        String userId = httpRequest.getHeader(RequestHeaderEnum.USERID.getName());
        if (StringUtils.isNotBlank(userId)) {
            contextMap.put(RequestHeaderEnum.USERID.getName(), userId);
        }
        String sid = httpRequest.getHeader(RequestHeaderEnum.SID.getName());
        if (StringUtils.isNotBlank(sid)) {
            contextMap.put(RequestHeaderEnum.SID.getName(), sid);
        }

        String traceId = httpRequest.getHeader(RequestHeaderEnum.TRACEID.getName());
        // 补充traceId
        if (StringUtils.isBlank(traceId)) {
            traceId = httpRequest.getHeader(RequestHeaderEnum.XREQUESTID.getName());
            if (StringUtils.isBlank(traceId)) {
                traceId = IdGenerator.traceid();
            }
        }
        contextMap.put(LogLabelEnum.TRACEID.getLabel(), traceId);

        String pspanId = httpRequest.getHeader(RequestHeaderEnum.SPANID.getName());
        if (StringUtils.isBlank(pspanId)) {
            pspanId = "0";
        }

        contextMap.put(LogLabelEnum.PSPANID.getLabel(), pspanId);
        contextMap.put(LogLabelEnum.REQUEST_TIME.getLabel(), System.currentTimeMillis() + "");
        contextMap.put(LogLabelEnum.URI.getLabel(), httpRequest.getRequestURI());
        contextMap.put(LogLabelEnum.REMOTEIP.getLabel(), httpRequest.getRemoteAddr());

        putAll(contextMap);
        LogUtils.initTrace(pspanId, traceId);
    }

    public static void setStandardContext(String key, String value) {
        put(key, value);
    }

    public static boolean containsKey(String key) {
        Map<String, Object> localMap = getLocalContextMap();
        return localMap.containsKey(key);
    }

    /**
     * 获取上下文所有信息
     * 
     * @return
     */
    public static Map<String, Object> getAll() {
        ThreadLocal<Map<String, Object>> contextMapLocal = contextThreadLocalMap.get(Thread.currentThread());
        if (contextMapLocal == null) {
            return new HashMap<>(0);
        }
        Map<String, Object> contextMap = contextMapLocal.get();
        if (contextMap == null) {
            return new HashMap<>(0);
        }
        Map<String, Object> standardMap = new HashMap<>();
        standardMap.putAll(contextMap);
        return standardMap;
    }

    /**
     * 将自定义的信息放入上下文
     * 
     * @param map
     */
    public static void putAll(Map<String, Object> map) {
        Map<String, Object> localMap = getLocalContextMap();
        localMap.putAll(map);
    }

    /**
     * 将自定义的信息放入上下文
     * 
     * @param map
     */
    public static void put(String key, String value) {
        Map<String, Object> localMap = getLocalContextMap();
        localMap.put(key, value);
    }

    private static Map<String, Object> getLocalContextMap() {
        ThreadLocal<Map<String, Object>> contextMapLocal = contextThreadLocalMap.get(Thread.currentThread());
        if (contextMapLocal == null) {
            contextMapLocal = new ThreadLocal<>();
        }
        Map<String, Object> contextMap = contextMapLocal.get();
        if (contextMap == null) {
            contextMap = new HashMap<>();
        }
        contextMapLocal.set(contextMap);
        contextThreadLocalMap.put(Thread.currentThread(), contextMapLocal);
        return contextMap;
    }

    /**
     * 获取特定上下文的值
     * 
     * @param key
     * @return
     */
    public static String getStandardContext(String key) {
        ThreadLocal<Map<String, Object>> contextMapLocal = contextThreadLocalMap.get(Thread.currentThread());
        if (contextMapLocal == null) {
            return "";
        }
        Map<String, Object> contextMap = contextMapLocal.get();
        Object val = contextMap.get(key);
        if (val == null) {
            return "";
        }
        return (String) val;
    }

    /**
     * 检查用户是否登录过期
     * 
     * @return
     */
    public static boolean isLoginExpire(String cacheKey) {
        UserEntity localUser = userThreadLocalMap.get(cacheKey);
        if (localUser == null) {
            return true;
        }
        // 用户已过期
        if (localUser.getExpiredTime() < System.currentTimeMillis()) {
            return true;
        }
        // 通过session获取的userid 可能还不存在于context中
        put(RequestHeaderEnum.USERID.getName(), localUser.getUserId());
        return false;
    }

    public static void setUserEntity(String cacheKey, UserEntity userEntity) {
        userThreadLocalMap.putIfAbsent(cacheKey, userEntity);
        put(RequestHeaderEnum.USERID.getName(), userEntity.getUserId());
    }

    /**
     * 检查登录必要参数
     * 
     * @param checkParams
     * @return
     */
    public static boolean loginCheck(RequestHeaderEnum... checkParams) {
        // 默认只检查sid
        if (checkParams.length == 0) {
            return StringUtils.isNotBlank(getStandardContext(RequestHeaderEnum.SID.getName()));
        }
        for (RequestHeaderEnum h : checkParams) {
            if (StringUtils.isBlank(getStandardContext(h.getName()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取用户登录后的上下文信息<br/>
     * 返回不会为null
     * 
     * @return
     */
    public static UserEntity getUserEntity() {
        String userId = getStandardContext(RequestHeaderEnum.USERID.getName());
        String role = getStandardContext(RequestHeaderEnum.ROLE.getName());
        UserEntity user = new UserEntity();
        user.setRole(role);
        user.setUserId(userId);
        return user;
    }

    public static String contextInfo() {
        return "contextInfo: contextThreadLocalMap->"
                + contextThreadLocalMap.size()
                + "; userThreadLocalMap->" + userThreadLocalMap.size();
    }

}
