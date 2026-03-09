package com.example.project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la réponse de vérification de token.
 *
 * Retournée par l'endpoint GET /api/auth/verify
 * Contient :
 * - valid   : true si le token est valide et correspond à celui stocké en base
 * - username: le nom d'utilisateur extrait du token (null si invalide)
 * - message : un message descriptif du résultat
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerifyTokenResponse {
    private boolean valid;
    private String username;
    private String message;
}
