package com.arqvora.pix.controller;

import com.arqvora.pix.dto.PixDtos;
import com.arqvora.pix.service.PixKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pix/keys")
@Tag(name = "Pix - Consulta de Chaves")
@SecurityRequirement(name = "bearerAuth")
public class PixKeyController {
    private final PixKeyService service;
    public PixKeyController(PixKeyService service) { this.service = service; }

    @GetMapping("/me")
    @Operation(summary = "Consultar minhas chaves Pix", description = "Lista somente as chaves da conta autenticada, com mascaramento e paginacao.")
    public Page<PixDtos.KeyResponse> mine(Authentication authentication,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return service.mine(currentUser(authentication), page, size);
    }

    @GetMapping("/me/{keyId}")
    @Operation(summary = "Consultar uma chave da minha conta")
    public PixDtos.KeyResponse mineById(Authentication authentication, @PathVariable UUID keyId) {
        return service.myKey(currentUser(authentication), keyId);
    }

    @PostMapping("/me")
    @Operation(summary = "Cadastrar chave ficticia na conta", description = "Apenas diretorio simulado local. Nao registra a chave no DICT do Banco Central.")
    public ResponseEntity<PixDtos.KeyResponse> register(Authentication authentication,
                                                         @Valid @RequestBody PixDtos.KeyRequest request) {
        PixDtos.KeyResponse saved = service.register(currentUser(authentication), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}").buildAndExpand(saved.id()).toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @DeleteMapping("/me/{keyId}")
    @Operation(summary = "Remover uma chave ficticia da minha conta")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable UUID keyId) {
        service.remove(currentUser(authentication), keyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/lookup")
    @Operation(summary = "Consultar destinatario por chave Pix", description = "Busca ficticia apenas em pix_keys do laboratorio. Limite de 20 consultas/minuto/usuario/instancia. Nunca acessa o DICT real.")
    public PixDtos.LookupResponse lookup(Authentication authentication,
                                         @Valid @RequestBody PixDtos.KeyRequest request) {
        return service.lookup(currentUser(authentication), request);
    }

    private static UUID currentUser(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
