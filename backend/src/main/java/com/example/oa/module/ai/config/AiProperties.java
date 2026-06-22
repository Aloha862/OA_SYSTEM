package com.example.oa.module.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oa.ai")
public class AiProperties {

    private String provider = "mock";
    private Tongyi tongyi = new Tongyi();

    @Data
    public static class Tongyi {
        private String apiKey;
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        private String model = "qwen-plus";
        private Double temperature = 0.3;
        private Integer maxTokens = 2048;
        private Boolean curlFallback = true;
        private String curlCommand = "curl.exe";
    }
}
