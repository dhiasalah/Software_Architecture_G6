# 🔐 Guide des Rôles - Système d'Authentification

## 📋 Vue d'ensemble

Le système de rôles utilise maintenant un **Enum** `RoleType` pour garantir que seuls les rôles **ADMIN** et **USER** sont acceptés.

## 🎯 Valeurs possibles

L'enum `RoleType` définit deux rôles :

- **ADMIN** : Administrateur avec toutes les permissions
- **USER** : Utilisateur standard avec permissions de base

## 🔧 Modifications apportées

### 1. Création de l'Enum `RoleType`
```java
public enum RoleType {
    ADMIN("Administrateur avec toutes les permissions"),
    USER("Utilisateur standard avec permissions de base");
}
```

### 2. Modification de l'entité `Role`
- Le champ `name` est maintenant de type `RoleType` au lieu de `String`
- Utilise `@Enumerated(EnumType.STRING)` pour sauvegarder en base de données

### 3. Modification des DTOs
- **UserRequest** : `roleName` est maintenant de type `RoleType`
- **RegisterRequest** : Ajout du champ `roleName` de type `RoleType`
- **UserResponse** : Convertit l'enum en String avec `.name()`

## 📝 Utilisation de l'API

### Inscription avec rôle spécifié

#### Créer un utilisateur USER
```json
POST /api/auth/register
{
  "username": "john_doe",
  "password": "password123",
  "roleName": "USER"
}
```

#### Créer un utilisateur ADMIN
```json
POST /api/auth/register
{
  "username": "admin_user",
  "password": "securepassword",
  "roleName": "ADMIN"
}
```

#### Créer un utilisateur sans spécifier le rôle (USER par défaut)
```json
POST /api/auth/register
{
  "username": "default_user",
  "password": "password123"
}
```
> ⚠️ Si `roleName` n'est pas spécifié, le rôle **USER** sera attribué automatiquement.

### Créer un utilisateur via l'API UserController

```json
POST /api/users
{
  "username": "new_user",
  "password": "password123",
  "roleName": "ADMIN"
}
```

### Modifier le rôle d'un utilisateur

```json
PUT /api/users/{id}
{
  "username": "john_doe",
  "password": "newpassword",
  "roleName": "ADMIN"
}
```

## ✅ Avantages de l'Enum

### Avant (String)
```java
// ❌ Possibilité d'erreurs de typo
role.setName("ADMN");      // Erreur non détectée
role.setName("user");      // Minuscule non gérée
role.setName("MODERATOR"); // Rôle non voulu
```

### Après (Enum)
```java
// ✅ Seulement 2 valeurs possibles
role.setName(RoleType.ADMIN);  // Auto-complétion
role.setName(RoleType.USER);   // Validation à la compilation
// role.setName(RoleType.MODERATOR); // ❌ Erreur de compilation !
```

## 🔍 Validation

Le système garantit maintenant que :
1. ✅ Seuls **ADMIN** et **USER** sont acceptés
2. ✅ Les erreurs de typo sont impossibles (validation à la compilation)
3. ✅ L'auto-complétion fonctionne dans l'IDE
4. ✅ Si aucun rôle n'est spécifié lors de l'inscription, **USER** est attribué par défaut

## 🗃️ Base de données

Les rôles sont initialisés automatiquement au démarrage de l'application :
- **USER** : Créé automatiquement
- **ADMIN** : Créé automatiquement

La table `roles` contient :
- `id` : Identifiant unique
- `name` : "ADMIN" ou "USER" (stocké comme String en base)
- `description` : Description du rôle

## 🎯 Exemples de réponses

### Succès
```json
{
  "id": 1,
  "username": "john_doe",
  "roleName": "USER"
}
```

### Erreur - Nom d'utilisateur déjà pris
```json
"Username is already taken"
```

### Erreur - Rôle invalide (dans Swagger/JSON)
Si vous essayez d'envoyer un rôle inexistant via JSON, Spring rejettera la requête car l'enum ne reconnaît que ADMIN et USER.

## 📚 Fichiers modifiés

1. **Nouveau** : `entity/RoleType.java` - Enum des rôles
2. **Modifié** : `entity/Role.java` - Utilise RoleType
3. **Modifié** : `dto/RegisterRequest.java` - Ajout de roleName
4. **Modifié** : `dto/UserRequest.java` - Utilise RoleType
5. **Modifié** : `dto/UserResponse.java` - Convertit enum en String
6. **Modifié** : `repository/RoleRepository.java` - findByName(RoleType)
7. **Modifié** : `configuration/DataInitializer.java` - Utilise RoleType, retire MODERATOR
8. **Modifié** : `controller/AuthController.java` - Gère roleName avec USER par défaut
9. **Modifié** : `service/UserService.java` - Utilise RoleType

## 🚀 Prochaines étapes

Pour tester les changements :
1. Redémarrer l'application
2. Accéder à Swagger UI : `http://localhost:8080/swagger-ui.html`
3. Tester l'endpoint `/api/auth/register` avec différents rôles
