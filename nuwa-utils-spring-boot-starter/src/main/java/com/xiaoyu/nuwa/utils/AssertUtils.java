package com.xiaoyu.nuwa.utils;

import com.xiaoyu.nuwa.utils.exception.BizNoLogException;
import com.xiaoyu.nuwa.utils.response.BaseResponseCode;

public class AssertUtils {

    public static void notNull(Object object, String msg) {
        if (object == null) {
            throw new BizNoLogException(BaseResponseCode.Args_Error, msg);
        }
    }

    public static void notBlank(String str, String msg) {
        if (str == null || str.equals("")) {
            throw new BizNoLogException(BaseResponseCode.Args_Error, msg);
        }
    }

    public static void isTrue(boolean flag, String msg) {
        if (!flag) {
            throw new BizNoLogException(BaseResponseCode.Args_Error, msg);
        }
    }
}
