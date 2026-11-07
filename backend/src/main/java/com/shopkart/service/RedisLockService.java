package com.shopkart.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * Minimal Redis distributed lock.
 *
 * <p>Acquire uses SET key value NX PX ttl (atomic set-if-absent with expiry).
 * Release uses a Lua script that deletes the key only if the stored token matches,
 * so a client can never release a lock it no longer owns (avoids the classic
 * "delete someone else's lock" race).
 */
@Service
public class RedisLockService {

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** Try to acquire the lock once. Returns a token if acquired, or null if already held. */
    public String tryAcquire(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        return Boolean.TRUE.equals(ok) ? token : null;
    }

    /** Release the lock only if we still own it (token match). */
    public void release(String key, String token) {
        if (token == null) {
            return;
        }
        redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), token);
    }
}
