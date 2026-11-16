package com.arqvora.pix.demo;

import com.arqvora.pix.model.Account;
import com.arqvora.pix.model.AppUser;
import com.arqvora.pix.model.PixKey;
import com.arqvora.pix.model.PixKeyType;
import com.arqvora.pix.repository.AccountRepository;
import com.arqvora.pix.repository.PixKeyRepository;
import com.arqvora.pix.repository.UserRepository;
import com.arqvora.pix.security.PixKeyHasher;
import com.arqvora.pix.service.PixKeyNormalizer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@Profile("demo")
public class DemoSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final AccountRepository accounts;
    private final PixKeyRepository keys;
    private final PixKeyNormalizer normalizer;
    private final PixKeyHasher hasher;
    private final PasswordEncoder encoder;

    public DemoSeeder(UserRepository users, AccountRepository accounts, PixKeyRepository keys,
                      PixKeyNormalizer normalizer, PixKeyHasher hasher, PasswordEncoder encoder) {
        this.users = users;
        this.accounts = accounts;
        this.keys = keys;
        this.normalizer = normalizer;
        this.hasher = hasher;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Seed idempotente, exclusivo de banco isolado. Nao ativa no perfil integrado.
        var demo = users.findByEmail("demo@arqvora.local")
                .orElseGet(() -> users.save(new AppUser("Usuario Demonstracao", "demo@arqvora.local",
                        encoder.encode("Demo@123456"))));
        var loja = users.findByEmail("loja@arqvora.local")
                .orElseGet(() -> users.save(new AppUser("Loja Exemplo", "loja@arqvora.local",
                        encoder.encode("Loja@123456"))));
        Account demoAccount = accounts.findByOwnerId(demo.getId())
                .orElseGet(() -> accounts.save(new Account(demo.getId(), BigDecimal.ZERO.setScale(2))));
        Account lojaAccount = accounts.findByOwnerId(loja.getId())
                .orElseGet(() -> accounts.save(new Account(loja.getId(), BigDecimal.ZERO.setScale(2))));

        addIfAbsent(demoAccount, PixKeyType.EMAIL, "demo@arqvora.local");
        addIfAbsent(demoAccount, PixKeyType.EVP, "11111111-2222-4333-8444-555555555555");
        addIfAbsent(lojaAccount, PixKeyType.EMAIL, "loja@example.com");
    }

    private void addIfAbsent(Account account, PixKeyType type, String raw) {
        String normalized = normalizer.normalize(type, raw);
        String hash = hasher.hash(type, normalized);
        if (keys.findByKeyHash(hash).isEmpty()) {
            keys.save(new PixKey(account, type, hash, normalizer.mask(type, normalized)));
        }
    }
}
