package com.example.project.repository;

import com.example.project.entity.Permission;
import com.example.project.entity.PermissionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * REPOSITORY PERMISSION
 *
 * Permet de communiquer avec la table "permissions" en base de données.
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /**
     * Trouver une permission par son type
     *
     * Spring génère automatiquement la requête SQL :
     * SELECT * FROM permissions WHERE name = ?
     */
    Permission findByName(PermissionType name);
}
