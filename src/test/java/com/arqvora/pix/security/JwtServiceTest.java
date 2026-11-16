package com.arqvora.pix.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    @Test
    void generatesAndValidatesTokenWithTheSameFormatAsSaldoApi() {
        String secret = Base64.getEncoder().encodeToString(new byte[32]); // Apenas teste.
        JwtService jwt = new JwtService(secret, 60);
        UUID userId = UUID.randomUUID();
        String token = jwt.generate(userId);
        assertEquals(userId, jwt.getUserId(token));
        assertEquals(3600, jwt.expirationSeconds());
        assertThrows(JwtException.class, () -> jwt.getUserId(token + "invalid"));
    }

    @Test
    void rejectsSecretSmallerThan256Bits() {
        String secret = Base64.getEncoder().encodeToString(new byte[16]);
        assertThrows(IllegalArgumentException.class, () -> new JwtService(secret, 60));
    }
}
