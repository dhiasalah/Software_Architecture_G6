package com.example.project.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

/**
 * ENTITÉ VERIFICATION TOKEN
 *
 * Cette table stocke les tokens de vérification d'e-mail.
 *
 * POURQUOI CETTE CLASSE ?
 * Quand un utilisateur s'inscrit, on génère un token secret (UUID).
 * On ne stocke PAS le token en clair en base (même principe que les mots de passe).
 * On stocke uniquement le HASH BCrypt du token.
 *
 * COMMENT ÇA MARCHE ?
 * 1. Inscription → on génère un UUID (ex: "abc-123-def")
 * 2. On stocke BCrypt("abc-123-def") dans tokenHash
 * 3. On envoie le token en clair dans l'e-mail : /verify?tokenId=tok_1&t=abc-123-def
 * 4. L'utilisateur clique le lien → on compare BCrypt(t) avec tokenHash
 * 5. Si ça matche ET pas expiré → compte vérifié !
 * 6. On supprime le token (usage unique = one-shot)
 *
 * CHAMPS :
 * - tokenId   : identifiant public du token (pour le retrouver en base)
 * - tokenHash : le hash BCrypt du vrai token (secret jamais stocké en clair)
 * - expiresAt : date d'expiration (30 min par défaut)
 * - user      : l'utilisateur à qui appartient ce token
 */
@Entity
@Table(name = "verification_tokens")
@Data
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identifiant public du token (UUID)
     * Sert à retrouver le token en base quand l'utilisateur clique le lien
     * Ce n'est PAS le secret — c'est juste un identifiant de recherche
     */
    @Column(nullable = false, unique = true)
    private String tokenId;

    /**
     * Hash BCrypt du vrai token
     * Le vrai token (en clair) n'est JAMAIS stocké en base
     * On compare avec BCrypt.matches(tokenClair, tokenHash)
     */
    @Column(nullable = false)
    private String tokenHash;

    /**
     * Date d'expiration du token
     * Passé ce délai, le token est invalide et ne peut plus être utilisé
     */
    @Column(nullable = false)
    private Instant expiresAt;

    /**
     * L'utilisateur à qui appartient ce token de vérification
     *
     * @ManyToOne : Plusieurs tokens peuvent exister pour un même utilisateur
     *              (ex: si l'utilisateur demande un renvoi d'e-mail)
     * @JoinColumn : Crée une colonne "user_id" dans la table verification_tokens
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
