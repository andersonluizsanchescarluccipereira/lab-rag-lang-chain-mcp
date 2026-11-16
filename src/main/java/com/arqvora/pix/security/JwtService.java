package com.arqvora.pix.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(@Value("${app.jwt.secret-base64}") String secretBase64,
                      @Value("${app.jwt.expiration-minutes:60}") long expirationMinutes) {
        byte[] secret = Decoders.BASE64.decode(secretBase64);
        if (secret.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET_BASE64 precisa conter no mínimo 32 bytes aleatórios.");
        }
        if (expirationMinutes < 1 || expirationMinutes > 1440) {
            throw new IllegalArgumentException("JWT_EXPIRATION_MINUTES deve estar entre 1 e 1440.");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.expirationMinutes = expirationMinutes;
    }

    public String generate(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public UUID getUserId(String token) {
        String subject = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        return UUID.fromString(subject);
    }

    public long expirationSeconds() {
        return expirationMinutes * 60;
    }
}
