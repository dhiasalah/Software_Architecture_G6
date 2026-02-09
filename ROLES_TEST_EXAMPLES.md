# 🧪 Exemples de requêtes pour tester les rôles

## Via cURL

### 1. Créer un utilisateur avec le rôle USER
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user1",
    "password": "password123",
    "roleName": "USER"
  }'
```

### 2. Créer un utilisateur avec le rôle ADMIN
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin1",
    "password": "adminpass",
    "roleName": "ADMIN"
  }'
```

### 3. Créer un utilisateur sans spécifier de rôle (USER par défaut)
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "defaultuser",
    "password": "password123"
  }'
```

## Via Swagger UI

1. Accédez à : `http://localhost:8080/swagger-ui.html`
2. Trouvez l'endpoint **POST /api/auth/register**
3. Cliquez sur "Try it out"
4. Utilisez l'un des exemples suivants :

### Exemple 1 : Utilisateur USER
```json
{
  "username": "john_doe",
  "password": "mypassword",
  "roleName": "USER"
}
```

### Exemple 2 : Utilisateur ADMIN
```json
{
  "username": "admin_user",
  "password": "securepassword",
  "roleName": "ADMIN"
}
```

### Exemple 3 : Sans rôle spécifié (USER par défaut)
```json
{
  "username": "simple_user",
  "password": "password123"
}
```

## Via Postman

1. **Méthode** : POST
2. **URL** : `http://localhost:8080/api/auth/register`
3. **Headers** : 
   - `Content-Type: application/json`
4. **Body** (raw JSON) :

### Créer un ADMIN
```json
{
  "username": "administrator",
  "password": "admin2024",
  "roleName": "ADMIN"
}
```

### Créer un USER
```json
{
  "username": "regularuser",
  "password": "user2024",
  "roleName": "USER"
}
```

## Réponses attendues

### ✅ Succès (Status 200)
```json
{
  "id": 1,
  "username": "john_doe",
  "password": "$2a$10$...", // mot de passe encodé
  "role": {
    "id": 1,
    "name": "USER",
    "description": "Utilisateur standard avec permissions de base"
  }
}
```

### ❌ Erreur : Username déjà pris (Status 400)
```
Username is already taken
```

### ❌ Erreur : Rôle non trouvé (Status 400)
```
Role not found: USER. Please contact administrator.
```

## Utilisation avec l'API Users

### Créer un utilisateur via /api/users (nécessite authentification)
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "username": "newuser",
    "password": "password123",
    "roleName": "ADMIN"
  }'
```

### Modifier le rôle d'un utilisateur existant
```bash
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "username": "newuser",
    "password": "newpassword",
    "roleName": "USER"
  }'
```

## Notes importantes

1. **Enum automatique dans Swagger** : Swagger UI affichera automatiquement une liste déroulante avec seulement ADMIN et USER comme options.

2. **Validation automatique** : Si vous essayez d'envoyer une valeur invalide (ex: "MODERATOR", "admin", "User"), Spring retournera automatiquement une erreur 400.

3. **Valeur par défaut** : Si `roleName` est `null` ou non spécifié, le rôle USER sera attribué automatiquement.

4. **Case-sensitive** : Les valeurs doivent être en MAJUSCULES : "ADMIN" et "USER" (pas "admin" ou "user").
