package com.example.project.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * CLASSE ROLE - Pour débutants
 *
 * Cette classe représente un RÔLE d'utilisateur (ADMIN ou USER)
 * Chaque rôle possède un ensemble de PERMISSIONS qui déterminent ce que l'utilisateur peut faire.
 *
 * Relation avec Permission :
 * - Un rôle peut avoir PLUSIEURS permissions (ManyToMany)
 * - Une permission peut appartenir à PLUSIEURS rôles
 *
 * Exemple :
 * - ADMIN → USER_READ, USER_CREATE, USER_UPDATE, USER_DELETE
 * - USER  → USER_READ
 */
@Entity
@Table(name = "roles")
@Data
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Type du rôle (ADMIN ou USER)
     */
    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private RoleType name;

    @Column(length = 500)
    private String description;

    /**
     * PERMISSIONS associées à ce rôle
     *
     * @ManyToMany : Relation N:N entre Role et Permission
     * @JoinTable : Crée une table intermédiaire "role_permissions"
     *   - joinColumns : Colonne qui référence le rôle (role_id)
     *   - inverseJoinColumns : Colonne qui référence la permission (permission_id)
     *
     * fetch = FetchType.EAGER : Charge automatiquement les permissions avec le rôle
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();
}
