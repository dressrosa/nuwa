package com.xiaoyu.nuwa.utils.filter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.springframework.util.StreamUtils;

import com.xiaoyu.nuwa.utils.GzipUtils;

public class ReadableRequestWrapper extends HttpServletRequestWrapper {
    private byte[] body;

    public ReadableRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        body = StreamUtils.copyToByteArray(request.getInputStream());
    }

    /**
     * 对请求数据解gzip
     * 
     */
    public void uncompressGzip() {
        body = GzipUtils.uncompress(body);
    }

    public boolean isEmptyBody() {
        return body == null || body.length == 0;
    }

    public byte[] getByteBody() {
        return body;
    }

    public String getBody() {
        if (body == null) {
            return null;
        }
        return new String(body);
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        final ByteArrayInputStream bais = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }

            @Override
            public int read() throws IOException {
                return bais.read();
            }
        };
    }
}
