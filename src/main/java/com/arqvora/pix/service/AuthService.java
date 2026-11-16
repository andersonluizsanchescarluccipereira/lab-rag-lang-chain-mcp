package com.arqvora.pix.service;

import com.arqvora.pix.dto.AuthDtos;
import com.arqvora.pix.error.ApiException;
import com.arqvora.pix.model.AppUser;
import com.arqvora.pix.repository.UserRepository;
import com.arqvora.pix.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional(readOnly = true)
    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        AppUser user = users.findByEmail(request.email().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas"));
        if (!encoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas");
        }
        return new AuthDtos.LoginResponse(jwt.generate(user.getId()), "Bearer", jwt.expirationSeconds());
    }
}
