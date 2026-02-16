package com.example.project.repository;

import com.example.project.entity.Role;
import com.example.project.entity.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * REPOSITORY ROLE - Pour débutants
 *
 * Qu'est-ce qu'un Repository ?
 * - C'est une interface qui permet de communiquer avec la base de données
 * - Spring génère automatiquement le code nécessaire
 * - Vous n'avez qu'à déclarer les méthodes, Spring les implémente !
 *
 * JpaRepository<Role, Long> signifie :
 * - On travaille avec l'entité "Role"
 * - L'ID du rôle est de type "Long"
 *
 * Méthodes automatiques disponibles (sans les écrire) :
 * - save(role) : Sauvegarder un rôle
 * - findById(id) : Trouver un rôle par son ID
 * - findAll() : Récupérer tous les rôles
 * - delete(role) : Supprimer un rôle
 *
 * NOUVEAU : Utilise maintenant RoleType (enum) au lieu de String
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByName(RoleType name);
}
