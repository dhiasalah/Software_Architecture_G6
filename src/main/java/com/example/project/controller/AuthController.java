package com.example.project.controller;

import com.example.project.configuration.JwtUtils;
import com.example.project.dto.LoginRequest;
import com.example.project.dto.RegisterRequest;
import com.example.project.dto.UserRegisteredEvent;
import com.example.project.dto.VerifyTokenResponse;
import com.example.project.entity.*;
import com.example.project.repository.CredentialsRepository;
import com.example.project.repository.RoleRepository;
import com.example.project.repository.UserRepository;
import com.example.project.repository.VerificationTokenRepository;
import com.example.project.service.CustomUserDetailsService;
import com.example.project.service.TokenBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * CONTROLLER AUTH - Inscription, Connexion et Vérification d'e-mail
 *
 * Ce contrôleur gère 3 endpoints :
 *
 * 1. POST /api/auth/register → Inscription d'un nouvel utilisateur
 *    - Crée l'utilisateur en base (verified=false)
 *    - Génère un token de vérification (stocke le HASH, pas le token en clair)
 *    - Publie un événement "UserRegistered" dans RabbitMQ
 *    - Le service Notification écoute cet événement et envoie un e-mail
 *
 * 2. GET /api/auth/verify?tokenId=...&t=... → Vérification de l'e-mail
 *    - Reçoit le tokenId et le token en clair (t) depuis le lien dans l'e-mail
 *    - Compare le hash BCrypt du token reçu avec le hash stocké en base
 *    - Si OK et non expiré → marque l'utilisateur comme vérifié
 *    - Supprime le token (usage unique)
 *
 * 3. POST /api/auth/login → Connexion (déjà existant)
 *
 * FLUX COMPLET :
 * register() → sauve User(verified=false) → sauve VerificationToken(hash)
 *            → publie UserRegisteredEvent dans RabbitMQ
 *            → NotificationListener reçoit → envoie e-mail avec lien
 *            → utilisateur clique le lien → verify() → verified=true
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "API pour l'inscription, la connexion et la vérification d'e-mail")
public class AuthController {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CredentialsRepository credentialsRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;

    /**
     * Service de blacklist des tokens JWT (pour le logout)
     * Quand un utilisateur se déconnecte, son token est ajouté à la blacklist
     * et ne sera plus accepté par le JwtFilter
     */
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * RabbitTemplate = l'outil pour PUBLIER des messages dans RabbitMQ
     * C'est l'équivalent de "déposer une lettre au bureau de poste"
     * Spring l'injecte automatiquement grâce à spring-boot-starter-amqp
     */
    private final RabbitTemplate rabbitTemplate;

    /** Nom de l'exchange RabbitMQ (lu depuis application.properties) */
    @Value("${app.mq.exchange}")
    private String exchange;

    /** Routing key pour les événements d'inscription (lu depuis application.properties) */
    @Value("${app.mq.rk.user-registered}")
    private String userRegisteredRoutingKey;

    /** Durée de vie du token en minutes (lu depuis application.properties) */
    @Value("${app.verification-token.expiration-minutes}")
    private int tokenExpirationMinutes;

    // =====================================================================
    // ENDPOINT 1 : POST /api/auth/register - INSCRIPTION
    // =====================================================================

    @PostMapping("/register")
    @Operation(
        summary = "Inscription d'un nouvel utilisateur",
        description = "Crée le compte (verified=false), génère un token de vérification, " +
                      "et publie un événement RabbitMQ pour envoyer l'e-mail de vérification"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Utilisateur créé, e-mail de vérification envoyé"),
            @ApiResponse(responseCode = "400", description = "Données invalides ou déjà utilisées")
    })
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        // --- ÉTAPE 1 : Validations de base ---
        if (request.getUsername() == null || request.getEmail() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest().body("Username, email and password are required");
        }

        if (userRepository.findByUsername(request.getUsername()) != null) {
            return ResponseEntity.badRequest().body("Username is already taken");
        }

        if (credentialsRepository.findByEmail(request.getEmail()) != null) {
            return ResponseEntity.badRequest().body("Email is already taken");
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
            if (credentialsRepository.findByPhoneNumber(request.getPhoneNumber()) != null) {
                return ResponseEntity.badRequest().body("Phone number is already taken");
            }
        }

        // --- ÉTAPE 2 : Créer l'utilisateur en base (verified = false) ---
        RoleType roleType = (request.getRoleType() != null) ? request.getRoleType() : RoleType.USER;
        Role userRole = roleRepository.findByName(roleType);
        if (userRole == null) {
            return ResponseEntity.badRequest().body("Role not found: " + roleType);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setRole(userRole);
        user.setVerified(false);  // PAS ENCORE VÉRIFIÉ → il doit cliquer le lien dans l'e-mail

        Credentials credentials = new Credentials();
        credentials.setEmail(request.getEmail());
        credentials.setPhoneNumber(request.getPhoneNumber());
        credentials.setPassword(passwordEncoder.encode(request.getPassword()));
        credentials.setUser(user);
        user.setCredentials(credentials);

        User savedUser = userRepository.save(user);
        System.out.println("✅ Utilisateur créé : " + savedUser.getUsername() + " (verified=false)");

        // --- ÉTAPE 3 : Générer le token de vérification ---
        // tokenId = identifiant public (pour retrouver le token en base)
        // tokenClear = le vrai secret (envoyé dans l'e-mail, JAMAIS stocké en base)
        // tokenHash = BCrypt(tokenClear) → c'est CE QUI EST STOCKÉ en base
        String tokenId = UUID.randomUUID().toString();
        String tokenClear = UUID.randomUUID().toString();
        String tokenHash = passwordEncoder.encode(tokenClear);

        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setTokenId(tokenId);
        verificationToken.setTokenHash(tokenHash);  // On stocke le HASH, pas le token en clair !
        verificationToken.setExpiresAt(LocalDateTime.now().plusMinutes(tokenExpirationMinutes));
        verificationToken.setUser(savedUser);

        verificationTokenRepository.save(verificationToken);
        System.out.println("🔑 Token de vérification créé : tokenId=" + tokenId + " (hash stocké, pas le token en clair)");

        // --- ÉTAPE 4 : Publier l'événement dans RabbitMQ ---
        // C'est ICI que le DÉCOUPLAGE se fait :
        // Auth ne sait PAS comment l'e-mail sera envoyé.
        // Il publie juste un fait ("un utilisateur s'est inscrit") et c'est tout.
        // Le service Notification écoute et fait son travail de son côté.
        UserRegisteredEvent event = new UserRegisteredEvent(
                UUID.randomUUID().toString(),  // eventId unique (pour traçabilité)
                savedUser.getId(),             // userId
                request.getEmail(),            // email du destinataire
                tokenId,                       // identifiant public du token
                tokenClear,                    // token en clair (pour que Notification construise le lien)
                Instant.now()                  // horodatage de l'événement
        );

        rabbitTemplate.convertAndSend(exchange, userRegisteredRoutingKey, event);
        System.out.println("📨 Événement UserRegistered publié dans RabbitMQ (exchange=" + exchange + ", rk=" + userRegisteredRoutingKey + ")");

        // --- ÉTAPE 5 : Retourner la réponse ---
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Inscription réussie ! Vérifiez votre e-mail pour activer votre compte.");
        response.put("userId", savedUser.getId());
        response.put("username", savedUser.getUsername());
        response.put("verified", false);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =====================================================================
    // ENDPOINT 2 : GET /api/auth/verify - VÉRIFICATION DE L'E-MAIL
    // =====================================================================

    @GetMapping("/verify")
    @Transactional  // @Transactional car on fait un DELETE + UPDATE dans la même opération
    @Operation(
        summary = "Vérifier l'e-mail d'un utilisateur",
        description = "Valide le token reçu par e-mail. Si le token est correct et non expiré, " +
                      "le compte est marqué comme vérifié et le token est supprimé (usage unique)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "E-mail vérifié avec succès"),
            @ApiResponse(responseCode = "400", description = "Token invalide, expiré ou déjà utilisé")
    })
    public ResponseEntity<?> verify(
            @RequestParam String tokenId,  // Identifiant public du token (pour le retrouver en base)
            @RequestParam String t         // Le token en clair (le secret envoyé dans l'e-mail)
    ) {
        System.out.println("🔍 Tentative de vérification : tokenId=" + tokenId);

        // --- ÉTAPE 1 : Retrouver le token en base par son identifiant public ---
        VerificationToken verificationToken = verificationTokenRepository.findByTokenId(tokenId);

        if (verificationToken == null) {
            System.out.println("❌ Token introuvable : tokenId=" + tokenId);
            return ResponseEntity.badRequest().body("Token invalide ou déjà utilisé");
        }

        // --- ÉTAPE 2 : Vérifier l'expiration ---
        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            System.out.println("❌ Token expiré : tokenId=" + tokenId + " (expiresAt=" + verificationToken.getExpiresAt() + ")");
            // On supprime le token expiré pour nettoyer la base
            verificationTokenRepository.deleteByTokenId(tokenId);
            return ResponseEntity.badRequest().body("Token expiré. Veuillez vous réinscrire ou demander un nouveau lien.");
        }

        // --- ÉTAPE 3 : Comparer le hash ---
        // passwordEncoder.matches(tokenClair, hash) fait la comparaison BCrypt
        // C'est exactement le même principe que pour vérifier un mot de passe
        if (!passwordEncoder.matches(t, verificationToken.getTokenHash())) {
            System.out.println("❌ Token invalide (hash ne correspond pas) : tokenId=" + tokenId);
            return ResponseEntity.badRequest().body("Token invalide");
        }

        // --- ÉTAPE 4 : Marquer l'utilisateur comme vérifié ---
        User user = verificationToken.getUser();

        // Idempotence : si déjà vérifié, on ne plante pas
        if (Boolean.TRUE.equals(user.getVerified())) {
            System.out.println("ℹ️ Utilisateur déjà vérifié : " + user.getUsername());
            verificationTokenRepository.deleteByTokenId(tokenId);
            return ResponseEntity.ok("Votre e-mail est déjà vérifié !");
        }

        user.setVerified(true);
        userRepository.save(user);
        System.out.println("✅ Utilisateur vérifié avec succès : " + user.getUsername());

        // --- ÉTAPE 5 : Supprimer le token (usage unique = one-shot) ---
        verificationTokenRepository.deleteByTokenId(tokenId);
        System.out.println("🗑️ Token supprimé (one-shot) : tokenId=" + tokenId);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("message", "E-mail vérifié avec succès ! Votre compte est maintenant actif.");
        responseBody.put("username", user.getUsername());
        responseBody.put("verified", true);

        return ResponseEntity.ok(responseBody);
    }

    // =====================================================================
    // ENDPOINT 3 : POST /api/auth/login - CONNEXION (existant)
    // =====================================================================

    @PostMapping("/login")
    @Operation(summary = "Connexion d'un utilisateur", description = "Authentifie un utilisateur et retourne un token JWT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentification réussie",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Identifiants invalides")
    })
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            if (loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
                return ResponseEntity.badRequest().body("Username and password are required");
            }

            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );

            if (authentication.isAuthenticated()) {
                Map<String, Object> authData = new HashMap<>();
                String token = jwtUtils.generateToken(loginRequest.getUsername());
                authData.put("token", token);
                authData.put("type", "Bearer");
                return ResponseEntity.ok(authData);
            }
            return ResponseEntity.status(401).body("Invalid credentials");
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }

    // =====================================================================
    // ENDPOINT 4 : POST /api/auth/logout - DÉCONNEXION
    // =====================================================================

    @PostMapping("/logout")
    @Operation(
        summary = "Déconnexion d'un utilisateur",
        description = "Invalide le token JWT en l'ajoutant à une blacklist. " +
                      "Le token ne sera plus accepté par le serveur même s'il n'est pas encore expiré. " +
                      "Le client doit envoyer le header Authorization avec le token à invalider."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Déconnexion réussie"),
            @ApiResponse(responseCode = "400", description = "Token absent ou invalide")
    })
    public ResponseEntity<?> logout(jakarta.servlet.http.HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        // --- ÉTAPE 1 : Vérifier que le header Authorization est présent ---
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Missing or invalid Authorization header");
        }

        // --- ÉTAPE 2 : Extraire le token et l'ajouter à la blacklist ---
        String jwt = authHeader.substring(7); // Enlever "Bearer "
        tokenBlacklistService.blacklist(jwt, jwtUtils.extractExpiration(jwt));
        System.out.println("🚪 Logout : token blacklisté pour " + jwtUtils.extractUsername(jwt));

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Déconnexion réussie. Le token a été invalidé.");
        return ResponseEntity.ok(response);
    }

    // =====================================================================
    // ENDPOINT 5 : GET /api/auth/validate - VALIDATION JWT (pour Nginx)
    // =====================================================================

    @GetMapping("/validate")
    @Operation(
        summary = "Valider un token JWT (utilise par Nginx)",
        description = "Endpoint appele par Nginx en subrequest (auth_request). " +
                      "Verifie que le header Authorization contient un JWT valide. " +
                      "Retourne 200 si valide, 401 sinon. " +
                      "Nginx utilise ce resultat pour autoriser ou bloquer la requete du client."
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token JWT valide"),
            @ApiResponse(responseCode = "401", description = "Token absent, invalide ou expire")
    })
    public ResponseEntity<?> validate(jakarta.servlet.http.HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        System.out.println("🔐 [Nginx auth_request] Validation JWT...");

        // --- ETAPE 1 : Verifier que le header Authorization est present ---
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("❌ [Nginx auth_request] Pas de token Bearer dans le header");
            return ResponseEntity.status(401).body("Missing or invalid Authorization header");
        }

        // --- ETAPE 2 : Extraire et valider le token ---
        String jwt = authHeader.substring(7); // Enlever "Bearer "
        try {
            String username = jwtUtils.extractUsername(jwt);

            if (username == null) {
                System.out.println("❌ [Nginx auth_request] Username null dans le token");
                return ResponseEntity.status(401).body("Invalid token");
            }

            // Charger le user pour verifier que le token est toujours valide
            var userDetails = customUserDetailsService.loadUserByUsername(username);

            if (!jwtUtils.validateToken(jwt, userDetails)) {
                System.out.println("❌ [Nginx auth_request] Token expire ou invalide pour " + username);
                return ResponseEntity.status(401).body("Token expired or invalid");
            }

            System.out.println("✅ [Nginx auth_request] Token valide pour " + username);

            // On retourne 200 + le username dans un header custom
            // Nginx peut transmettre ce header au service backend
            return ResponseEntity.ok()
                    .header("X-Auth-User", username)
                    .body(new VerifyTokenResponse(true, username, "Token valid"));

        } catch (Exception e) {
            System.out.println("❌ [Nginx auth_request] Erreur validation : " + e.getMessage());
            return ResponseEntity.status(401).body("Invalid token: " + e.getMessage());
        }
    }

}
