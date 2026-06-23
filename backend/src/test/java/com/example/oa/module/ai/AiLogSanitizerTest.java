package com.example.oa.module.ai;

import com.example.oa.module.ai.util.AiLogSanitizer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiLogSanitizerTest {
    @Test
    void requestIsRedactedAndTruncated() {
        String value = "Bearer secret-token admin@example.com 13800138000 " + "x".repeat(5000);
        String sanitized = AiLogSanitizer.request(value);
        assertThat(sanitized).contains("Bearer [token]", "[email]", "[phone]", "[truncated]");
        assertThat(sanitized).doesNotContain("secret-token", "admin@example.com", "13800138000");
    }
}
