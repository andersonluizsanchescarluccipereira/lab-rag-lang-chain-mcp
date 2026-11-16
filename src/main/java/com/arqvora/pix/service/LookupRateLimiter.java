package com.arqvora.pix.service;

import com.arqvora.pix.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Limite simplificado em memoria por instancia, apenas para laboratorio. */
@Component
public class LookupRateLimiter {
    private record Window(Instant startsAt, int count) { }
    private final ConcurrentHashMap<UUID, Window> windows = new ConcurrentHashMap<>();
    private final int maxPerMinute;
    private final Clock clock;

    @Autowired
    public LookupRateLimiter(@Value("${app.pix.lookup-max-per-minute:20}") int maxPerMinute) {
        this(maxPerMinute, Clock.systemUTC());
    }

    // Construtor explicito para testes deterministas.
    LookupRateLimiter(int maxPerMinute, Clock clock) {
        if (maxPerMinute < 1) throw new IllegalArgumentException("Limite invalido");
        this.maxPerMinute = maxPerMinute;
        this.clock = clock;
    }

    public void check(UUID userId) {
        Instant now = clock.instant();
        windows.compute(userId, (id, current) -> {
            if (current == null || Duration.between(current.startsAt(), now).toSeconds() >= 60) {
                return new Window(now, 1);
            }
            if (current.count() >= maxPerMinute) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Limite de consultas excedido; tente novamente em um minuto");
            }
            return new Window(current.startsAt(), current.count() + 1);
        });
    }

    @Scheduled(fixedDelay = 600_000)
    public void clearExpired() {
        Instant now = clock.instant();
        windows.entrySet().removeIf(entry -> Duration.between(entry.getValue().startsAt(), now).toSeconds() >= 60);
    }
}
