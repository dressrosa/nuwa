package com.xiaoyu.nuwa.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.net.URIBuilder;

import com.alibaba.fastjson2.JSON;
import com.xiaoyu.nuwa.utils.context.ContextUtil;
import com.xiaoyu.nuwa.utils.enums.LogLabelEnum;
import com.xiaoyu.nuwa.utils.exception.BizNoLogException;
import com.xiaoyu.nuwa.utils.exception.CommonException;
import com.xiaoyu.nuwa.utils.response.BaseResponseCode;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * http请求的封装
 * 
 * @author xiaoyu
 *
 */
@Slf4j
public class HttpFluentUtils {

    private static final Object PoolLock = new Object();

    private static PoolingHttpClientConnectionManager CONPOOL = null;

    /**
     * 创建一个http请求
     * 
     * @param host
     * @return
     */
    public static HttpFluent create(String host) {
        if (StringUtils.isBlank(host) || !host.startsWith("http")) {
            throw new CommonException(BaseResponseCode.Args_Error, "host not correct");
        }
        return new HttpFluent(host);
    }

    public static enum Method {
        GET, POST, PROTO, TEXT, POSTJSON;
    }

    public static class HttpFluent {
        private String host;
        private String path;
        private long connectTimeout;
        private long socketTimeout;
        private Map<String, String> headers;
        private Boolean logReq;
        private Boolean logResp;
        private Method method;
        private ErrResponseHandler handler;
        // 代理信息
        private HttpHost proxy;

        private ContentType contentType;

        private boolean pool;
        // 设置最大连接数
        private int maxTotal;
        // 设置每个路由的最大连接数
        private int maxPerRoute;

        private HttpFluent() {

        }

        private HttpFluent(String host) {
            this.host = host;
            logReq = true;
            logResp = true;
            handler = new DefaultErrResponseHandler();
            connectTimeout = 1000L;
            socketTimeout = 1000L;
        }

        /**
         * 开启连接池 ,默认maxTotal=200,maxPerRoute = 20
         * 
         * @param isPool
         * @return
         */
        public HttpFluent pool(boolean isPool) {
            this.pool = isPool;
            this.maxTotal = 200;
            this.maxPerRoute = 20;
            return this;
        }

        public HttpFluent customPool(int maxTotal, int maxPerRoute) {
            this.maxPerRoute = maxPerRoute;
            this.maxTotal = maxTotal;
            return this;
        }

        public HttpFluent connectTimeout(long connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public HttpFluent socketTimeout(long socketTimeout) {
            this.socketTimeout = socketTimeout;
            return this;
        }

        public HttpFluent path(String path) {
            this.path = path;
            return this;
        }

        public HttpFluent logReq(boolean isLog) {
            this.logReq = isLog;
            return this;
        }

        public HttpFluent logResp(boolean isLog) {
            this.logResp = isLog;
            return this;
        }

        public HttpFluent headers(Map<String, String> headers) {
            if (headers != null) {
                this.headers = headers;
            }
            return this;
        }

        public HttpFluent errRespHandler(ErrResponseHandler handler) {
            if (handler != null) {
                this.handler = handler;
            }
            return this;
        }

        public HttpFluent proxy(HttpHost proxy) {
            this.proxy = proxy;
            return this;
        }

        /**
         * @param params
         * @param contentType
         * @return
         */
        public byte[] post(byte[] params, ContentType contentType) {
            method = Method.POST;
            this.contentType = contentType;
            return doExecute(this, params);
        }

        /**
         * json请求
         * 
         * @param paramsMap
         * @return
         */
        public String postJson(Map<String, Object> paramsMap) {
            method = Method.POSTJSON;
            byte[] ret = doExecute(this, paramsMap);
            if (ret == null) {
                return null;
            }
            return new String(ret);
        }

        /**
         * text/plain请求
         * 
         * @param params
         * @return
         */
        public String postText(String params) {
            method = Method.TEXT;
            byte[] ret = doExecute(this, params);
            if (ret == null) {
                return null;
            }
            return new String(ret);
        }

        /**
         * json请求
         * 
         * @param object
         * @return
         */
        public String postJson(Object object) {
            method = Method.POSTJSON;
            byte[] ret = doExecute(this, object);
            if (ret == null) {
                return null;
            }
            return new String(ret);
        }

        public byte[] postProto(byte[] bytes) {
            method = Method.PROTO;
            this.logReq = false;
            this.logResp = false;
            return doExecute(this, bytes);
        }

        /**
         * get请求
         * 
         * @param paramsMap
         * @return
         */
        public String get(Map<String, Object> paramsMap) {
            byte[] ret = getRaw(paramsMap);
            if (ret == null) {
                return null;
            }
            return new String(ret);
        }

        /**
         * get请求
         * 
         */
        public String get() {
            method = Method.GET;
            return get(null);
        }

        /**
         * @param paramsMap
         * @return byte[]
         */
        public byte[] getRaw(Map<String, Object> paramsMap) {
            method = Method.GET;
            byte[] ret = doExecute(this, paramsMap);
            if (ret == null) {
                return null;
            }
            return ret;
        }

    }

    public static interface ErrResponseHandler {
        void handleErrResponse(ErrResponse response);
    }

    public static class DefaultErrResponseHandler implements ErrResponseHandler {

        @Override
        public void handleErrResponse(ErrResponse response) {
            if (response.getHttpStatus() == 404) {
                throw new BizNoLogException(BaseResponseCode.Connect_404, "404");
            }
        }

    }

    @Getter
    @Setter
    public static class ErrResponse {
        byte[] result;
        int httpStatus;

        public ErrResponse(byte[] result, int httpStatus) {
            this.result = result;
            this.httpStatus = httpStatus;
        }
    }

    /**
     * 执行并获取http请求结果
     * 
     * @param httpFluent
     * @param paramsObject
     * @return
     */
    private static byte[] doExecute(HttpFluent httpFluent, Object paramsObject) {
        if (StringUtils.isBlank(httpFluent.host)) {
            throw new CommonException(BaseResponseCode.Args_Error, "params error");
        }

        ConnectionConfig conConfig = ConnectionConfig.custom()
                .setConnectTimeout(httpFluent.connectTimeout, TimeUnit.MILLISECONDS)
                .build();
        SocketConfig soConfig = SocketConfig.custom()
                .setSoTimeout(Long.valueOf(httpFluent.socketTimeout).intValue(), TimeUnit.MILLISECONDS).build();
        HttpClientBuilder clientBuilder = HttpClients.custom();
        if (httpFluent.proxy != null) {
            clientBuilder.setProxy(httpFluent.proxy);
        }
        if (httpFluent.pool) {
            PoolingHttpClientConnectionManager pool = CONPOOL;
            if (pool == null) {
                synchronized (PoolLock) {
                    if (pool == null) {
                        pool = new PoolingHttpClientConnectionManager();
                        pool.setMaxTotal(httpFluent.maxTotal);
                        pool.setDefaultMaxPerRoute(httpFluent.maxPerRoute);
                        pool.setDefaultConnectionConfig(conConfig);
                        pool.setDefaultSocketConfig(soConfig);
                        CONPOOL = pool;
                    }
                }
            }
            clientBuilder.setConnectionManagerShared(true);
            // clientBuilder.evictIdleConnections(TimeValue.ofSeconds(120));
            clientBuilder.setConnectionManager(pool);
        } else {
            BasicHttpClientConnectionManager con = new BasicHttpClientConnectionManager();
            con.setConnectionConfig(conConfig);
            con.setSocketConfig(soConfig);
            clientBuilder.setConnectionManager(con);
        }

        BasicClassicHttpRequest httpRequest = null;
        String uri = httpFluent.host;
        if (StringUtils.isNotBlank(httpFluent.path)) {
            if (uri.endsWith("/") && httpFluent.path.startsWith("/")) {
                uri = uri + httpFluent.path.replaceFirst("/", "");
            } else if (!uri.endsWith("/") && !httpFluent.path.startsWith("/")) {
                uri = uri + "/" + httpFluent.path;
            } else {
                uri += httpFluent.path;
            }
        }
        switch (httpFluent.method) {
        case GET:
            try {
                URIBuilder uriBuilder = new URIBuilder(new URI(uri));
                if (paramsObject instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> paramsMap = (Map<String, Object>) paramsObject;
                    paramsMap.entrySet().forEach(en -> {
                        uriBuilder.addParameter(en.getKey(), String.valueOf(en.getValue()));
                    });
                }
                httpRequest = new HttpGet(uriBuilder.build());
            } catch (URISyntaxException e) {
                log.error("", e);
            }
            break;
        case POST:
            httpRequest = new HttpPost(uri);
            if (paramsObject != null) {
                httpRequest.setEntity(new ByteArrayEntity((byte[]) paramsObject, httpFluent.contentType));
            }
            break;
        case POSTJSON:
            httpRequest = new HttpPost(uri);
            if (paramsObject != null) {
                httpRequest.setEntity(new StringEntity(JSON.toJSONString(paramsObject), ContentType.APPLICATION_JSON));
            }
            break;
        case PROTO:
            httpRequest = new HttpPost(uri);
            if (paramsObject != null) {
                InputStreamEntity en = new InputStreamEntity(new ByteArrayInputStream((byte[]) paramsObject),
                        ContentType.APPLICATION_OCTET_STREAM);
                httpRequest.setEntity(en);
            }
            httpRequest.addHeader("Content-Type", "application/x-protobuf");
            break;
        case TEXT:
            httpRequest = new HttpPost(uri);
            if (paramsObject != null) {
                httpRequest.setEntity(new StringEntity(String.valueOf(paramsObject), ContentType.TEXT_PLAIN));
            }
            break;
        default:
            break;
        }
        // 拦截器
        clientBuilder.addRequestInterceptorFirst(requestHandler(paramsObject, httpFluent));
        CloseableHttpClient client = null;
        long startTime = System.currentTimeMillis();
        Exception err = null;
        try {
            client = clientBuilder.build();
            return client.execute(httpRequest, responseHandler(httpRequest, httpFluent, startTime));
        } catch (ConnectTimeoutException | ConnectException e) {
            err = e;
            BizLogUtils.bizWarnLog("outservice-connect", e);
            throw new CommonException(BaseResponseCode.Connect_Error, "connect out service error");
        } catch (SocketTimeoutException e) {
            err = e;
            BizLogUtils.bizWarnLog("outservice-socket", e);
            throw new CommonException(BaseResponseCode.Socket_Error, "socket out service error");
        } catch (CommonException e) {
            err = e;
            throw e;
        } catch (Exception e) {
            err = e;
            BizLogUtils.bizErrorLog("outService-err", e);
            throw new CommonException(BaseResponseCode.Outer_Error, "request out service error");
        } finally {
            if (err != null) {
                LogUtils.httpOut(httpRequest, err.getMessage(), getCost(startTime),
                        HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
            if (!httpFluent.pool) {
                if (client != null) {
                    try {
                        client.close();
                    } catch (IOException e) {
                        log.error("", e);
                    }
                }
            }
        }
    }

    // 请求信息处理 日志
    private static HttpRequestInterceptor requestHandler(Object paramsObject, HttpFluent httpFluent) {
        return new HttpRequestInterceptor() {
            @Override
            public void process(HttpRequest request, EntityDetails entity, HttpContext context)
                    throws HttpException, IOException {
                // 遍历header
                fillHeader(request, httpFluent.headers);
                if (httpFluent.logReq) {
                    LogUtils.httpIn(request, paramsObject);
                } else {
                    LogUtils.httpIn(request, "");
                }
            }
        };
    }

    // 返回信息处理 日志
    private static HttpClientResponseHandler<byte[]> responseHandler(BasicClassicHttpRequest httpRequest,
            HttpFluent httpFluent, long startTime) {
        return new HttpClientResponseHandler<byte[]>() {
            @Override
            public byte[] handleResponse(ClassicHttpResponse response) throws HttpException, IOException {
                byte[] bytes = null;
                int status = response.getCode();
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    bytes = EntityUtils.toByteArray(entity);
                }
                // 打日志
                long cost = getCost(startTime);
                if (httpFluent.logResp) {
                    LogUtils.httpOut(httpRequest, new String(bytes, Charset.defaultCharset()), cost, status);
                } else {
                    LogUtils.httpOut(httpRequest, "", cost, status);
                }
                if (status < 200 || status >= 300) {
                    httpFluent.handler.handleErrResponse(new ErrResponse(bytes, status));
                    return null;
                }
                return bytes;
            }
        };
    }

    private static long getCost(long startTime) {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * 将trace及header信息放入请求头中
     * 
     * @param request
     * @param headers
     */
    private static void fillHeader(HttpRequest request, Map<String, String> headers) {
        if (headers != null) {
            headers.entrySet().forEach(en -> {
                request.addHeader(en.getKey(), en.getValue());
            });
        }
        request.addHeader(LogLabelEnum.SPANID.getLabel(),
                ContextUtil.getStandardContext(LogLabelEnum.SPANID.getLabel()));
        request.addHeader(LogLabelEnum.TRACEID.getLabel(),
                ContextUtil.getStandardContext(LogLabelEnum.TRACEID.getLabel()));
    }
}
