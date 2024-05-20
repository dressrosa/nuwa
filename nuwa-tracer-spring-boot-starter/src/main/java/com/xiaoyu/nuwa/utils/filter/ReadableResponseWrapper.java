package com.xiaoyu.nuwa.utils.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.springframework.util.StreamUtils;

import com.xiaoyu.nuwa.utils.GzipUtils;

public class ReadableResponseWrapper extends HttpServletResponseWrapper {

    private ByteArrayOutputStream baos = null;

    private boolean needGzip;

    private boolean isFlush;

    private byte[] bytes;

    public ReadableResponseWrapper(HttpServletResponse response) {
        super(response);
        baos = new ByteArrayOutputStream();
    }

    /**
     * 对返回数据进行gzip
     * 
     * @param gzip
     */
    public void setNeedGzip(boolean gzip) {
        needGzip = gzip;
    }

    public String getBody() {
        return new String(getBytes());
    }

    /**
     * 获取原始bytes
     * 
     * @return
     */
    public byte[] getBytes() {
        if (!isFlush) {
            bytes = baos.toByteArray();
        }
        return bytes;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return new ServletOutputStream() {

            @Override
            public void write(int b) throws IOException {
                baos.write(b);
            }

            @Override
            public void setWriteListener(WriteListener listener) {
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void flush() throws IOException {
                if (isFlush) {
                    return;
                }
                byte[] respBytes = getBytes();
                if (needGzip) {
                    respBytes = GzipUtils.compress(respBytes);
                }
                StreamUtils.copy(respBytes, ReadableResponseWrapper.this.getResponse().getOutputStream());
                isFlush = true;
            }

        };
    }

}