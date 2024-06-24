[TOC]  
# 简介
提供底层基础功能插件,统一使用规范,降低使用门槛,可见即可用.

# 整体结构
- nuwa-utils-spring-boot-starter: 公共工具
- nuwa-tracer-spring-boot-starter: 提供请求拦截相关功能
- nuwa-protobuf-spring-boot-starter: 扩展支持protobuf功能
- nuwa-redis-spring-boot-starter: 提供redis相关功能
- nuwa-sso-spring-boot-starter: 提供sso统一登录相关功能

## nuwa-utils-spring-boot-starter
nuwa-utils提供一系列日常开发中的基础功能.
## 快速使用
```xml
 <dependency>
            <groupId>com.iflytek.ime</groupId>
            <artifactId>nuwa-utils-spring-boot-starter</artifactId>
            <version>0.0.7-SNAPSHOT</version>
 </dependency>
```
### 限流
采用guava的内存限流机制,针对请求header中的参数进行限流
```yaml
# open 是否开启 默认不开启 
# total 单机总限流 默认1000
nuwa:
  rate:
    open: true
    total: 1000
```
代码中添加注解@RateLimit
 ```java
 //示例
    //通过请求头中userId限流,qps限制为2
    @RateLimit(limitHeader = "userId", limit = 2)
    @PostMapping("/api/xxx")
    public Object updateUser(@RequestBody Object param) {
        return null;
    }
```

### 异常类
提供通用异常类CommonException,异常基于
 ERROR , WARN, INFO, DEBUG, TRACE, NO_LOG, NO_STACK七大类型.
 nuwa提供常用异常类.
- BizInfoLogException
- BizNoLogException
- BizNoStackException
- BizWarnException
开发者可根据不同异常进行捕获,再根据实际需求对日志进行错误级别处理.
基于错误种类,nuwa提供参数判断工具类

```java

import com.iflytek.ime.nuwa.utils.AssertUtils

public void check(String userName) {
 	AssertUtils.notBlank(userName, "用户名不能为空");
}

```

### 线程池
nuwa提供通用的线程池实现和管理.
1. 提供ExecutorRTask,ExecutorCTask实现,分表代表runnable和callable,由此支持上下文信息的父子线程传递.
2. 支持动态修改线程池参数
3. 支持线程池任务中断
```java
//示例
 CommonThreadPoolExecutor executor = CommonExecutors.getExecutor(executorName, bizType).execute(new ExecutorRTask() {
            @Override
            public void doRun() {
                //do something
            }
        });

```
### http请求
nuwa提供统一的http请求工具HttpFluentUtils.\
1. 支持GET/POST请求,提供丰富的功能定义
2. 支持protobuf格式请求
3. 支持上下文链路追踪
4. 输出标准化日志
```java
//示例
     String response = HttpFluentUtils.create("http://xxxx")
     .path("/api/userinfo")
     .connectTimeout(1000L)
     .socketTimeout(1000L)
     .get();
```

### 缓存
nuwa提供内存缓存工具类 SimpleLocalCacheUtils
1. 支持泛型,使用简单
2. 缓存过期不自动删除,不适用变化大的缓存(提供主动删除方法).
```java
//示例
     List<CityInfo> cache = SimpleLocalCacheUtils.getCache("city_info");
    List<CityInfo> cityList = new ArrayList<>();
     SimpleLocalCacheUtils.setCache("city_info", cityList);
```

### 其他工具类
 - EncryptUtil 异或加解密
 - GzipUtils gzip解压缩
 - IdGenerator 分布式唯一id
 - TimeUtils 时间处理

### 上下文
上下文包括两部分信息(获取上下文需启用nuwa-tracer)\
一是请求头里面所有的信息(注:过滤了header里面过长的数据)\
二是用户信息(用户信息可根据实际情况自行封装).
nuwa提供统一的处理工具类.\

```java
import com.iflytek.ime.nuwa.utils.context.ContextUtil

ContextUtil.setStandardContext("hello", "world");

String value = ContextUtil.getStandardContext("hello");
```
- 用户信息  
nuwa本身不提供用户信息的获取,用户可通过注解@UserInfoRequired,自行开发过滤器来实现获取用户信息并放入上下文中.
```java
//示例 通过添加注解@UserInfoRequired标识必须传递用户信息
    @PostMapping(value = "/api/v1/xxx")
    @UserInfoRequired
    public Object userList(@RequestBody UserRequest req) {
    
    }
```

```java
//设置用户信息于上下文中
 ContextUtil.setUserEntity(cacheKey, userEntity);
 //获取用户上下文信息
 UserEntity user = ContextUtil.getUserEntity();
```
默认用户的上下文信息是永不过期的,开发者可自行处理(设置expiredTime值),调用下面的方法,nuwa会定时清理过期(超过expiredTime时刻)用户信息

 ```java
 // cycleSecond 定时任务周期
 ContextUtil.startUserCleanTreadIfNot(cycleSecond);
```

# nuwa-tracer-spring-boot-starter
tracer提供全局请求拦截和日志处理,依赖nuwa-utils
## 快速使用
```xml
 <dependency>
            <groupId>com.iflytek.ime</groupId>
            <artifactId>nuwa-tracer-spring-boot-starter</artifactId>
            <version>0.0.7-SNAPSHOT</version>
 </dependency>
```

## 日志处理
### 日志格式
nuwa提供标准化日志定义.格式如下

```
xml配置的格式:
# [日志级别][时间][类名:行号][上游spanid][spanid][traceid]开发打印的日志.
 <pattern>[%p][%d{yyyy-MM-dd HH:mm:ss.SSS}][%logger{1.}:%L]%X{pspanid}%X{spanid}%X{traceid}%m%n</pattern>

实际打印如下:
[INFO][2023-10-23 17:21:48.058][com.iflytek.ime.photo.common.tracer.utils.LogUtils:132][d4eb41c0d5cbf80577b37][923264071472513024][ebe19dd4eb41c0d5cbf80577b3765b6d]_msg=com_request_out||uri=/photo/api/v1/model/callback||remoteip=192.180.196.64||cost=2||body={"code":"100500","count":0,"message":"system error"}
```
对于开发打印的日志,基于以下标签进行格式约定.
```text
标签名=标签值||标签名=标签值||标签名=标签值
```
固定标签如下:

|名称   |  释义 |
| ------------ | ------------ |
|_msg   |  标识日志的类别. 具体如下 com_request_in:外部请求 com_request_out:外部请求的返回 http_request_in: 请求外部服务 http_request_out: 请求外部服务的返回|
|uri   |  请求路径|
|remoteip   |  请求ip或域名|
|cost   |  请求耗时(ms)|
|return_code   |  请求返回码|
|http_status   |  http状态码|
|body   |  请求体或返回体|
|params |  get请求的参数|
|query  |  url后拼接的参数|


开发者可根据此格式自行定义标签.nuwa提供类似功能的工具类,自定义了event标签.
```java
 BizLogUtils.bizWarnLog("userEvent", "get userInfo error xxxxxxx" );
```

### 接口请求/返回日志
nuwa提供统一请求返回日志处理
1. 默认对所有GET请求以及POST请求中的application/json和text/plain进行日志输出.
2. 日志输出默认拦截所有请求,可自定义配置,不拦截的请求不会输出日志

```yaml
# patterns拦截指定的请求url模版,支持spring通配符
# excludes 指定url不拦截.
# contentTypes 在默认拦截请求类型的基础上加入用户自定义的请求类型
# cors 是否支持跨域
nuwa:
  request:
    patterns:
	  - /api/v1/hello
	  - /api/v2/**
    excludes:
    -/static/**
    contentTypes:
    - application/hello
    cors: true
```

### 日志过滤/脱敏
对于一些返回数据过大,又不影响日志排查的,可选择不打印返回体.  
(注: 与上面的请求request.excludes的区别在于,request.excludes不经过任何过滤器,
而log.filter只是不打印返回报文.)
```yaml
# filters 指定一些请求不打印返回体,支持spring通配符
# desensitize 是否对日志进行脱敏处理,比如身份证等,只针对json类报文
# 
nuwa:
  log:
    filters:
	  -/xx/api/v1/xxxx
	  -/xx/api/v1/xxxx
    desensitize:
      open: false #是否开启 默认关闭
      keywords: userName,password,email #以英文逗号分隔,表示json返回报文中的key

```

### 启用日志上传
默认日志会开启本地端口12345进行udp消息传输,按标准化格式进行日志发送.
开发者可安装logAgent,让日志进行上传远端日志服务器.


## nuwa-protobuf-spring-boot-starter
对于protobuf格式进行交互的,可通过引入此插件进行请求信息处理.依赖nuwa-utils
 ### 快速使用
```xml
 <dependency>
            <groupId>com.iflytek.ime</groupId>
            <artifactId>nuwa-protobuf-spring-boot-starter</artifactId>
            <version>0.0.7-SNAPSHOT</version>
 </dependency>

```
对于需要加解密或解压缩的请求,可结合nuwa-tracer使用.
```java
@WebFilter(filterName = "accessFilter", urlPatterns = { "/api/*", })
public class AccessFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        ReadableRequestWrapper requestWrapper = null;
        if (request instanceof ReadableRequestWrapper) {
            requestWrapper = (ReadableRequestWrapper) request;
        } else {
            requestWrapper = new ReadableRequestWrapper((HttpServletRequest) request);
        }
        ReadableResponseWrapper respWrapper = null;
        if (response instanceof ReadableResponseWrapper) {
            respWrapper = (ReadableResponseWrapper) response;
        } else {
            respWrapper = new ReadableResponseWrapper((HttpServletResponse) response);
        }
        // 对请求进行解压缩
        if (ContentTypeUtil.isProtoRequest(request.getContentType())) {
            requestWrapper.uncompressGzip();
            respWrapper.setNeedGzip(true);
        }
        chain.doFilter(requestWrapper, respWrapper);
    }

}
```

## nuwa-redis-spring-boot-starter
 ### 快速使用
 ```xml
 <dependency>
            <groupId>com.iflytek.ime</groupId>
            <artifactId>nuwa-redis-spring-boot-starter</artifactId>
            <version>0.0.2-SNAPSHOT</version>
 </dependency>
```
配置:
 ```yaml
nuwa:
  redis:
    configs:
     - name: default
       open: true
       host: 10.100.109.20:18104,10.100.109.21:18104
       masterName: msgcenter
       db: 8
       maxIdle: 10
       minIdle: 3
       maxWaitMillis: 1000
       testOnBorrow: true
       testOnReturn: true
     - name: user
       open: true
       host: 172.22.23.37:8200
       masterName:
       db: 8
       maxIdle: 10
       minIdle: 3
       maxWaitMillis: 1000
       testOnBorrow: true
       testOnReturn: true
```
|名称   |  释义 |
| ------------ | ------------ |
|name   |  redis名称, 用于区分多个实例.标注主使用的redis为default|
|open   | 是否开启|
|host   |  redis地址|
|masterName   | 哨兵模式中的master,不填则为单机模式|
|db   |  默认为0|
1. nuwa提供JedisUtils供开发者使用,默认使用default的redis,多实例情况下开发者可根据name来获取不同的redis.\
(注:nuwa中jedis会延迟初始化,因此使用@PostConstruct时,此时jedis还未初始化.)
```java
public static Jedis getRedis(String name)
```
2. nuwa提供RedisLock来实现分布式锁,默认使用default实例实现.
```java
 RedisLock lock = RedisLock.getLock("hello");
  try {
   if (lock.lock(second)) {
    // do something
      }
  } finally {
     lock.unlock();
   }
```

## nuwa-sso-spring-boot-starter
对于管理后台需要接入集团sso登录的,可通过引入此插件sso登录拦截处理.依赖nuwa-utils
 ### 快速使用
```xml
 <dependency>
            <groupId>com.iflytek.ime</groupId>
            <artifactId>nuwa-sso-spring-boot-starter</artifactId>
            <version>0.0.7-SNAPSHOT</version>
 </dependency>

```
配置使用

```yaml
nuwa:
  sso:
    #是否开启 默认关闭
    open: true 
    # 指定请求需要进行登录拦截 支持spring通配符
    patterns: 
    -/api/v1/xxxx
    -/api/v2/**
    #登录成功后回跳的页面链接
    jump: http://xxx/page/main 
    #单位:秒 默认6h 登录成功后按userId进行缓存
    cacheSecond: 3600   

```
1. 对于首次未登录的请求,会在以httpstatus=401返回给前端,并在header中返回redirectUrl,开发人员自行处理重定向.
2. 对于拦截登录的请求,登录后的ssoUserId,会通过header返回给前端,开发人员可自行处理
3. 单独引入nuwa-sso只会拦截登录信息,如果需要将登录后信息传入业务中,
需要配合nuwa-tracer共同使用.

