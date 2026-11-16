package com.arqvora.pix.service;

import com.arqvora.pix.error.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LookupRateLimiterTest {
    @Test
    void blocksAfterConfiguredLimitOnlyForThatUser() {
        var limiter = new LookupRateLimiter(2, Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC));
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        limiter.check(a);
        limiter.check(a);
        ApiException ex = assertThrows(ApiException.class, () -> limiter.check(a));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
        assertDoesNotThrow(() -> limiter.check(b));
    }
}
