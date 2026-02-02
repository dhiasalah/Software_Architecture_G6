package com.example.project.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Tag(name = "Accueil", description = "Endpoint d'accueil de l'API")
public class HomeController {

    @GetMapping("/")
    @Operation(summary = "Page d'accueil", description = "Retourne les informations de base sur l'API")
    @ApiResponse(responseCode = "200", description = "Informations de l'API")
    public ResponseEntity<?> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Bienvenue sur l'API Spring Boot");
        response.put("status", "running");
        response.put("endpoints", Map.of(
            "register", "/api/auth/register",
            "login", "/api/auth/login"
        ));
        return ResponseEntity.ok(response);
    }
}
