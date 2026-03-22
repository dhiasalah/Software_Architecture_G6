package com.example.project.service;

import com.example.project.entity.Credentials;
import com.example.project.entity.Role;
import com.example.project.entity.User;
import com.example.project.repository.CredentialsRepository;
import com.example.project.repository.RoleRepository;
import com.example.project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 🔧 SERVICE USER - Pour débutants
 *
 * Qu'est-ce qu'un Service ?
 * - Contient la LOGIQUE MÉTIER de l'application
 * - Fait le lien entre le Controller et le Repository
 * - Gère les transformations et validations
 *
 * Pourquoi un Service ?
 * - Sépare la logique de l'API (Controller reste simple)
 * - Réutilisable dans plusieurs contrôleurs
 * - Facilite les tests unitaires
 *
 * ✨ SIMPLIFIÉ : Utilise directement l'entité User (pas de DTO)
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CredentialsRepository credentialsRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 📖 READ - Récupérer tous les utilisateurs
     *
     * @return Liste de tous les utilisateurs (les mots de passe sont masqués par @JsonIgnore)
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * 📖 READ - Récupérer un utilisateur par son ID
     *
     * @param id ID de l'utilisateur à récupérer
     * @return L'utilisateur trouvé
     * @throws RuntimeException Si l'utilisateur n'existe pas
     */
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));
    }

    /**
     * ➕ CREATE - Créer un nouvel utilisateur
     *
     * @param user Données du nouvel utilisateur
     * @return L'utilisateur créé
     * @throws RuntimeException Si le rôle n'existe pas ou le username/email est déjà pris
     */
    public User createUser(User user) {
        // Vérifier si le username existe déjà
        if (userRepository.findByUsername(user.getUsername()) != null) {
            throw new RuntimeException("Le nom d'utilisateur '" + user.getUsername() + "' est déjà utilisé");
        }

        // Vérifier que les credentials sont fournis
        if (user.getCredentials() == null) {
            throw new RuntimeException("Les credentials sont obligatoires (email et password)");
        }

        // Vérifier si l'email existe déjà
        if (credentialsRepository.findByEmail(user.getCredentials().getEmail()) != null) {
            throw new RuntimeException("L'email '" + user.getCredentials().getEmail() + "' est déjà utilisé");
        }

        // Vérifier si le numéro de téléphone existe déjà (s'il est fourni)
        String phoneNumber = user.getCredentials().getPhoneNumber();
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            if (credentialsRepository.findByPhoneNumber(phoneNumber) != null) {
                throw new RuntimeException("Le numéro de téléphone '" + phoneNumber + "' est déjà utilisé");
            }
        }

        // Récupérer le rôle
        Role role = roleRepository.findByName(user.getRole().getName());
        if (role == null) {
            throw new RuntimeException("Le rôle '" + user.getRole().getName() + "' n'existe pas");
        }
        user.setRole(role);

        // Encoder le mot de passe
        Credentials credentials = user.getCredentials();
        credentials.setPassword(passwordEncoder.encode(credentials.getPassword()));
        credentials.setUser(user);

        // Sauvegarder et retourner
        return userRepository.save(user);
    }

    /**
     * ✏️ UPDATE - Modifier un utilisateur existant
     *
     * @param id ID de l'utilisateur à modifier
     * @param updatedUser Nouvelles données
     * @return L'utilisateur modifié
     * @throws RuntimeException Si l'utilisateur ou le rôle n'existe pas
     */
    public User updateUser(Long id, User updatedUser) {
        // Trouver l'utilisateur existant
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));

        // Vérifier si le nouveau username est déjà pris (par un autre utilisateur)
        User userWithSameUsername = userRepository.findByUsername(updatedUser.getUsername());
        if (userWithSameUsername != null && !userWithSameUsername.getId().equals(id)) {
            throw new RuntimeException("Le nom d'utilisateur '" + updatedUser.getUsername() + "' est déjà utilisé");
        }

        // Vérifier que les credentials sont fournis
        if (updatedUser.getCredentials() != null) {
            // Vérifier si le nouvel email est déjà pris (par un autre utilisateur)
            Credentials existingCredsByEmail = credentialsRepository.findByEmail(updatedUser.getCredentials().getEmail());
            if (existingCredsByEmail != null && !existingCredsByEmail.getUser().getId().equals(id)) {
                throw new RuntimeException("L'email '" + updatedUser.getCredentials().getEmail() + "' est déjà utilisé");
            }

            // Vérifier si le nouveau numéro de téléphone est déjà pris (s'il est fourni)
            String phoneNumber = updatedUser.getCredentials().getPhoneNumber();
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                Credentials existingCredsByPhone = credentialsRepository.findByPhoneNumber(phoneNumber);
                if (existingCredsByPhone != null && !existingCredsByPhone.getUser().getId().equals(id)) {
                    throw new RuntimeException("Le numéro de téléphone '" + phoneNumber + "' est déjà utilisé");
                }
            }
        }

        // Mettre à jour le username si fourni
        if (updatedUser.getUsername() != null) {
            existingUser.setUsername(updatedUser.getUsername());
        }

        // Mettre à jour le rôle si fourni
        if (updatedUser.getRole() != null && updatedUser.getRole().getName() != null) {
            Role role = roleRepository.findByName(updatedUser.getRole().getName());
            if (role == null) {
                throw new RuntimeException("Le rôle '" + updatedUser.getRole().getName() + "' n'existe pas");
            }
            existingUser.setRole(role);
        }

        // Mettre à jour les credentials si fournis
        if (updatedUser.getCredentials() != null) {
            Credentials existingCredentials = existingUser.getCredentials();
            if (existingCredentials == null) {
                // Si les credentials n'existent pas, les créer
                existingCredentials = new Credentials();
                existingCredentials.setUser(existingUser);
                existingUser.setCredentials(existingCredentials);
            }

            existingCredentials.setEmail(updatedUser.getCredentials().getEmail());
            existingCredentials.setPhoneNumber(updatedUser.getCredentials().getPhoneNumber());

            // Mettre à jour le mot de passe seulement s'il est fourni et non vide
            String newPassword = updatedUser.getCredentials().getPassword();
            if (newPassword != null && !newPassword.isEmpty()) {
                existingCredentials.setPassword(passwordEncoder.encode(newPassword));
            }
        }

        // Sauvegarder et retourner
        return userRepository.save(existingUser);
    }

    /**
     * ❌ DELETE - Supprimer un utilisateur
     *
     * @param id ID de l'utilisateur à supprimer
     * @throws RuntimeException Si l'utilisateur n'existe pas
     */
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));
        userRepository.delete(user);
    }
}
