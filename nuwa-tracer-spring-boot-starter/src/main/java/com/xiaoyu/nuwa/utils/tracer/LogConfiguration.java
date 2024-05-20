package com.xiaoyu.nuwa.utils.tracer;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.xiaoyu.nuwa.utils.LogUtils;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "nuwa.log")
@Getter
public class LogConfiguration {

    private List<String> filters;

    private Desensitize desensitize;

    public void setFilters(List<String> f) {
        this.filters = f;
        if (f != null && !f.isEmpty()) {
            LogUtils.fillNoLogUriFilters(f);
        }
    }

    public void setDesensitize(Desensitize d) {
        this.desensitize = d;
        if (d != null && d.open && StringUtils.isNotBlank(d.getKeywords())) {
            List<String> keywordList = new ArrayList<>();
            for (String k : d.getKeywords().split(",")) {
                if (StringUtils.isNotBlank(k)) {
                    // 拼上特征后缀
                    keywordList.add(k + "\"");
                }
            }
            if (!keywordList.isEmpty()) {
                // 补充进logutils当中
                LogUtils.fillDesensitize(keywordList);
            }
        }

    }

    @Getter
    @Setter
    private static final class Desensitize {
        private boolean open;
        private String keywords;
    }
}