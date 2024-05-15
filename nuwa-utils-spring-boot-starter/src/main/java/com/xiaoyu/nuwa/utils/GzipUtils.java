package com.xiaoyu.nuwa.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GzipUtils {

    public static byte[] compress(byte[] data) {
        if (data == null || data.length == 0) {
            return data;
        }
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            GZIPOutputStream gZipStream = new GZIPOutputStream(outputStream);
            gZipStream.write(data);
            gZipStream.close();
            outputStream.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }

    public static byte[] uncompress(byte[] data) {
        if (data == null || data.length == 0) {
            return data;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GZIPInputStream ungzip = new GZIPInputStream(new ByteArrayInputStream(data));
            byte[] buffer = new byte[1024];
            int n;
            while ((n = ungzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
            ungzip.close();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
            log.error("", e);
        }
        return null;
    }

}
