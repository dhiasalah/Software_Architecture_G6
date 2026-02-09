package com.example.project.configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 🔐 JWT UTILS - Pour débutants
 *
 * JWT = JSON Web Token (Jeton Web JSON)
 * C'est comme un "badge électronique" qui prouve l'identité de l'utilisateur
 *
 * 📝 QU'EST-CE QU'UN JWT ?
 * Un JWT est un texte encodé qui contient 3 parties :
 * 1. HEADER (En-tête)     : Type de token et algorithme de signature
 * 2. PAYLOAD (Contenu)    : Les données (username, rôle, expiration, etc.)
 * 3. SIGNATURE (Signature): Pour vérifier que le token n'a pas été modifié
 *
 * Exemple de JWT :
 * eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6ImpvaG4iLCJleHAiOjE2MTYyMzk.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
 *        ↑ HEADER                    ↑ PAYLOAD (Claims)              ↑ SIGNATURE
 *
 * 🔍 QU'EST-CE QUE LES "CLAIMS" ?
 * Claims = Revendications/Déclarations
 * Ce sont les INFORMATIONS contenues dans le token (le PAYLOAD)
 *
 * Exemples de Claims :
 * - "sub" (subject)    : Le nom d'utilisateur (ex: "john_doe")
 * - "iat" (issued at)  : Date de création du token
 * - "exp" (expiration) : Date d'expiration du token
 * - "role"             : Le rôle de l'utilisateur (ex: "ADMIN")
 *
 * 🎯 POURQUOI UTILISER JWT ?
 * - L'utilisateur se connecte UNE FOIS
 * - Il reçoit un TOKEN
 * - Il envoie ce TOKEN à chaque requête (au lieu du mot de passe)
 * - Le serveur VÉRIFIE le token et sait qui est l'utilisateur
 *
 * 🔒 SÉCURITÉ
 * - Le token est SIGNÉ avec une clé secrète (secretKey)
 * - Si quelqu'un modifie le token, la signature sera invalide
 * - Le serveur refusera un token invalide ou expiré
 */
@Component
public class JwtUtils {

    /**
     * 🔑 CLÉ SECRÈTE
     * C'est le "mot de passe" utilisé pour signer et vérifier les tokens
     * IMPORTANT : Ne JAMAIS partager cette clé !
     * Elle est définie dans application.properties
     */
    @Value("${app.secret-key}")
    private String secretKey;

    /**
     * ⏰ DURÉE DE VIE DU TOKEN
     * Temps en millisecondes avant que le token expire
     * Exemple : 3600000 ms = 1 heure
     * Défini dans application.properties
     */
    @Value("${app.expiration-time}")
    private long expirationTime;

    /**
     * 🏗️ GÉNÉRER UN TOKEN
     *
     * Cette méthode crée un nouveau token JWT pour un utilisateur
     *
     * @param username Le nom d'utilisateur (ex: "john_doe")
     * @return Le token JWT sous forme de texte (ex: "eyJhbGciOiJIUzI1NiIs...")
     *
     * Exemple d'utilisation :
     * String token = jwtUtils.generateToken("john_doe");
     * // Retourne : "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     */
    public String generateToken(String username) {
        // Claims = Les informations qu'on veut mettre dans le token
        // Pour l'instant, on crée une Map vide (on pourrait ajouter le rôle, email, etc.)
        Map<String, Object> claims = new HashMap<>();

        // Créer le token avec ces informations
        return createToken(claims, username);
    }

    /**
     * 🔨 CRÉER LE TOKEN (méthode interne)
     *
     * Cette méthode construit réellement le token JWT
     *
     * @param claims Les informations supplémentaires à mettre dans le token
     * @param username Le nom d'utilisateur
     * @return Le token JWT complet
     *
     * Le token contient :
     * - Les claims (données personnalisées)
     * - Le subject (username)
     * - La date de création
     * - La date d'expiration
     * - Une signature pour la sécurité
     */
    private String createToken(Map<String, Object> claims, String username) {
        // Jwts.builder() = Constructeur de token JWT
        return Jwts.builder()
                .setClaims(claims)                    // Ajouter les infos personnalisées
                .setSubject(username)                 // Claim "sub" : le username
                .setIssuedAt(new Date(System.currentTimeMillis()))  // Claim "iat" : date de création
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime)) // Claim "exp" : date d'expiration
                .signWith(getSignKey(), SignatureAlgorithm.HS256)  // Signer avec la clé secrète
                .compact();                           // Convertir en texte
    }

    /**
     * 🔑 OBTENIR LA CLÉ DE SIGNATURE
     *
     * Convertit la clé secrète (String) en objet Key utilisable pour signer
     *
     * @return La clé de signature
     */
    private Key getSignKey() {
        // Convertir la clé secrète en tableau de bytes
        byte[] keyBytes = secretKey.getBytes();
        // Créer une clé de type HS256 (HMAC-SHA256)
        return new SecretKeySpec(keyBytes, SignatureAlgorithm.HS256.getJcaName());
    }

    /**
     * ✅ VALIDER UN TOKEN
     *
     * Vérifie si un token est valide et appartient bien à l'utilisateur
     *
     * @param token Le token JWT à vérifier
     * @param userDetails Les détails de l'utilisateur
     * @return true si le token est valide, false sinon
     *
     * Vérifie 2 choses :
     * 1. Le username dans le token correspond à l'utilisateur
     * 2. Le token n'est pas expiré
     *
     * Exemple :
     * if (jwtUtils.validateToken(token, userDetails)) {
     *     // Le token est valide, autoriser l'accès
     * } else {
     *     // Le token est invalide, refuser l'accès
     * }
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        // Extraire le username du token
        final String username = extractUsername(token);

        // Vérifier que le username correspond ET que le token n'est pas expiré
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * ⏰ VÉRIFIER SI LE TOKEN EST EXPIRÉ
     *
     * @param token Le token à vérifier
     * @return true si expiré, false sinon
     */
    private boolean isTokenExpired(String token) {
        // Récupérer la date d'expiration du token
        Date expirationDate = extractExpiration(token);

        // Comparer avec la date actuelle
        // Si la date d'expiration est AVANT maintenant → expiré
        return expirationDate.before(new Date());
    }

    /**
     * 📅 EXTRAIRE LA DATE D'EXPIRATION
     *
     * Récupère la date d'expiration stockée dans le token
     *
     * @param token Le token JWT
     * @return La date d'expiration
     */
    private Date extractExpiration(String token) {
        // Claims::getExpiration = Fonction qui récupère le claim "exp" (expiration)
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 👤 EXTRAIRE LE USERNAME
     *
     * Récupère le nom d'utilisateur stocké dans le token
     *
     * @param token Le token JWT
     * @return Le username (ex: "john_doe")
     *
     * Exemple :
     * String username = jwtUtils.extractUsername(token);
     * // Retourne : "john_doe"
     */
    public String extractUsername(String token) {
        // Claims::getSubject = Fonction qui récupère le claim "sub" (subject = username)
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 🔍 EXTRAIRE UN CLAIM SPÉCIFIQUE
     *
     * Méthode générique pour extraire n'importe quel claim du token
     *
     * @param token Le token JWT
     * @param claimsResolver La fonction pour extraire le claim voulu
     * @return La valeur du claim
     *
     * Cette méthode est "générique" car elle peut extraire n'importe quoi :
     * - Le username (extractClaim(token, Claims::getSubject))
     * - L'expiration (extractClaim(token, Claims::getExpiration))
     * - Etc.
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        // 1. Décoder le token et récupérer TOUS les claims
        final Claims claims = extractAllClaims(token);

        // 2. Appliquer la fonction pour extraire le claim voulu
        // claimsResolver.apply(claims) = exécuter la fonction (ex: Claims::getSubject)
        return claimsResolver.apply(claims);
    }

    /**
     * 📦 EXTRAIRE TOUS LES CLAIMS
     *
     * Décode le token et récupère TOUTES les informations qu'il contient
     *
     * @param token Le token JWT
     * @return Un objet Claims contenant toutes les infos du token
     *
     * Claims = Dictionnaire/Map des informations :
     * - "sub" : "john_doe"
     * - "iat" : 1616239022
     * - "exp" : 1616242622
     * - etc.
     *
     * IMPORTANT :
     * Si le token est invalide (modifié, signature incorrecte),
     * cette méthode lancera une exception
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSignKey())
                .parseClaimsJws(token)
                .getBody();
    }
}
