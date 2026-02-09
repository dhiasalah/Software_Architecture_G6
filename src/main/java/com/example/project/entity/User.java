package com.example.project.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 👤 CLASSE USER - Pour débutants
 *
 * Cette classe représente un UTILISATEUR de l'application
 *
 * Changements appliqués :
 * - ❌ Ancienne version : role était un simple String
 * - ✅ Nouvelle version : role est une relation vers la classe Role
 * - ✨ NOUVEAU : Les credentials (email, phone, password) sont dans une classe séparée
 *
 * Avantages :
 * - Meilleure organisation du code
 * - Séparation des données de base (username, role) et des identifiants sensibles (credentials)
 * - Plus facile d'ajouter des informations sur les rôles
 * - Possibilité d'évoluer vers plusieurs rôles par utilisateur
 */
@Entity
@Table(name = "users")
@Data
public class User {

    /**
     * ID unique de l'utilisateur
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nom d'utilisateur (unique pour chaque utilisateur)
     */
    @Column(unique = true, nullable = false)
    private String username;

    /**
     * 🔐 RELATION AVEC ROLE
     *
     * @ManyToOne : Plusieurs utilisateurs peuvent avoir le MÊME rôle
     *              (ex: 100 utilisateurs avec le rôle "USER")
     *
     * @JoinColumn : Crée une colonne "role_id" dans la table "users"
     *               qui fait référence à l'ID dans la table "roles"
     *
     * fetch = FetchType.EAGER : Charge automatiquement le rôle avec l'utilisateur
     *                           (sinon il faudrait faire une requête séparée)
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    /**
     * 🔑 RELATION AVEC CREDENTIALS (One-to-One)
     *
     * @OneToOne : Une relation 1:1 avec Credentials
     * mappedBy = "user" : Indique que la relation est gérée par le champ "user" dans Credentials
     *                      (Credentials possède la clé étrangère)
     *
     * cascade = CascadeType.ALL : Toutes les opérations sur User sont propagées à Credentials
     *                              (save, update, delete)
     *
     * orphanRemoval = true : Si on retire les credentials d'un user, ils sont supprimés de la BD
     *
     * fetch = FetchType.EAGER : Charge automatiquement les credentials avec l'utilisateur
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Credentials credentials;
}
