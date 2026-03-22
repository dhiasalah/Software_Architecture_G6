package com.example.project.service;

import com.example.project.entity.BlacklistedToken;
import com.example.project.repository.BlacklistedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * SERVICE TOKEN BLACKLIST - Gestion de la deconnexion (Logout)
 *
 * PROBLEME :
 *   Avec JWT stateless, le serveur ne stocke PAS de session.
 *   Le token est valide jusqu'a son expiration (15 min).
 *   Donc quand un utilisateur se deconnecte, son token est ENCORE valide.
 *
 * SOLUTION :
 *   On stocke les tokens invalides dans une TABLE en base de donnees (blacklisted_tokens).
 *   Cela permet de partager la blacklist entre TOUTES les instances backend.
 *   Quand un utilisateur fait POST /api/auth/logout :
 *     1. Son JWT est ajoute a la table blacklisted_tokens
 *     2. Le JwtFilter verifie si le token est dans la table AVANT de l'accepter
 *     3. Si le token est blackliste → 401 Unauthorized
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final BlacklistedTokenRepository blacklistedTokenRepository;

    /**
     * Ajouter un token a la blacklist (= le rendre invalide)
     * Appele lors du logout.
     *
     * @param token     Le JWT a invalider
     * @param expiresAt La date d'expiration du JWT
     */
    public void blacklist(String token, Date expiresAt) {
        if (!blacklistedTokenRepository.existsByToken(token)) {
            LocalDateTime expiry = expiresAt.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            blacklistedTokenRepository.save(new BlacklistedToken(token, expiry));
        }
    }

    /**
     * Verifier si un token est dans la blacklist.
     * Appele par le JwtFilter a chaque requete.
     *
     * @param token Le JWT a verifier
     * @return true si le token est blackliste (invalide), false sinon
     */
    public boolean isBlacklisted(String token) {
        return blacklistedTokenRepository.existsByToken(token);
    }
}
