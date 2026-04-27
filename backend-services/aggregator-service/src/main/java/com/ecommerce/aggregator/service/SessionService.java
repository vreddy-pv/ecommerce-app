package com.ecommerce.aggregator.service;

import com.ecommerce.aggregator.dto.SessionData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SessionService {

    private static final String SESSION_PREFIX = "session:";
    private static final Duration SESSION_TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;

    public void save(Long userId, SessionData session) {
        redisTemplate.opsForValue().set(key(userId), session, SESSION_TTL);
    }

    public Optional<SessionData> get(Long userId) {
        Object val = redisTemplate.opsForValue().get(key(userId));
        if (val instanceof SessionData sd) {
            // Slide TTL on access
            redisTemplate.expire(key(userId), SESSION_TTL);
            return Optional.of(sd);
        }
        return Optional.empty();
    }

    public void invalidate(Long userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(Long userId) {
        return SESSION_PREFIX + userId;
    }
}
