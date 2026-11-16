package com.arqvora.pix.security;

import com.arqvora.pix.model.PixKeyType;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class PixKeyHasherTest {
    private final String secret = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    @Test
    void sameKeyProducesSameHashWithoutExposingPlaintext() {
        var hasher = new PixKeyHasher(secret);
        String hash = hasher.hash(PixKeyType.EMAIL, "loja@example.com");
        assertEquals(64, hash.length());
        assertEquals(hash, hasher.hash(PixKeyType.EMAIL, "loja@example.com"));
        assertNotEquals(hash, hasher.hash(PixKeyType.EMAIL, "outra@example.com"));
        assertNotEquals(hash, hasher.hash(PixKeyType.CPF, "loja@example.com"));
        assertFalse(hash.contains("loja"));
    }

    @Test
    void rejectsWeakKey() {
        assertThrows(IllegalArgumentException.class,
                () -> new PixKeyHasher(Base64.getEncoder().encodeToString(new byte[10])));
    }
}
