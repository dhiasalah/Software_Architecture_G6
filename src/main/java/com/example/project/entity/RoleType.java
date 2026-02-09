package com.example.project.entity;

/**
 * 🔐 ENUM ROLE TYPE - Pour débutants
 *
 * Un Enum (énumération) est un type spécial qui définit un ensemble fixe de constantes
 *
 * Pourquoi un Enum pour les rôles ?
 * - ✅ Garantit que seules les valeurs ADMIN et USER sont possibles
 * - ✅ Évite les erreurs de typo (ex: "ADMN" ou "user" au lieu de "ADMIN" ou "USER")
 * - ✅ Auto-complétion dans l'IDE
 * - ✅ Plus sûr et maintenable qu'un String
 *
 * Valeurs possibles :
 * - ADMIN : Administrateur avec toutes les permissions
 * - USER : Utilisateur standard avec permissions de base
 */
public enum RoleType {
    /**
     * Rôle Administrateur - Toutes les permissions
     */
    ADMIN("Administrateur avec toutes les permissions"),

    /**
     * Rôle Utilisateur - Permissions de base
     */
    USER("Utilisateur standard avec permissions de base");

    /**
     * Description du rôle
     */
    private final String description;

    /**
     * Constructeur de l'enum
     * @param description Description du rôle
     */
    RoleType(String description) {
        this.description = description;
    }

    /**
     * Récupère la description du rôle
     * @return Description du rôle
     */
    public String getDescription() {
        return description;
    }
}
