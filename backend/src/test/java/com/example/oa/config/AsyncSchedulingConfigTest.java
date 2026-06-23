package com.example.oa.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncSchedulingConfigTest {

    @Test
    void createsBoundedDedicatedSchedulerPool() {
        ThreadPoolTaskScheduler scheduler = new AsyncSchedulingConfig().taskScheduler(6);
        scheduler.initialize();
        try {
            assertThat(scheduler.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(6);
            assertThat(scheduler.getThreadNamePrefix()).isEqualTo("oa-scheduled-");
        } finally {
            scheduler.shutdown();
        }
    }
}
