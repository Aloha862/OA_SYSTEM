package com.example.oa.security.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oa.jwt")
public class JwtProperties {

    private String secret;
    private long expiration;
}
