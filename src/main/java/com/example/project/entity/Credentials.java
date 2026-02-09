package com.example.project.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

/**
 * 🔑 CLASSE CREDENTIALS - Pour débutants
 *
 * Cette classe représente les IDENTIFIANTS d'un utilisateur
 *
 * Contient :
 * - Email
 * - Numéro de téléphone
 * - Mot de passe
 *
 * Pourquoi une classe séparée ?
 * - Séparation des responsabilités : User contient les infos de base, Credentials les infos sensibles
 * - Facilite la gestion de la sécurité (encryption, validation)
 * - Permet d'isoler les données sensibles
 *
 * Relation avec User :
 * - @OneToOne : Un utilisateur a exactement UN ensemble d'identifiants
 * - @OneToOne : Un ensemble d'identifiants appartient à UN seul utilisateur
 */
@Entity
@Table(name = "credentials")
@Data
public class Credentials {

    /**
     * ID unique des identifiants
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Email de l'utilisateur
     * @Column :
     * - unique = true : Un email ne peut être utilisé qu'une seule fois
     * - nullable = false : L'email est obligatoire
     */
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * Numéro de téléphone
     * @Column :
     * - unique = true : Un numéro ne peut être utilisé qu'une seule fois
     * - nullable = true : Le numéro de téléphone est optionnel
     */
    @Column(unique = true, nullable = true)
    private String phoneNumber;

    /**
     * Mot de passe (encodé en BCrypt par Spring Security)
     * @JsonProperty(access = WRITE_ONLY) : Permet de RECEVOIR le password (write) mais pas de l'EXPOSER (read)
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    /**
     * 🔗 RELATION AVEC USER (One-to-One)
     *
     * @OneToOne : Une relation 1:1 avec User
     * @JoinColumn : Crée une colonne "user_id" dans la table "credentials"
     *               qui fait référence à l'ID dans la table "users"
     *
     * fetch = FetchType.LAZY : Charge l'utilisateur seulement quand on y accède
     *                          (optimisation des performances)
     *
     * optional = false : Les credentials DOIVENT être liés à un utilisateur
     *
     * @JsonIgnore : Évite les références circulaires lors de la sérialisation JSON
     */
    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
}
