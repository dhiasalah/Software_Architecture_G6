package com.example.project.controller;

import com.example.project.configuration.JwtUtils;
import com.example.project.dto.LoginRequest;
import com.example.project.dto.RegisterRequest;
import com.example.project.entity.Credentials;
import com.example.project.entity.Role;
import com.example.project.entity.RoleType;
import com.example.project.entity.User;
import com.example.project.repository.CredentialsRepository;
import com.example.project.repository.RoleRepository;
import com.example.project.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "API pour l'inscription et la connexion des utilisateurs")
public class AuthController {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CredentialsRepository credentialsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    @Operation(summary = "Inscription d'un nouvel utilisateur", description = "Crée un nouveau compte utilisateur avec le rôle spécifié (ADMIN ou USER)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Utilisateur créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Nom d'utilisateur, email ou téléphone déjà pris")
    })
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if(request.getUsername() == null || request.getEmail() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest().body("Username, email and password are required");
        }

        if(userRepository.findByUsername(request.getUsername()) != null) {
            return ResponseEntity.badRequest().body("Username is already taken");
        }

        if(credentialsRepository.findByEmail(request.getEmail()) != null) {
            return ResponseEntity.badRequest().body("Email is already taken");
        }

        if(request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
            if(credentialsRepository.findByPhoneNumber(request.getPhoneNumber()) != null) {
                return ResponseEntity.badRequest().body("Phone number is already taken");
            }
        }

        RoleType roleType = (request.getRoleType() != null) ? request.getRoleType() : RoleType.USER;
        Role userRole = roleRepository.findByName(roleType);
        if(userRole == null) {
            return ResponseEntity.badRequest().body("Role not found: " + roleType + ". Please contact administrator.");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setRole(userRole);

        Credentials credentials = new Credentials();
        credentials.setEmail(request.getEmail());
        credentials.setPhoneNumber(request.getPhoneNumber());
        credentials.setPassword(passwordEncoder.encode(request.getPassword()));
        credentials.setUser(user);

        user.setCredentials(credentials);

        User savedUser = userRepository.save(user);

        return ResponseEntity.ok(savedUser);
    }

    @PostMapping("/login")
    @Operation(summary = "Connexion d'un utilisateur", description = "Authentifie un utilisateur et retourne un token JWT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentification réussie",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Identifiants invalides")
    })
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try{
            if(loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
                return ResponseEntity.badRequest().body("Username and password are required");
            }

            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );

            if(authentication.isAuthenticated()) {
                Map<String,Object> authData = new HashMap<>();
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

}
