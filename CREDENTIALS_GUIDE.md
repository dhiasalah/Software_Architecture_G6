# 🔑 Guide Credentials - Séparation des identifiants sensibles

## 📋 Vue d'ensemble

La classe `Credentials` a été créée pour **séparer les identifiants sensibles** (email, téléphone, mot de passe) de l'entité `User`.

## 🎯 Structure

### User (Données de base)
- `id` : Identifiant unique
- `username` : Nom d'utilisateur
- `role` : Rôle (ADMIN ou USER)
- `credentials` : Lien vers les identifiants

### Credentials (Identifiants sensibles)
- `id` : Identifiant unique
- `email` : Email (unique, obligatoire)
- `phoneNumber` : Numéro de téléphone (unique, optionnel)
- `password` : Mot de passe encodé (obligatoire)
- `user` : Lien vers l'utilisateur

## 🔗 Relation One-to-One

```
User <---> Credentials
  1           1
```

- **Un utilisateur** a exactement **un ensemble d'identifiants**
- **Un ensemble d'identifiants** appartient à **un seul utilisateur**

### Configuration JPA

#### Dans User
```java
@OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
private Credentials credentials;
```

#### Dans Credentials
```java
@OneToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "user_id", nullable = false, unique = true)
private User user;
```

## ✅ Avantages

### 1. Séparation des responsabilités
- `User` : Informations de base (username, rôle)
- `Credentials` : Informations sensibles (email, téléphone, mot de passe)

### 2. Sécurité améliorée
- Isolation des données sensibles
- Facilite l'application de règles de sécurité spécifiques
- Possibilité d'encoder/décoder facilement

### 3. Flexibilité
- Facile d'ajouter d'autres champs sensibles
- Possibilité d'avoir plusieurs credentials par utilisateur (future évolution)
- Gestion indépendante des credentials

### 4. Maintenance
- Code plus organisé et maintenable
- Modifications isolées (changement de credentials sans toucher à User)

## 📝 Utilisation de l'API

### Inscription avec credentials

```json
POST /api/auth/register
{
  "username": "john_doe",
  "email": "john@example.com",
  "phoneNumber": "+33612345678",
  "password": "securePassword123",
  "roleName": "USER"
}
```

**Réponse :**
```json
{
  "id": 1,
  "username": "john_doe",
  "role": {
    "id": 1,
    "name": "USER",
    "description": "Utilisateur standard avec permissions de base"
  },
  "credentials": {
    "id": 1,
    "email": "john@example.com",
    "phoneNumber": "+33612345678",
    "password": "$2a$10$...", // Encodé
    "user": {
      "id": 1,
      "username": "john_doe"
    }
  }
}
```

### Créer un utilisateur via API

```json
POST /api/users
Headers: Authorization: Bearer YOUR_JWT_TOKEN
{
  "username": "jane_smith",
  "email": "jane@example.com",
  "phoneNumber": "+33698765432",
  "password": "password123",
  "roleName": "ADMIN"
}
```

### Récupérer un utilisateur

```json
GET /api/users/1
Headers: Authorization: Bearer YOUR_JWT_TOKEN

Response:
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "phoneNumber": "+33612345678",
  "roleName": "USER"
}
```

**Note :** Le mot de passe n'est **jamais exposé** dans les réponses API.

### Modifier un utilisateur

```json
PUT /api/users/1
Headers: Authorization: Bearer YOUR_JWT_TOKEN
{
  "username": "john_doe_updated",
  "email": "newemail@example.com",
  "phoneNumber": "+33687654321",
  "password": "newPassword123",
  "roleName": "ADMIN"
}
```

**Note :** Si vous ne voulez pas changer le mot de passe, vous pouvez omettre le champ `password`.

## 🔍 Validations

Le système vérifie automatiquement :

### 1. Unicité du username
```
"Le nom d'utilisateur 'john_doe' est déjà utilisé"
```

### 2. Unicité de l'email
```
"L'email 'john@example.com' est déjà utilisé"
```

### 3. Unicité du numéro de téléphone
```
"Le numéro de téléphone '+33612345678' est déjà utilisé"
```

### 4. Présence des credentials
```
"User credentials not found for: username"
```

## 🗃️ Structure de la base de données

### Table `users`
| Colonne      | Type   | Contraintes           |
|--------------|--------|-----------------------|
| id           | BIGINT | PRIMARY KEY, AUTO_INCREMENT |
| username     | VARCHAR| UNIQUE, NOT NULL      |
| role_id      | BIGINT | FOREIGN KEY, NOT NULL |

### Table `credentials`
| Colonne      | Type   | Contraintes           |
|--------------|--------|-----------------------|
| id           | BIGINT | PRIMARY KEY, AUTO_INCREMENT |
| email        | VARCHAR| UNIQUE, NOT NULL      |
| phone_number | VARCHAR| UNIQUE, NULL          |
| password     | VARCHAR| NOT NULL              |
| user_id      | BIGINT | UNIQUE, FOREIGN KEY, NOT NULL |

### Relations
```sql
-- Dans credentials
FOREIGN KEY (user_id) REFERENCES users(id)

-- Dans users
FOREIGN KEY (role_id) REFERENCES roles(id)
```

## 🔄 Cascade Operations

Grâce à `cascade = CascadeType.ALL` :

### Sauvegarde
```java
User user = new User();
Credentials credentials = new Credentials();
credentials.setUser(user);
user.setCredentials(credentials);

userRepository.save(user); // Sauvegarde aussi les credentials
```

### Suppression
```java
userRepository.delete(user); // Supprime aussi les credentials
```

### Mise à jour
```java
user.getCredentials().setEmail("new@email.com");
userRepository.save(user); // Met à jour aussi les credentials
```

## 🔐 Authentification

Lors du login, Spring Security :

1. Récupère l'utilisateur par `username`
2. Charge les credentials avec `FetchType.EAGER`
3. Récupère le mot de passe depuis `user.getCredentials().getPassword()`
4. Compare avec le mot de passe fourni (après encodage)
5. Crée un token JWT avec le rôle

## 📚 Fichiers modifiés/créés

### Nouveaux fichiers
1. **entity/Credentials.java** - Entité pour les identifiants sensibles
2. **repository/CredentialsRepository.java** - Repository pour Credentials

### Fichiers modifiés
1. **entity/User.java** - Ajout relation OneToOne, suppression du champ password
2. **dto/RegisterRequest.java** - Ajout email et phoneNumber
3. **dto/UserRequest.java** - Ajout email et phoneNumber
4. **dto/UserResponse.java** - Ajout email et phoneNumber
5. **controller/AuthController.java** - Gestion des credentials lors de l'inscription
6. **service/UserService.java** - Création et mise à jour des credentials
7. **service/CustomUserDetailsService.java** - Récupération du password depuis Credentials

## 🚀 Migration depuis l'ancienne structure

Si vous aviez déjà des utilisateurs avec `password` dans `User` :

### Option 1 : Migration SQL
```sql
-- Créer la table credentials
CREATE TABLE credentials (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(50) UNIQUE,
    password VARCHAR(255) NOT NULL,
    user_id BIGINT UNIQUE NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Migrer les données
INSERT INTO credentials (email, password, user_id)
SELECT CONCAT(username, '@example.com'), password, id
FROM users;

-- Supprimer l'ancienne colonne password de users
ALTER TABLE users DROP COLUMN password;
```

### Option 2 : Nouvelle base de données
- Supprimer la base de données existante
- Laisser JPA recréer les tables avec la nouvelle structure

## 💡 Bonnes pratiques

1. **Ne jamais exposer le mot de passe** dans les réponses API
2. **Toujours encoder** le mot de passe avant de le sauvegarder
3. **Valider l'email** au format correct (ex: avec `@Email` annotation)
4. **Valider le téléphone** au format international (ex: avec regex)
5. **Utiliser HTTPS** en production pour protéger les credentials

## 🎯 Prochaines étapes possibles

1. Ajouter validation des emails (`@Email`)
2. Ajouter validation du téléphone (regex)
3. Implémenter la réinitialisation de mot de passe
4. Ajouter 2FA (Two-Factor Authentication) avec le téléphone
5. Historique des changements de credentials
6. Expiration des mots de passe
