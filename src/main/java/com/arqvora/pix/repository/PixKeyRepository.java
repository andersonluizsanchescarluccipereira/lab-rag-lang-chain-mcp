package com.arqvora.pix.repository;

import com.arqvora.pix.model.PixKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PixKeyRepository extends JpaRepository<PixKey, UUID> {
    Optional<PixKey> findByKeyHash(String hash);
    Optional<PixKey> findByIdAndAccount_OwnerId(UUID id, UUID ownerId);
    Page<PixKey> findByAccount_OwnerId(UUID ownerId, Pageable pageable);
    long countByAccount_Id(UUID accountId);
}
