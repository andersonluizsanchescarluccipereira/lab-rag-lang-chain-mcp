package com.arqvora.pix.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pix_keys",
        uniqueConstraints = @UniqueConstraint(name = "uk_pix_keys_hash", columnNames = "key_hash"),
        indexes = @Index(name = "idx_pix_keys_account", columnList = "account_id"))
public class PixKey {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "key_type", nullable = false, length = 12)
    private PixKeyType type;

    // Nunca persistimos CPF, CNPJ, e-mail, telefone ou EVP em texto claro.
    @Column(name = "key_hash", nullable = false, length = 64)
    private String keyHash;

    @Column(name = "key_masked", nullable = false, length = 200)
    private String maskedKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PixKey() { }

    public PixKey(Account account, PixKeyType type, String keyHash, String maskedKey) {
        this.id = UUID.randomUUID();
        this.account = account;
        this.type = type;
        this.keyHash = keyHash;
        this.maskedKey = maskedKey;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Account getAccount() { return account; }
    public PixKeyType getType() { return type; }
    public String getKeyHash() { return keyHash; }
    public String getMaskedKey() { return maskedKey; }
    public Instant getCreatedAt() { return createdAt; }
}
