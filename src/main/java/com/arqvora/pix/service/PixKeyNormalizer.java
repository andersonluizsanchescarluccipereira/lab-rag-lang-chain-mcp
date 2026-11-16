package com.arqvora.pix.service;

import com.arqvora.pix.error.ApiException;
import com.arqvora.pix.model.PixKeyType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class PixKeyNormalizer {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");
    private static final Pattern E164 = Pattern.compile("^\\+[1-9][0-9]{7,14}$");
    private static final Pattern EVP = Pattern.compile("^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$");

    public String normalize(PixKeyType type, String raw) {
        if (type == null || raw == null || raw.isBlank() || raw.length() > 255) {
            throw invalid();
        }
        String value = raw.trim();
        return switch (type) {
            case EMAIL -> {
                if (!EMAIL.matcher(value).matches()) throw invalid();
                yield value.toLowerCase(Locale.ROOT);
            }
            case PHONE -> {
                if (!E164.matcher(value).matches()) throw invalid();
                yield value;
            }
            case EVP -> {
                if (!EVP.matcher(value).matches()) throw invalid();
                yield UUID.fromString(value).toString();
            }
            case CPF -> {
                String digits = value.replaceAll("[.\\-\\s]", "");
                if (!digits.matches("\\d{11}") || !validCpf(digits)) throw invalid();
                yield digits;
            }
            case CNPJ -> {
                String digits = value.replaceAll("[./\\-\\s]", "");
                if (!digits.matches("\\d{14}") || !validCnpj(digits)) throw invalid();
                yield digits;
            }
        };
    }

    public String mask(PixKeyType type, String normalized) {
        return switch (type) {
            case EMAIL -> {
                int at = normalized.indexOf('@');
                yield normalized.substring(0, Math.min(2, at)) + "***" + normalized.substring(at);
            }
            case PHONE -> normalized.substring(0, Math.min(3, normalized.length()))
                    + "*******" + normalized.substring(normalized.length() - 4);
            case EVP -> normalized.substring(0, 8) + "-****-****-****-************";
            case CPF -> "***.***.***-" + normalized.substring(9);
            case CNPJ -> "**.***.***/****-" + normalized.substring(12);
        };
    }

    private static boolean validCpf(String x) {
        if (x.chars().distinct().count() == 1) return false;
        int sum = 0;
        for (int i = 0; i < 9; i++) sum += (x.charAt(i) - '0') * (10 - i);
        int d1 = (sum * 10) % 11;
        if (d1 == 10) d1 = 0;
        sum = 0;
        for (int i = 0; i < 10; i++) sum += (x.charAt(i) - '0') * (11 - i);
        int d2 = (sum * 10) % 11;
        if (d2 == 10) d2 = 0;
        return d1 == x.charAt(9) - '0' && d2 == x.charAt(10) - '0';
    }

    private static boolean validCnpj(String x) {
        if (x.chars().distinct().count() == 1) return false;
        int[] w1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] w2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum1 = 0;
        for (int i = 0; i < 12; i++) sum1 += (x.charAt(i) - '0') * w1[i];
        int d1 = sum1 % 11 < 2 ? 0 : 11 - sum1 % 11;
        int sum2 = 0;
        for (int i = 0; i < 13; i++) sum2 += (x.charAt(i) - '0') * w2[i];
        int d2 = sum2 % 11 < 2 ? 0 : 11 - sum2 % 11;
        return d1 == x.charAt(12) - '0' && d2 == x.charAt(13) - '0';
    }

    private static ApiException invalid() {
        return new ApiException(HttpStatus.BAD_REQUEST, "Formato de chave Pix invalido para o tipo informado");
    }
}
