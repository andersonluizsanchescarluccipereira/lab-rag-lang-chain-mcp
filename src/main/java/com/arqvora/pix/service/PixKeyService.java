package com.arqvora.pix.service;

import com.arqvora.pix.dto.PixDtos;
import com.arqvora.pix.error.ApiException;
import com.arqvora.pix.model.Account;
import com.arqvora.pix.model.AppUser;
import com.arqvora.pix.model.PixKey;
import com.arqvora.pix.repository.AccountRepository;
import com.arqvora.pix.repository.PixKeyRepository;
import com.arqvora.pix.repository.UserRepository;
import com.arqvora.pix.security.PixKeyHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PixKeyService {
    private final PixKeyRepository keys;
    private final AccountRepository accounts;
    private final UserRepository users;
    private final PixKeyNormalizer normalizer;
    private final PixKeyHasher hasher;
    private final LookupRateLimiter limiter;
    private final String institution;

    public PixKeyService(PixKeyRepository keys, AccountRepository accounts, UserRepository users,
                         PixKeyNormalizer normalizer, PixKeyHasher hasher, LookupRateLimiter limiter,
                         @Value("${app.pix.institution:ARQVORA Bank Lab}") String institution) {
        this.keys = keys;
        this.accounts = accounts;
        this.users = users;
        this.normalizer = normalizer;
        this.hasher = hasher;
        this.limiter = limiter;
        this.institution = institution;
    }

    @Transactional(readOnly = true)
    public Page<PixDtos.KeyResponse> mine(UUID userId, int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "page >= 0 e size entre 1 e 50");
        }
        return keys.findByAccount_OwnerId(userId, PageRequest.of(page, size,
                        Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PixDtos.KeyResponse myKey(UUID userId, UUID keyId) {
        return toResponse(keys.findByIdAndAccount_OwnerId(keyId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chave nao encontrada")));
    }

    @Transactional
    public PixDtos.KeyResponse register(UUID userId, PixDtos.KeyRequest request) {
        Account account = accounts.findByOwnerId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conta nao encontrada"));
        if (keys.countByAccount_Id(account.getId()) >= 5) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Limite de cinco chaves por conta neste laboratorio");
        }
        String normalized = normalizer.normalize(request.type(), request.key());
        String hash = hasher.hash(request.type(), normalized);
        if (keys.findByKeyHash(hash).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Chave ja cadastrada");
        }
        PixKey saved = keys.saveAndFlush(new PixKey(account, request.type(), hash,
                normalizer.mask(request.type(), normalized)));
        return toResponse(saved);
    }

    @Transactional
    public void remove(UUID userId, UUID keyId) {
        PixKey key = keys.findByIdAndAccount_OwnerId(keyId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chave nao encontrada"));
        keys.delete(key);
    }

    @Transactional(readOnly = true)
    public PixDtos.LookupResponse lookup(UUID userId, PixDtos.KeyRequest request) {
        limiter.check(userId);
        String normalized = normalizer.normalize(request.type(), request.key());
        String hash = hasher.hash(request.type(), normalized);
        PixKey key = keys.findByKeyHash(hash)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chave nao encontrada no diretorio simulado"));
        Account account = key.getAccount();
        AppUser owner = users.findById(account.getOwnerId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chave nao encontrada no diretorio simulado"));
        return new PixDtos.LookupResponse(key.getType(), key.getMaskedKey(),
                maskName(owner.getName()), institution, "****" + account.getId().toString().substring(32));
    }

    private PixDtos.KeyResponse toResponse(PixKey key) {
        return new PixDtos.KeyResponse(key.getId(), key.getType(), key.getMaskedKey(), key.getCreatedAt());
    }

    static String maskName(String name) {
        if (name == null || name.isBlank()) return "***";
        String[] words = name.trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (!out.isEmpty()) out.append(' ');
            out.append(word.charAt(0)).append("***");
        }
        return out.toString();
    }
}
