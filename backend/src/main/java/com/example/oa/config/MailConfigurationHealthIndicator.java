package com.example.oa.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component("mailConfigurationHealthIndicator")
public class MailConfigurationHealthIndicator implements HealthIndicator {
    @Value("${spring.mail.host:}")
    private String host;
    @Value("${spring.mail.username:}")
    private String username;
    @Value("${spring.mail.password:}")
    private String password;

    @Override
    public Health health() {
        boolean configured = StringUtils.hasText(host) && StringUtils.hasText(username) && StringUtils.hasText(password);
        return configured
                ? Health.up().withDetail("configured", true).build()
                : Health.down().withDetail("configured", false).withDetail("reason", "SMTP环境变量未完整配置").build();
    }
}
