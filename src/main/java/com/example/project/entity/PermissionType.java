package com.example.project.entity;

/**
 * 🔐 ENUM PERMISSION TYPE - Permissions fines
 *
 * Définit toutes les permissions disponibles dans l'application.
 * Chaque permission correspond à une ACTION spécifique (lire, créer, modifier, supprimer).
 *
 * Avantage par rapport aux rôles simples :
 * - Un rôle peut avoir N'IMPORTE QUELLE combinaison de permissions
 * - On peut facilement ajouter de nouvelles permissions sans toucher aux rôles
 * - Contrôle d'accès plus granulaire (ex: un modérateur peut lire et modifier, mais pas supprimer)
 */
public enum PermissionType {

    /** Permission de lire/consulter les utilisateurs */
    USER_READ("Permet de consulter la liste des utilisateurs"),

    /** Permission de créer de nouveaux utilisateurs */
    USER_CREATE("Permet de créer de nouveaux utilisateurs"),

    /** Permission de modifier les utilisateurs existants */
    USER_UPDATE("Permet de modifier les utilisateurs existants"),

    /** Permission de supprimer des utilisateurs */
    USER_DELETE("Permet de supprimer des utilisateurs");

    private final String description;

    PermissionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
