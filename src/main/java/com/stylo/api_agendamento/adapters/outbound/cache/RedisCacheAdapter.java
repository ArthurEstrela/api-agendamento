package com.stylo.api_agendamento.adapters.outbound.cache;

import com.stylo.api_agendamento.core.ports.ICacheService;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service // Esta anotação resolve o erro do Spring!
public class RedisCacheAdapter implements ICacheService {

    private final RedissonClient redissonClient;

    public RedisCacheAdapter(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void set(String key, Object value, long ttlMinutes) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        // Salva o valor no Redis com o tempo de expiração (TTL)
        bucket.set(value, Duration.ofMinutes(ttlMinutes));
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> clazz) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        T value = bucket.get();
        return Optional.ofNullable(value);
    }

    @Override
    public void evict(String key) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        bucket.delete(); // Remove a chave específica do Redis
    }

    @Override
    public void evictPattern(String pattern) {
        RKeys keys = redissonClient.getKeys();
        keys.deleteByPattern(pattern); // Remove todas as chaves que batem com o padrão (ex: appt:prof:123:*)
    }
}