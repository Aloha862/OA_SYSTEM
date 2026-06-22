package com.example.oa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oa.file")
public class FileProperties {

    private String uploadPath;
    private String accessPrefix;
}
