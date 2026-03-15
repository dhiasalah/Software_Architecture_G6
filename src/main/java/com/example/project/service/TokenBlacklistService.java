package com.example.project.service;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SERVICE TOKEN BLACKLIST - Gestion de la deconnexion (Logout)
 *
 * PROBLEME :
 *   Avec JWT stateless, le serveur ne stocke PAS de session.
 *   Le token est valide jusqu'a son expiration (15 min).
 *   Donc quand un utilisateur se deconnecte, son token est ENCORE valide.
 *   Un attaquant qui a vole le token peut encore l'utiliser.
 *
 * SOLUTION :
 *   On maintient une BLACKLIST en memoire (Set) des tokens invalides.
 *   Quand un utilisateur fait POST /api/auth/logout :
 *     1. Son JWT est ajoute a la blacklist
 *     2. Le JwtFilter verifie si le token est dans la blacklist AVANT de l'accepter
 *     3. Si le token est dans la blacklist → 401 Unauthorized
 *
 * POURQUOI ConcurrentHashMap ?
 *   Parce que plusieurs requetes (threads) peuvent acceder a la blacklist en meme temps.
 *   ConcurrentHashMap est thread-safe (pas de probleme de concurrence).
 *
 * LIMITATION :
 *   La blacklist est en MEMOIRE → elle est perdue au redemarrage du serveur.
 *   En production, on utiliserait Redis ou une table en base de donnees.
 *   Mais pour le TP, c'est suffisant car les tokens expirent en 15 min de toute facon.
 */
@Service
public class TokenBlacklistService {

    /**
     * Set thread-safe qui stocke les tokens invalides (logout).
     * On utilise ConcurrentHashMap.newKeySet() pour avoir un Set concurrent.
     */
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    /**
     * Ajouter un token a la blacklist (= le rendre invalide)
     * Appele lors du logout.
     *
     * @param token Le JWT a invalider
     */
    public void blacklist(String token) {
        blacklistedTokens.add(token);
    }

    /**
     * Verifier si un token est dans la blacklist
     * Appele par le JwtFilter a chaque requete.
     *
     * @param token Le JWT a verifier
     * @return true si le token est blackliste (invalide), false sinon
     */
    public boolean isBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }
}
