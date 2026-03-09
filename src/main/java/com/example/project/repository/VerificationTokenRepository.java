package com.example.project.repository;

import com.example.project.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * REPOSITORY VERIFICATION TOKEN
 *
 * Interface pour accéder aux tokens de vérification en base de données.
 *
 * Spring génère automatiquement l'implémentation :
 * - findByTokenId("tok_abc") → SELECT * FROM verification_tokens WHERE token_id = 'tok_abc'
 * - deleteByTokenId("tok_abc") → DELETE FROM verification_tokens WHERE token_id = 'tok_abc'
 */
@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    /**
     * Trouver un token par son identifiant public (tokenId)
     * C'est le tokenId qu'on met dans le lien de vérification
     */
    VerificationToken findByTokenId(String tokenId);

    /**
     * Supprimer un token par son identifiant public
     * Utilisé après vérification réussie (one-shot)
     */
    void deleteByTokenId(String tokenId);
}
