package com.arqvora.pix.dto;

import com.arqvora.pix.model.PixKeyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public final class PixDtos {
    private PixDtos() { }

    public record KeyRequest(
            @NotNull @Schema(example = "EMAIL") PixKeyType type,
            @NotBlank @Schema(example = "loja@example.com") String key) { }

    public record KeyResponse(UUID id, PixKeyType type, String maskedKey, Instant createdAt) { }

    public record LookupResponse(PixKeyType type, String maskedKey,
                                 String recipientMaskedName, String institution,
                                 String accountMasked) { }
}
