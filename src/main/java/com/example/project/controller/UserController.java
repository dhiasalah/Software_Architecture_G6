package com.example.project.controller;

import com.example.project.entity.User;
import com.example.project.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 🎮 CONTROLLER USER - Pour débutants
 *
 * Qu'est-ce qu'un Controller ?
 * - C'est le point d'entrée de l'API REST
 * - Reçoit les requêtes HTTP (GET, POST, PUT, DELETE)
 * - Appelle le Service pour faire le travail
 * - Retourne une réponse HTTP
 *
 * CRUD signifie :
 * - CREATE (POST) : Créer un utilisateur
 * - READ (GET) : Lire/Récupérer des utilisateurs
 * - UPDATE (PUT) : Modifier un utilisateur
 * - DELETE (DELETE) : Supprimer un utilisateur
 *
 * ✨ SIMPLIFIÉ : Utilise directement l'entité User (pas de DTO)
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "API pour gérer les utilisateurs (CRUD)")
public class UserController {

    private final UserService userService;

    /**
     * READ ALL - Récupérer tous les utilisateurs
     *
     * Endpoint : GET /api/users
     * Exemple : http://localhost:8080/api/users
     *
     * Sécurité : Uniquement les ADMIN peuvent accéder
     */
    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "Récupérer tous les utilisateurs",
               description = "Retourne la liste de tous les utilisateurs (les mots de passe sont masqués)")
    @ApiResponse(responseCode = "200", description = "Liste récupérée avec succès")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * READ ONE - Récupérer un utilisateur par son ID
     *
     * Endpoint : GET /api/users/{id}
     * Exemple : http://localhost:8080/api/users/1
     *
     * @param id ID de l'utilisateur à récupérer
     * Sécurité : Uniquement les ADMIN peuvent accéder
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "Récupérer un utilisateur par ID",
               description = "Retourne un utilisateur spécifique par son ID")
    @ApiResponse(responseCode = "200", description = "Utilisateur trouvé")
    @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * CREATE - Créer un nouvel utilisateur
     *
     * Endpoint : POST /api/users
     * Exemple : http://localhost:8080/api/users
     * Body JSON :
     * {
     *   "username": "newuser",
     *   "role": { "name": "USER" },
     *   "credentials": {
     *     "email": "user@example.com",
     *     "phoneNumber": "+33612345678",
     *     "password": "password123"
     *   }
     * }
     *
     * @param user Données du nouvel utilisateur
     * Sécurité : Uniquement les ADMIN peuvent créer des utilisateurs
     */
    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    @Operation(summary = "Créer un nouvel utilisateur",
               description = "Crée un nouvel utilisateur avec les informations fournies")
    @ApiResponse(responseCode = "201", description = "Utilisateur créé avec succès")
    @ApiResponse(responseCode = "400", description = "Données invalides")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    /**
     * UPDATE - Modifier un utilisateur existant
     *
     * Endpoint : PUT /api/users/{id}
     * Exemple : http://localhost:8080/api/users/1
     * Body JSON :
     * {
     *   "username": "updateduser",
     *   "role": { "name": "ADMIN" },
     *   "credentials": {
     *     "email": "newemail@example.com",
     *     "phoneNumber": "+33687654321",
     *     "password": "newpassword123"
     *   }
     * }
     *
     * @param id ID de l'utilisateur à modifier
     * @param user Nouvelles données
     * Sécurité : Uniquement les ADMIN peuvent modifier des utilisateurs
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Operation(summary = "Modifier un utilisateur",
               description = "Met à jour les informations d'un utilisateur existant")
    @ApiResponse(responseCode = "200", description = "Utilisateur modifié avec succès")
    @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @RequestBody User user) {
        User updatedUser = userService.updateUser(id, user);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * DELETE - Supprimer un utilisateur
     *
     * Endpoint : DELETE /api/users/{id}
     * Exemple : http://localhost:8080/api/users/1
     *
     * @param id ID de l'utilisateur à supprimer
     * Sécurité : Uniquement les ADMIN peuvent supprimer des utilisateurs
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    @Operation(summary = "Supprimer un utilisateur",
               description = "Supprime un utilisateur de la base de données")
    @ApiResponse(responseCode = "204", description = "Utilisateur supprimé avec succès")
    @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
