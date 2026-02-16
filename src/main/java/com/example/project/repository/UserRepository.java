package com.example.project.repository;

import com.example.project.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * REPOSITORY USER - Pour débutants
 *
 * Qu'est-ce qu'un Repository ?
 * - C'est une interface qui permet de communiquer avec la base de données
 * - Spring génère automatiquement le code nécessaire
 * - Vous n'avez qu'à déclarer les méthodes, Spring les implémente !
 *
 * JpaRepository<User, Long> signifie :
 * - On travaille avec l'entité "User"
 * - L'ID de l'utilisateur est de type "Long"
 *
 * Méthodes automatiques disponibles (sans les écrire) :
 * - save(user) : Sauvegarder un utilisateur
 * - findById(id) : Trouver un utilisateur par son ID
 * - findAll() : Récupérer tous les utilisateurs
 * - delete(user) : Supprimer un utilisateur
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Trouver un utilisateur par son nom d'utilisateur
     *
     * Spring génère automatiquement la requête SQL :
     * SELECT * FROM users WHERE username = ?
     *
     * Exemples d'utilisation :
     * - userRepository.findByUsername("john")
     * - userRepository.findByUsername("admin")
     */
    User findByUsername(String username);

}
