package com.example.project.configuration;

import com.example.project.entity.Role;
import com.example.project.entity.RoleType;
import com.example.project.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 🚀 INITIALISATEUR DE DONNÉES - Pour débutants
 *
 * Cette classe s'exécute automatiquement au DÉMARRAGE de l'application
 *
 * Pourquoi ?
 * - Pour créer les rôles par défaut dans la base de données
 * - Sans cela, l'inscription échouerait car le rôle "USER" n'existe pas
 *
 * ✨ NOUVEAU : Utilise l'Enum RoleType pour garantir uniquement ADMIN et USER
 *
 * @Component : Indique à Spring de gérer cette classe
 * CommandLineRunner : Interface qui s'exécute après le démarrage
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    /**
     * Cette méthode s'exécute automatiquement au démarrage
     */
    @Override
    public void run(String... args) throws Exception {
        // Créer le rôle USER s'il n'existe pas
        if (roleRepository.findByName(RoleType.USER) == null) {
            Role userRole = new Role();
            userRole.setName(RoleType.USER);
            userRole.setDescription(RoleType.USER.getDescription());
            roleRepository.save(userRole);
            System.out.println("✅ Rôle USER créé avec succès");
        }

        // Créer le rôle ADMIN s'il n'existe pas
        if (roleRepository.findByName(RoleType.ADMIN) == null) {
            Role adminRole = new Role();
            adminRole.setName(RoleType.ADMIN);
            adminRole.setDescription(RoleType.ADMIN.getDescription());
            roleRepository.save(adminRole);
            System.out.println("✅ Rôle ADMIN créé avec succès");
        }
    }
}
