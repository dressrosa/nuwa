/**
 * copyright com.xiaoyu
 */
package com.xiaoyu.nuwa.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

/**
 * 加密方法
 * 
 * @author xiaoyu
 *
 */
public class EncryptUtil {

    private static final int xor = 1;

    public static String xorEncrypt(String origin) {
        return doCrypt(origin, true);
    }

    public static String xorDecrypt(String encrypt) {
        return doCrypt(encrypt, false);
    }

    private static String doCrypt(String value, boolean isEncrypt) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        byte[] arr = isEncrypt ? value.getBytes() : Base64.decodeBase64(value);
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (byte) (arr[i] ^ xor);
        }
        return isEncrypt ? Base64.encodeBase64String(arr) : new String(arr);
    }

    public static String xorCrypt(String source, String key, boolean isEncrypt) {
        byte[] arr = isEncrypt ? source.getBytes() : Base64.decodeBase64(source);
        byte[] keyArr = key.getBytes();
        if (isEncrypt) {
            for (int i = 0; i < arr.length; i++) {
                for (int j = 0; j < keyArr.length; j++) {
                    arr[i] = (byte) (arr[i] ^ keyArr[j]);
                }
            }
        } else {
            for (int i = 0; i < arr.length; i++) {
                for (int j = keyArr.length - 1; j >= 0; j--) {
                    arr[i] = (byte) (arr[i] ^ keyArr[j]);
                }
            }
        }
        return isEncrypt ? Base64.encodeBase64String(arr) : new String(arr);
    }

}
