package com.example.project.repository;

import com.example.project.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * REPOSITORY BLACKLISTED TOKEN
 *
 * Accède à la table des tokens JWT invalidés (après logout).
 * Partagée entre toutes les instances backend via PostgreSQL.
 */
@Repository
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {

    /** Vérifie si un token est dans la blacklist */
    boolean existsByToken(String token);

    /** Supprime les tokens expirés (nettoyage) */
    void deleteAllByExpiresAtBefore(java.time.LocalDateTime dateTime);
}
