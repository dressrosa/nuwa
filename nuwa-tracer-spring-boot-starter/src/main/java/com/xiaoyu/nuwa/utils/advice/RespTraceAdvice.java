package com.xiaoyu.nuwa.utils.advice;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.xiaoyu.nuwa.utils.ContentTypeUtil;
import com.xiaoyu.nuwa.utils.context.ContextUtil;
import com.xiaoyu.nuwa.utils.enums.LogLabelEnum;
import com.xiaoyu.nuwa.utils.response.BaseResponseMapper;

/**
 * 返回体拦截, 处理返回码
 * 
 * @author xiaoyu
 *
 */

@Configuration
@ControllerAdvice
@Order(0)
public class RespTraceAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
            ServerHttpResponse response) {
        HttpServletRequest req = ((ServletServerHttpRequest) request).getServletRequest();
        // 这里不处理protobuf
        if (ContentTypeUtil.isProtoRequest(req.getContentType())) {
            return body;
        }
        if (body instanceof BaseResponseMapper) {
            BaseResponseMapper mapper = (BaseResponseMapper) body;
            ContextUtil.setStandardContext(LogLabelEnum.RETURNCODE.getLabel(), mapper.returnCode());
        }
        return body;
    }

}
