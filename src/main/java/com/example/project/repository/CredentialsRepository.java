package com.example.project.repository;

import com.example.project.entity.Credentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 📦 REPOSITORY CREDENTIALS - Pour débutants
 *
 * Qu'est-ce qu'un Repository ?
 * - C'est une interface qui permet de communiquer avec la base de données
 * - Spring génère automatiquement le code nécessaire
 * - Vous n'avez qu'à déclarer les méthodes, Spring les implémente !
 *
 * JpaRepository<Credentials, Long> signifie :
 * - On travaille avec l'entité "Credentials"
 * - L'ID des credentials est de type "Long"
 *
 * Méthodes automatiques disponibles (sans les écrire) :
 * - save(credentials) : Sauvegarder des identifiants
 * - findById(id) : Trouver des identifiants par leur ID
 * - findAll() : Récupérer tous les identifiants
 * - delete(credentials) : Supprimer des identifiants
 */
@Repository
public interface CredentialsRepository extends JpaRepository<Credentials, Long> {

    /**
     * Trouver des credentials par email
     *
     * Spring génère automatiquement la requête SQL :
     * SELECT * FROM credentials WHERE email = ?
     *
     * @param email L'email à rechercher
     * @return Les credentials correspondants ou null
     */
    Credentials findByEmail(String email);

    /**
     * Trouver des credentials par numéro de téléphone
     *
     * Spring génère automatiquement la requête SQL :
     * SELECT * FROM credentials WHERE phone_number = ?
     *
     * @param phoneNumber Le numéro de téléphone à rechercher
     * @return Les credentials correspondants ou null
     */
    Credentials findByPhoneNumber(String phoneNumber);
}
