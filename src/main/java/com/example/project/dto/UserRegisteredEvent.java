package com.example.project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO ÉVÉNEMENT "UserRegistered"
 *
 * C'est le MESSAGE qui transite dans RabbitMQ entre Auth et Notification.
 *
 * POURQUOI UN DTO SÉPARÉ (et pas l'entité User) ?
 * - On ne veut PAS envoyer toute l'entité User dans la messagerie
 * - On minimise les données (principe de moindre privilège)
 * - On envoie uniquement ce dont Notification a besoin pour envoyer l'e-mail
 *
 * CHAMPS :
 * - eventId    : UUID unique de cet événement (pour traçabilité et dédoublonnage)
 * - userId     : ID de l'utilisateur qui vient de s'inscrire
 * - email      : adresse e-mail pour envoyer le mail de vérification
 * - tokenId    : identifiant public du token (pour construire le lien /verify?tokenId=...)
 * - tokenClear : le token en clair (pour construire le lien /verify?...&t=...)
 *                NOTE : en production on éviterait de le mettre dans l'événement,
 *                mais pour le TP c'est la façon la plus simple
 * - occurredAt : horodatage de l'événement (pour audit/traçabilité)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {

    private String eventId;
    private Long userId;
    private String email;
    private String tokenId;
    private String tokenClear;
    private Instant occurredAt;
}
