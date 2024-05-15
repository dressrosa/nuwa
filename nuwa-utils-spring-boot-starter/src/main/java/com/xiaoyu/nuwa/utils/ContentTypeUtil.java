/**
 * copyright com.xiaoyu
 */
package com.xiaoyu.nuwa.utils;

import org.apache.commons.lang3.StringUtils;

public class ContentTypeUtil {

    public static boolean isProtoRequest(String contentType) {
        if (StringUtils.isBlank(contentType)) {
            return false;
        }
        return contentType.contains("x-protobuf") || contentType.contains("application/pb");
    }

    public static boolean isApplicationJson(String contentType) {
        if (StringUtils.isBlank(contentType)) {
            return false;
        }
        return contentType.contains("application/json");
    }

    public static boolean isTextPlain(String contentType) {
        if (StringUtils.isBlank(contentType)) {
            return false;
        }
        return contentType.contains("text/plain");
    }

}
