package com.example.project.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 🔐 CLASSE ROLE - Pour débutants
 *
 * Cette classe représente un RÔLE d'utilisateur (ADMIN ou USER uniquement)
 *
 * Pourquoi une classe séparée ?
 * - Séparation des responsabilités : User gère les données, Role gère les permissions
 * - Facilite l'ajout de nouveaux rôles sans modifier User
 * - Permet à un utilisateur d'avoir plusieurs rôles (future évolution)
 *
 * ✨ NOUVEAU : Utilise maintenant l'Enum RoleType pour garantir des valeurs valides
 */
@Entity
@Table(name = "roles")
@Data
public class Role {

    /**
     * ID unique du rôle dans la base de données
     * @GeneratedValue : Spring génère automatiquement cet ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Type du rôle (ADMIN ou USER)
     * @Enumerated(EnumType.STRING) : Sauvegarde le nom de l'enum en base ("ADMIN" ou "USER")
     * @Column : Configuration de la colonne en base de données
     * - unique = true : Pas de doublons (un seul rôle "ADMIN" par exemple)
     * - nullable = false : Ce champ est obligatoire
     */
    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private RoleType name;

    /**
     * Description du rôle (optionnel)
     * Utile pour documenter ce que le rôle permet de faire
     */
    @Column(length = 500)
    private String description;
}
