package com.example.project.configuration;

import com.example.project.entity.*;
import com.example.project.repository.CredentialsRepository;
import com.example.project.repository.PermissionRepository;
import com.example.project.repository.RoleRepository;
import com.example.project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * 🚀 INITIALISATEUR DE DONNÉES
 *
 * S'exécute automatiquement au DÉMARRAGE de l'application.
 *
 * Crée :
 * 1. Les permissions par défaut (USER_READ, USER_CREATE, USER_UPDATE, USER_DELETE)
 * 2. Les rôles par défaut (ADMIN, USER)
 * 3. Associe les permissions aux rôles :
 *    - ADMIN → toutes les permissions
 *    - USER  → USER_READ uniquement
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final CredentialsRepository credentialsRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {
        // --- 1. Créer les permissions ---
        for (PermissionType permType : PermissionType.values()) {
            if (permissionRepository.findByName(permType) == null) {
                Permission permission = new Permission();
                permission.setName(permType);
                permission.setDescription(permType.getDescription());
                permissionRepository.save(permission);
                System.out.println("✅ Permission " + permType + " créée avec succès");
            }
        }

        // --- 2. Créer les rôles et associer les permissions ---
        // Rôle USER : permission USER_READ uniquement
        if (roleRepository.findByName(RoleType.USER) == null) {
            Role userRole = new Role();
            userRole.setName(RoleType.USER);
            userRole.setDescription(RoleType.USER.getDescription());

            Set<Permission> userPermissions = new HashSet<>();
            userPermissions.add(permissionRepository.findByName(PermissionType.USER_READ));
            userRole.setPermissions(userPermissions);

            roleRepository.save(userRole);
            System.out.println("✅ Rôle USER créé avec permissions: USER_READ");
        }

        // Rôle ADMIN : toutes les permissions
        if (roleRepository.findByName(RoleType.ADMIN) == null) {
            Role adminRole = new Role();
            adminRole.setName(RoleType.ADMIN);
            adminRole.setDescription(RoleType.ADMIN.getDescription());

            Set<Permission> adminPermissions = new HashSet<>();
            for (PermissionType permType : PermissionType.values()) {
                adminPermissions.add(permissionRepository.findByName(permType));
            }
            adminRole.setPermissions(adminPermissions);

            roleRepository.save(adminRole);
            System.out.println("✅ Rôle ADMIN créé avec toutes les permissions");
        }

        // --- 3. Créer le compte admin par défaut ---
        if (userRepository.findByUsername(adminUsername) == null) {
            Role adminRole = roleRepository.findByName(RoleType.ADMIN);

            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setRole(adminRole);
            admin.setVerified(true);
            admin.setEnabled(true);

            Credentials credentials = new Credentials();
            credentials.setEmail(adminEmail);
            credentials.setPassword(passwordEncoder.encode(adminPassword));
            credentials.setUser(admin);
            admin.setCredentials(credentials);

            userRepository.save(admin);
            System.out.println("✅ Compte admin par défaut créé (username: " + adminUsername + ")");
        }
    }
}
