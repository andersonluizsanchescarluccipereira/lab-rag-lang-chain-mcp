package com.arqvora.pix.service;

import com.arqvora.pix.dto.PixDtos;
import com.arqvora.pix.error.ApiException;
import com.arqvora.pix.model.Account;
import com.arqvora.pix.model.AppUser;
import com.arqvora.pix.model.PixKey;
import com.arqvora.pix.model.PixKeyType;
import com.arqvora.pix.repository.AccountRepository;
import com.arqvora.pix.repository.PixKeyRepository;
import com.arqvora.pix.repository.UserRepository;
import com.arqvora.pix.security.PixKeyHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PixKeyServiceTest {
    @Mock PixKeyRepository keys;
    @Mock AccountRepository accounts;
    @Mock UserRepository users;
    @Mock PixKeyHasher hasher;
    @Mock LookupRateLimiter limiter;

    PixKeyService service;
    UUID userId;
    Account account;

    @BeforeEach
    void init() {
        service = new PixKeyService(keys, accounts, users, new PixKeyNormalizer(), hasher, limiter, "ARQVORA Bank Lab");
        userId = UUID.randomUUID();
        account = new Account(userId, BigDecimal.ZERO);
    }

    @Test
    void lookupUsesNormalizedHashedKeyAndMasksOwner() {
        when(hasher.hash(PixKeyType.EMAIL, "loja@example.com")).thenReturn("hash");
        when(keys.findByKeyHash("hash")).thenReturn(Optional.of(
                new PixKey(account, PixKeyType.EMAIL, "hash", "lo***@example.com")));
        when(users.findById(userId)).thenReturn(Optional.of(new AppUser("Loja Exemplo", "loja@arqvora.local", "fakehash")));

        var result = service.lookup(userId, new PixDtos.KeyRequest(PixKeyType.EMAIL, "LOJA@Example.com"));
        assertEquals("lo***@example.com", result.maskedKey());
        assertEquals("L*** E***", result.recipientMaskedName());
        assertEquals("ARQVORA Bank Lab", result.institution());
        verify(limiter).check(userId);
        verify(keys).findByKeyHash("hash");
    }

    @Test
    void cannotReadAnotherUsersKeyById() {
        UUID id = UUID.randomUUID();
        when(keys.findByIdAndAccount_OwnerId(id, userId)).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class, () -> service.myKey(userId, id));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(keys, never()).findById(any());
    }

    @Test
    void protectsPagination() {
        assertThrows(ApiException.class, () -> service.mine(userId, 0, 51));
        assertThrows(ApiException.class, () -> service.mine(userId, -1, 20));
        verifyNoInteractions(keys, accounts, users);
    }

    @Test
    void rejectsMissingAccountForRegistration() {
        when(accounts.findByOwnerId(userId)).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class,
                () -> service.register(userId, new PixDtos.KeyRequest(PixKeyType.EMAIL, "loja@example.com")));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
