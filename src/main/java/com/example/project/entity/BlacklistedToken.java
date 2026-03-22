package com.example.project.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ENTITÉ BLACKLISTED TOKEN
 *
 * Stocke les tokens JWT invalidés (après logout) dans la base de données.
 * Cela permet de partager la blacklist entre toutes les instances backend
 * (contrairement à un Set en mémoire qui est propre à chaque instance).
 *
 * Le JwtFilter vérifie cette table avant d'accepter un token.
 * Les tokens expirés peuvent être nettoyés périodiquement.
 */
@Entity
@Table(name = "blacklisted_tokens")
@Data
@NoArgsConstructor
public class BlacklistedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Le token JWT invalidé */
    @Column(nullable = false, unique = true, length = 512)
    private String token;

    /** Date à laquelle le token a été blacklisté */
    @Column(nullable = false)
    private LocalDateTime blacklistedAt;

    /** Date d'expiration du JWT (pour pouvoir nettoyer les anciens tokens) */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public BlacklistedToken(String token, LocalDateTime expiresAt) {
        this.token = token;
        this.blacklistedAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
    }
}
