package com.ecommerce.aggregator.service;

import com.ecommerce.aggregator.dto.SessionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SessionServiceTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOps;

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        sessionService = new SessionService(redisTemplate);
    }

    private SessionData sampleSession(Long userId) {
        return SessionData.builder()
            .userId(userId).username("alice").role("USER").lastLogin(Instant.now()).build();
    }

    @Test
    void saveStoresSessionWithTtl() {
        sessionService.save(1L, sampleSession(1L));

        verify(valueOps).set(eq("session:1"), any(SessionData.class), eq(Duration.ofHours(24)));
    }

    @Test
    void getReturnsStoredSession() {
        SessionData session = sampleSession(1L);
        when(valueOps.get("session:1")).thenReturn(session);

        var result = sessionService.get(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("alice");
        verify(redisTemplate).expire(eq("session:1"), eq(Duration.ofHours(24)));
    }

    @Test
    void getMissingSessionReturnsEmpty() {
        when(valueOps.get("session:99")).thenReturn(null);

        assertThat(sessionService.get(99L)).isEmpty();
    }

    @Test
    void invalidateDeletesKey() {
        sessionService.invalidate(1L);

        verify(redisTemplate).delete("session:1");
    }

    @Test
    void sessionTtlIsSliding() {
        SessionData session = sampleSession(1L);
        when(valueOps.get("session:1")).thenReturn(session);

        sessionService.get(1L);

        verify(redisTemplate).expire(eq("session:1"), eq(Duration.ofHours(24)));
    }
}
