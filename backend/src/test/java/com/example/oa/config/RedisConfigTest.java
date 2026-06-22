package com.example.oa.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RedisConfigTest {

    @Test
    void immutableStreamListCanRoundTrip() {
        GenericJackson2JsonRedisSerializer serializer = new RedisConfig().redisSerializer();
        List<String> source = Stream.of("OA", "AI").toList();

        Object restored = serializer.deserialize(serializer.serialize(source));

        assertThat(restored).isInstanceOf(List.class);
        assertThat(restored).isEqualTo(source);
    }
}
