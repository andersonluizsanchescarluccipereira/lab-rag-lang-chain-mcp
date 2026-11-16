package com.arqvora.pix.security;

import com.arqvora.pix.model.PixKeyType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class PixKeyHasher {
    private final SecretKeySpec key;

    public PixKeyHasher(@Value("${app.pix.lookup-hmac-secret-base64}") String secretBase64) {
        byte[] secret = Base64.getDecoder().decode(secretBase64);
        if (secret.length < 32) {
            throw new IllegalArgumentException("PIX_LOOKUP_SECRET_BASE64 deve conter no minimo 32 bytes aleatorios");
        }
        this.key = new SecretKeySpec(secret, "HmacSHA256");
    }

    public String hash(PixKeyType type, String normalizedKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            byte[] digest = mac.doFinal((type.name() + ":" + normalizedKey)
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao calcular hash da chave", e);
        }
    }
}
