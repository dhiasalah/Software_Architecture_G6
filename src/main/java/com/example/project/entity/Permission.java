package com.example.project.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 🔑 CLASSE PERMISSION
 *
 * Représente une PERMISSION individuelle (ex: USER_READ, USER_DELETE).
 *
 * Relation avec Role :
 * - Un rôle peut avoir PLUSIEURS permissions (ManyToMany)
 * - Une permission peut appartenir à PLUSIEURS rôles
 *
 * Exemple :
 * - ADMIN a les permissions : USER_READ, USER_CREATE, USER_UPDATE, USER_DELETE
 * - USER a la permission : USER_READ
 */
@Entity
@Table(name = "permissions")
@Data
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Type de permission (USER_READ, USER_CREATE, etc.)
     * @Enumerated(EnumType.STRING) : Sauvegarde le nom en base ("USER_READ" etc.)
     * unique = true : Pas de doublons
     */
    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private PermissionType name;

    /**
     * Description de la permission
     */
    @Column(length = 500)
    private String description;
}
