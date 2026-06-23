package com.example.oa.common.cache;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.SerializationException;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CacheSupportTest {

    @Test
    void returnsCachedValueWithoutCallingLoader() {
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("dict:active")).thenReturn("cached");
        AtomicInteger loads = new AtomicInteger();

        String result = new CacheSupport(redis).getOrLoad(
                "dict:active", Duration.ofMinutes(5), Duration.ofSeconds(30),
                () -> {
                    loads.incrementAndGet();
                    return "database";
                }, String::isEmpty);

        assertThat(result).isEqualTo("cached");
        assertThat(loads).hasValue(0);
    }

    @Test
    void fallsBackToLoaderWhenRedisLockAcquisitionFails() {
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("news:1")).thenReturn(null);
        when(values.setIfAbsent(eq("lock:news:1"), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(Duration.class)))
                .thenThrow(new IllegalStateException("redis unavailable"));

        String result = new CacheSupport(redis).getOrLoad(
                "news:1", Duration.ofMinutes(5), Duration.ofSeconds(30),
                () -> "database", String::isEmpty);

        assertThat(result).isEqualTo("database");
    }

    @Test
    void deletesAndRebuildsCacheWhenStoredValueCannotBeDeserialized() {
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("dict:type:approval_type"))
                .thenThrow(new SerializationException("legacy cache format"));
        when(values.setIfAbsent(eq("lock:dict:type:approval_type"), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(Duration.class)))
                .thenReturn(true);

        String result = new CacheSupport(redis).getOrLoad(
                "dict:type:approval_type", Duration.ofMinutes(5), Duration.ofSeconds(30),
                () -> "database", String::isEmpty);

        assertThat(result).isEqualTo("database");
        verify(redis).delete("dict:type:approval_type");
        verify(values).set(eq("dict:type:approval_type"), eq("database"),
                org.mockito.ArgumentMatchers.any(Duration.class));
    }
}
