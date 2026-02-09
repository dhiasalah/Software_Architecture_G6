# 🔐 Guide JWT pour Débutants

## 📚 Qu'est-ce que JWT ?

**JWT** = **J**SON **W**eb **T**oken (Jeton Web JSON)

C'est comme un **badge électronique** qui prouve l'identité d'un utilisateur sans qu'il ait besoin de donner son mot de passe à chaque fois.

## 🎯 Pourquoi utiliser JWT ?

### Problème sans JWT
```
Utilisateur : "Je veux voir mes données"
Serveur     : "Qui êtes-vous ? Donnez-moi votre username et password"
Utilisateur : "john_doe / password123"
Serveur     : "OK, voici vos données"

Utilisateur : "Je veux modifier mes données"
Serveur     : "Qui êtes-vous ? Donnez-moi votre username et password"
Utilisateur : "john_doe / password123"  ← Répéter à chaque fois !
```

### Solution avec JWT
```
Utilisateur : "Je veux me connecter : john_doe / password123"
Serveur     : "OK, voici votre TOKEN : eyJhbGciOi..."
Utilisateur : "Je veux voir mes données. Voici mon TOKEN"
Serveur     : "Token valide ! Voici vos données"
Utilisateur : "Je veux modifier mes données. Voici mon TOKEN"
Serveur     : "Token valide ! Modification effectuée"
```

✅ **L'utilisateur donne son mot de passe UNE SEULE FOIS** et utilise ensuite le token !

## 🏗️ Structure d'un JWT

Un JWT est un **long texte** composé de **3 parties** séparées par des points (`.`) :

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6ImpvaG4iLCJleHAiOjE2MTYyMzk.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
        ↑ PARTIE 1: HEADER              ↑ PARTIE 2: PAYLOAD (Claims)        ↑ PARTIE 3: SIGNATURE
```

### 1️⃣ HEADER (En-tête)
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```
- **alg** : Algorithme de signature utilisé (HS256 = HMAC-SHA256)
- **typ** : Type de token (JWT)

### 2️⃣ PAYLOAD (Contenu = Claims)
```json
{
  "sub": "john_doe",
  "iat": 1616239022,
  "exp": 1616242622
}
```
- **sub** (subject) : Le nom d'utilisateur
- **iat** (issued at) : Date de création (en secondes depuis 1970)
- **exp** (expiration) : Date d'expiration

### 3️⃣ SIGNATURE (Sécurité)
```
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secretKey
)
```
- Calculée avec la **clé secrète** du serveur
- Permet de **vérifier que le token n'a pas été modifié**

## 🔍 Qu'est-ce que les CLAIMS ?

**Claims** = Revendications / Déclarations / Informations

Ce sont les **données stockées dans le token** (la partie PAYLOAD).

### Claims Standards (définis par JWT)

| Claim | Nom complet | Description | Exemple |
|-------|-------------|-------------|---------|
| **sub** | Subject | Sujet (généralement le username) | "john_doe" |
| **iat** | Issued At | Date de création du token | 1616239022 |
| **exp** | Expiration | Date d'expiration du token | 1616242622 |
| **iss** | Issuer | Qui a créé le token | "mon-serveur.com" |
| **aud** | Audience | Pour qui est le token | "mon-app.com" |

### Claims Personnalisés (vous pouvez en ajouter)

```json
{
  "sub": "john_doe",
  "role": "ADMIN",
  "email": "john@example.com",
  "permissions": ["READ", "WRITE", "DELETE"]
}
```

## 🔄 Flux d'authentification avec JWT

```
┌─────────┐                                              ┌─────────┐
│         │  1. POST /api/auth/login                     │         │
│         │     {username: "john", password: "pass"}     │         │
│         │ ──────────────────────────────────────────►  │         │
│         │                                              │         │
│ CLIENT  │  2. Vérification des credentials              │ SERVEUR │
│         │     ✓ Username correct                        │         │
│         │     ✓ Password correct                        │         │
│         │                                              │         │
│         │  3. Génération du JWT Token                  │         │
│         │     token = jwtUtils.generateToken("john")   │         │
│         │                                              │         │
│         │  4. Réponse avec le token                    │         │
│         │ ◄──────────────────────────────────────────  │         │
│         │     {token: "eyJhbGc...", type: "Bearer"}    │         │
│         │                                              │         │
│         │  5. Stockage du token                        │         │
│         │     localStorage.setItem("token", ...)       │         │
│         │                                              │         │
│         │  6. Requête avec le token                    │         │
│         │     GET /api/users                           │         │
│         │     Header: Authorization: Bearer eyJhbGc... │         │
│         │ ──────────────────────────────────────────►  │         │
│         │                                              │         │
│         │  7. Validation du token                      │         │
│         │     ✓ Signature valide ?                     │         │
│         │     ✓ Token expiré ?                         │         │
│         │     ✓ Username existe ?                      │         │
│         │                                              │         │
│         │  8. Réponse avec les données                 │         │
│         │ ◄──────────────────────────────────────────  │         │
│         │     [{id: 1, username: "john"}, ...]         │         │
└─────────┘                                              └─────────┘
```

## 💻 Code Java : Comment ça marche

### 1. Générer un Token (Login)

```java
// Dans AuthController.java

@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody User loginUser) {
    // 1. Vérifier username et password
    Authentication auth = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(username, password)
    );
    
    if(auth.isAuthenticated()) {
        // 2. Créer le token JWT
        String token = jwtUtils.generateToken(username);
        // token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        
        // 3. Retourner le token au client
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("type", "Bearer");
        return ResponseEntity.ok(response);
    }
}
```

### 2. Valider un Token (à chaque requête)

```java
// Dans JwtFilter.java (filtre de sécurité)

@Override
protected void doFilterInternal(HttpServletRequest request, ...) {
    // 1. Extraire le token du header "Authorization"
    String authHeader = request.getHeader("Authorization");
    // authHeader = "Bearer eyJhbGciOiJIUzI1NiIs..."
    
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7); // Retirer "Bearer "
        
        // 2. Extraire le username du token
        String username = jwtUtils.extractUsername(token);
        // username = "john_doe"
        
        // 3. Charger l'utilisateur de la base de données
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        
        // 4. Valider le token
        if (jwtUtils.validateToken(token, userDetails)) {
            // ✅ Token valide ! Autoriser l'accès
            // Créer l'authentification dans Spring Security
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
                );
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
    }
    
    // Continuer avec la requête
    filterChain.doFilter(request, response);
}
```

## 🔒 Sécurité du JWT

### ✅ Avantages

1. **Pas de session côté serveur** : Le serveur n'a pas besoin de stocker les tokens
2. **Scalable** : Fonctionne avec plusieurs serveurs
3. **Auto-contenu** : Le token contient toutes les infos nécessaires
4. **Signature cryptographique** : Impossible de modifier sans la clé secrète

### ⚠️ Points importants

1. **Le token n'est PAS crypté** : N'importe qui peut le décoder et lire les claims
   - ❌ Ne JAMAIS mettre de données sensibles (mot de passe, numéro de carte bancaire)
   - ✅ OK pour : username, rôle, email

2. **Protéger la clé secrète** : Si quelqu'un obtient votre `secretKey`, il peut créer des faux tokens
   - Stocker dans `application.properties` (ne JAMAIS commit dans Git)
   - Utiliser des variables d'environnement en production

3. **Expiration** : Les tokens doivent avoir une durée de vie limitée
   - Exemple : 1 heure, 24 heures
   - Après expiration, l'utilisateur doit se reconnecter

4. **HTTPS** : Toujours utiliser HTTPS en production pour éviter l'interception

## 🛠️ Configuration dans application.properties

```properties
# Clé secrète pour signer les tokens (minimum 256 bits pour HS256)
app.secret-key=VotreCleSecreteTresLongueEtComplexePourLaSecurite123456789

# Durée de vie du token en millisecondes
# 3600000 ms = 1 heure
# 86400000 ms = 24 heures
app.expiration-time=3600000
```

## 📝 Exemples pratiques

### Test avec cURL

```bash
# 1. Se connecter et obtenir le token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "credentials": {
      "password": "password123"
    }
  }'

# Réponse :
# {
#   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#   "type": "Bearer"
# }

# 2. Utiliser le token pour accéder à une ressource protégée
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Décoder un JWT (pour voir les claims)

Allez sur https://jwt.io et collez votre token :

```
Token : eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdCI6MTYxNjIzOTAyMiwiZXhwIjoxNjE2MjQyNjIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c

Décodé :
HEADER:
{
  "alg": "HS256",
  "typ": "JWT"
}

PAYLOAD (Claims):
{
  "sub": "john_doe",
  "iat": 1616239022,
  "exp": 1616242622
}
```

## 🔍 Claims en détail - Explication simple

Imaginez les **Claims** comme une **carte d'identité électronique** :

```
┌─────────────────────────────────┐
│   CARTE D'IDENTITÉ JWT          │
├─────────────────────────────────┤
│ Nom (sub)     : john_doe        │
│ Rôle (role)   : ADMIN           │
│ Créée le (iat): 2024-03-21 14:00│
│ Expire le (exp): 2024-03-21 15:00│
│                                 │
│ Signature : ✓ Valide            │
└─────────────────────────────────┘
```

Chaque **claim** est une **information** dans cette carte :
- **sub** (subject) = Le nom de la personne
- **iat** (issued at) = Quand la carte a été créée
- **exp** (expiration) = Quand la carte expire

Le serveur peut **lire** ces informations pour savoir :
- Qui est l'utilisateur
- Si son "badge" est encore valide
- Quels sont ses droits (rôle ADMIN ou USER)

## 🎓 Résumé pour les débutants

1. **JWT = Badge électronique** qui prouve l'identité
2. **Claims = Informations** contenues dans le badge
3. **Le token contient 3 parties** : Header, Payload (Claims), Signature
4. **Workflow** :
   - Login → Recevoir le token
   - Stocker le token
   - Envoyer le token à chaque requête
   - Le serveur vérifie le token
5. **Le token n'est PAS crypté** mais il est **signé** (impossible de modifier)
6. **Le token expire** après un certain temps

## 📚 Pour aller plus loin

- Documentation JWT : https://jwt.io/introduction
- Tester vos tokens : https://jwt.io
- RFC 7519 (spécification JWT) : https://tools.ietf.org/html/rfc7519
