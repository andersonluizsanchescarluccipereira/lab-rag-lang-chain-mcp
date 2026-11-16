package com.arqvora.pix.service;

import com.arqvora.pix.error.ApiException;
import com.arqvora.pix.model.PixKeyType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PixKeyNormalizerTest {
    private final PixKeyNormalizer norm = new PixKeyNormalizer();

    @Test
    void normalizesEmailAndEvpAndMasksSensitiveData() {
        assertEquals("loja@example.com", norm.normalize(PixKeyType.EMAIL, "  LOJA@Example.COM "));
        assertEquals("lo***@example.com", norm.mask(PixKeyType.EMAIL, "loja@example.com"));
        assertEquals("11111111-2222-4333-8444-555555555555", norm.normalize(PixKeyType.EVP,
                "11111111-2222-4333-8444-555555555555"));
        assertEquals("11111111-****-****-****-************", norm.mask(PixKeyType.EVP,
                "11111111-2222-4333-8444-555555555555"));
    }

    @Test
    void validatesPhoneCpfAndCnpj() {
        assertEquals("+5511999990000", norm.normalize(PixKeyType.PHONE, "+5511999990000"));
        assertThrows(ApiException.class, () -> norm.normalize(PixKeyType.PHONE, "11999990000"));
        assertEquals("52998224725", norm.normalize(PixKeyType.CPF, "529.982.247-25"));
        assertEquals("11222333000181", norm.normalize(PixKeyType.CNPJ, "11.222.333/0001-81"));
        assertThrows(ApiException.class, () -> norm.normalize(PixKeyType.CPF, "111.111.111-11"));
        assertThrows(ApiException.class, () -> norm.normalize(PixKeyType.CNPJ, "11.222.333/0001-82"));
    }

    @Test
    void invalidInputRejected() {
        assertThrows(ApiException.class, () -> norm.normalize(PixKeyType.EVP, "not-uuid"));
        assertThrows(ApiException.class, () -> norm.normalize(PixKeyType.EMAIL, "sem-arroba"));
        assertThrows(ApiException.class, () -> norm.normalize(PixKeyType.EMAIL, " "));
    }
}
