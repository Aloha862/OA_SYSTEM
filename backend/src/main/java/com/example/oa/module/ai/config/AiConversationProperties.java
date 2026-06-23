package com.example.oa.module.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "oa.ai.conversation")
public class AiConversationProperties {
    private int maxMessages = 20;
    private int maxCharacters = 12_000;
    private Duration cacheTtl = Duration.ofMinutes(30);
}
