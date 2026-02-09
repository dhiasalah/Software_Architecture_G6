# 🚀 Guide de Test - API d'Authentification

## Prérequis

Assurez-vous que l'application est démarrée :
```bash
./mvnw spring-boot:run
```

---

## 🔧 Tests avec cURL (Windows PowerShell)

### 1. Inscription d'un nouvel utilisateur

```powershell
$body = @{
    username = "john"
    password = "password123"
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:8080/api/auth/register" `
    -Method POST `
    -ContentType "application/json" `
    -Body $body
```

**Réponse attendue** :
```json
{
  "id": 1,
  "username": "john",
  "password": "$2a$10$...", 
  "role": {
    "id": 1,
    "name": "USER",
    "description": "Utilisateur standard avec permissions de base"
  }
}
```

---

### 2. Connexion avec l'utilisateur créé

```powershell
$body = @{
    username = "john"
    password = "password123"
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" `
    -Method POST `
    -ContentType "application/json" `
    -Body $body
```

**Réponse attendue** :
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huIiwiaWF0IjoxNzA5...",
  "type": "Bearer"
}
```

---

### 3. Tester la page d'accueil protégée

```powershell
$token = "VOTRE_TOKEN_ICI"

Invoke-WebRequest -Uri "http://localhost:8080/api/home" `
    -Method GET `
    -Headers @{ "Authorization" = "Bearer $token" }
```

**Réponse attendue** :
```
Welcome to the home page
```

---

## 🧪 Tests avec Postman

### Collection Postman

#### 1. Register (Inscription)
- **Méthode** : `POST`
- **URL** : `http://localhost:8080/api/auth/register`
- **Headers** :
  - `Content-Type: application/json`
- **Body (raw JSON)** :
```json
{
  "username": "john",
  "password": "password123"
}
```

#### 2. Login (Connexion)
- **Méthode** : `POST`
- **URL** : `http://localhost:8080/api/auth/login`
- **Headers** :
  - `Content-Type: application/json`
- **Body (raw JSON)** :
```json
{
  "username": "john",
  "password": "password123"
}
```

#### 3. Home (Page protégée)
- **Méthode** : `GET`
- **URL** : `http://localhost:8080/api/home`
- **Headers** :
  - `Authorization: Bearer VOTRE_TOKEN`

---

## 🧪 Scénarios de Test

### ✅ Test 1 : Inscription réussie
```json
POST /api/auth/register
{
  "username": "alice",
  "password": "secure123"
}

→ Statut : 200 OK
→ Utilisateur créé avec rôle "USER"
```

---

### ❌ Test 2 : Inscription avec username déjà pris
```json
POST /api/auth/register
{
  "username": "alice",  ← Déjà utilisé !
  "password": "autre"
}

→ Statut : 400 Bad Request
→ Message : "Username is already taken"
```

---

### ✅ Test 3 : Connexion réussie
```json
POST /api/auth/login
{
  "username": "alice",
  "password": "secure123"
}

→ Statut : 200 OK
→ Token JWT retourné
```

---

### ❌ Test 4 : Connexion avec mauvais mot de passe
```json
POST /api/auth/login
{
  "username": "alice",
  "password": "wrong"  ← Mauvais mot de passe !
}

→ Statut : 401 Unauthorized
→ Message : "Invalid credentials"
```

---

### ❌ Test 5 : Accès sans token
```
GET /api/home
(Sans header Authorization)

→ Statut : 403 Forbidden
→ Accès refusé
```

---

### ✅ Test 6 : Accès avec token valide
```
GET /api/home
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

→ Statut : 200 OK
→ Message : "Welcome to the home page"
```

---

## 🔍 Vérifier les Rôles dans la Base

Si vous utilisez H2 Console :

1. Accédez à : `http://localhost:8080/h2-console`
2. Connectez-vous avec les paramètres dans `application.properties`
3. Exécutez cette requête :

```sql
-- Voir tous les rôles
SELECT * FROM roles;

-- Voir tous les utilisateurs avec leurs rôles
SELECT u.id, u.username, r.name as role
FROM users u
JOIN roles r ON u.role_id = r.id;
```

---

## 📊 Résultats Attendus en Base

### Table `roles`
```
+----+-----------+----------------------------------+
| id | name      | description                      |
+----+-----------+----------------------------------+
| 1  | USER      | Utilisateur standard...          |
| 2  | ADMIN     | Administrateur...                |
| 3  | MODERATOR | Modérateur...                    |
+----+-----------+----------------------------------+
```

### Table `users` (après inscription de "john")
```
+----+----------+-------------------------------+---------+
| id | username | password                      | role_id |
+----+----------+-------------------------------+---------+
| 1  | john     | $2a$10$xxxxxxxxxxxxxxxxxxxxx      | 1       |
+----+----------+-------------------------------+---------+
```

---

## 🐛 Résolution de Problèmes

### Problème : "Default role not found"
**Cause** : Les rôles n'ont pas été créés au démarrage

**Solution** :
1. Vérifiez que `DataInitializer` s'est exécuté
2. Regardez les logs au démarrage :
   ```
   ✅ Rôle USER créé avec succès
   ✅ Rôle ADMIN créé avec succès
   ✅ Rôle MODERATOR créé avec succès
   ```
3. Si absent, vérifiez la configuration de la base de données

---

### Problème : "Username is already taken"
**Cause** : L'utilisateur existe déjà dans la base

**Solution** :
- Utilisez un autre username
- OU supprimez la base H2 et redémarrez (si en mode dev)

---

### Problème : Token invalide (403 Forbidden)
**Cause** : Token expiré ou malformé

**Solution** :
1. Reconnectez-vous pour obtenir un nouveau token
2. Vérifiez que le token est bien dans le header `Authorization: Bearer TOKEN`
3. Vérifiez la configuration JWT dans `application.properties`

---

## 📝 Script PowerShell Complet

Sauvegardez ce script dans `test-api.ps1` :

```powershell
# Script de test complet
$baseUrl = "http://localhost:8080/api/auth"

Write-Host "🚀 Test 1: Inscription" -ForegroundColor Green
$register = @{
    username = "testuser"
    password = "test123"
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "$baseUrl/register" `
        -Method POST `
        -ContentType "application/json" `
        -Body $register
    Write-Host "✅ Inscription réussie" -ForegroundColor Green
    $response.Content | ConvertFrom-Json | Format-List
} catch {
    Write-Host "❌ Erreur lors de l'inscription" -ForegroundColor Red
    $_.Exception.Message
}

Start-Sleep -Seconds 2

Write-Host "`n🔐 Test 2: Connexion" -ForegroundColor Green
$login = @{
    username = "testuser"
    password = "test123"
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "$baseUrl/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $login
    Write-Host "✅ Connexion réussie" -ForegroundColor Green
    $tokenData = $response.Content | ConvertFrom-Json
    $tokenData | Format-List
    
    $token = $tokenData.token
    Write-Host "`n🏠 Test 3: Accès page protégée" -ForegroundColor Green
    
    $homeResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/home" `
        -Method GET `
        -Headers @{ "Authorization" = "Bearer $token" }
    Write-Host "✅ Accès réussi: $($homeResponse.Content)" -ForegroundColor Green
    
} catch {
    Write-Host "❌ Erreur" -ForegroundColor Red
    $_.Exception.Message
}
```

**Exécution** :
```powershell
.\test-api.ps1
```

---

## 🎓 Comprendre les Réponses

### Réponse d'Inscription
```json
{
  "id": 1,                    ← ID unique de l'utilisateur
  "username": "john",         ← Nom d'utilisateur
  "password": "$2a$10$...",   ← Mot de passe ENCODÉ (BCrypt)
  "role": {
    "id": 1,                  ← ID du rôle
    "name": "USER",           ← Nom du rôle assigné
    "description": "..."      ← Description du rôle
  }
}
```

### Réponse de Connexion
```json
{
  "token": "eyJ...",          ← Token JWT à utiliser pour l'auth
  "type": "Bearer"            ← Type d'authentification
}
```

**Comment utiliser le token** :
```
Authorization: Bearer eyJ...
                ^     ^
                |     |
              type  token
```

---

**🎉 Vous êtes prêt à tester votre API !**
