package com.example.oa.common.constant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CacheConstantsTest {
    @Test
    void businessCachesHaveFiniteTtl() {
        assertThat(CacheConstants.DICT_TTL).isPositive();
        assertThat(CacheConstants.DEPARTMENT_TTL).isPositive();
        assertThat(CacheConstants.NEWS_DETAIL_TTL).isPositive();
        assertThat(CacheConstants.NOTIFICATION_UNREAD_TTL).isPositive();
    }
}
