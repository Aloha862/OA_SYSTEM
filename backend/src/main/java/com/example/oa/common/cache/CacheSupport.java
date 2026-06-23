package com.example.oa.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheSupport {
    private static final String NULL_MARKER = "__OA_CACHE_NULL__";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end", Long.class);
    private final RedisTemplate<String, Object> redisTemplate;

    public Duration jitter(Duration base) {
        long seconds = Math.max(1, base.toSeconds());
        long spread = Math.max(1, seconds / 10);
        return Duration.ofSeconds(seconds + ThreadLocalRandom.current().nextLong(-spread, spread + 1));
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrLoad(String key, Duration ttl, Duration emptyTtl, Supplier<T> loader, Predicate<T> empty) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (NULL_MARKER.equals(cached)) return null;
            if (cached != null) return (T) cached;
        } catch (SerializationException e) {
            log.info("检测到旧版或无效缓存，将自动删除并重建: key={}", key);
            try {
                redisTemplate.delete(key);
            } catch (Exception deleteException) {
                log.warn("删除无效缓存失败，将继续尝试覆盖: key={}", key, deleteException);
            }
        } catch (Exception e) {
            log.warn("缓存读取失败，将回源: key={}", key, e);
            return loader.get();
        }
        String lockKey = "lock:" + key;
        String token = UUID.randomUUID().toString();
        boolean locked;
        try {
            locked = Boolean.TRUE.equals(redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, token, Duration.ofSeconds(10)));
        } catch (Exception e) {
            log.warn("缓存锁获取失败，将直接回源: key={}", key, e);
            return loader.get();
        }
        if (!locked) {
            try {
                Thread.sleep(60);
                Object cached = redisTemplate.opsForValue().get(key);
                if (NULL_MARKER.equals(cached)) return null;
                if (cached != null) return (T) cached;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.warn("等待热点缓存加载失败: key={}", key, e);
            }
            return loader.get();
        }
        try {
            T value = loader.get();
            boolean emptyValue = value == null || empty.test(value);
            try {
                redisTemplate.opsForValue().set(key, value == null ? NULL_MARKER : value,
                        jitter(emptyValue ? emptyTtl : ttl));
            } catch (Exception e) {
                log.warn("缓存写入失败: key={}", key, e);
            }
            return value;
        } finally {
            try {
                redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockKey), token);
            } catch (Exception e) {
                log.debug("缓存锁释放失败: key={}", lockKey, e);
            }
        }
    }

    public void deleteAfterCommit(String... keys) {
        Runnable deletion = () -> {
            try {
                redisTemplate.delete(Arrays.asList(keys));
            } catch (Exception e) {
                log.warn("事务后缓存失效失败: keyCount={}", keys.length, e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deletion.run();
                }
            });
        } else {
            deletion.run();
        }
    }
}
