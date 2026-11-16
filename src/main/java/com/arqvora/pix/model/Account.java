package com.arqvora.pix.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {
    @Id
    private UUID id;
    @Column(name = "owner_id", nullable = false, unique = true)
    private UUID ownerId;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    protected Account() { }

    public Account(UUID ownerId, BigDecimal balance) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.balance = balance;
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public BigDecimal getBalance() { return balance; }
}
