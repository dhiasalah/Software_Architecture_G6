# 🎓 Guide de Séparation des Classes - Pour Débutants

## 📋 Résumé des Changements

J'ai appliqué le principe de **Séparation des Responsabilités (SRP)** à votre projet.

---

## ✅ Ce qui a été modifié

### 1. **Nouvelle Classe `Role`** (entity/Role.java)
**Avant** : Le rôle était juste un `String` dans la classe `User`
```java
private String role; // ❌ Simple texte
```

**Après** : Le rôle est maintenant une entité séparée
```java
@Entity
public class Role {
    private Long id;
    private String name;        // "ADMIN", "USER", "MODERATOR"
    private String description; // Description du rôle
}
```

**Pourquoi ?**
- ✅ **Meilleure organisation** : Les informations sur les rôles sont centralisées
- ✅ **Évolutivité** : Facile d'ajouter des champs (permissions, priorité, etc.)
- ✅ **Réutilisabilité** : Plusieurs utilisateurs partagent le même rôle
- ✅ **Intégrité** : Impossible d'avoir des fautes de frappe ("ADMIM" au lieu de "ADMIN")

---

### 2. **Classe `User` Modifiée** (entity/User.java)

**Avant** :
```java
private String role; // Simple texte
```

**Après** :
```java
@ManyToOne(fetch = FetchType.EAGER)
@JoinColumn(name = "role_id", nullable = false)
private Role role; // ✅ Relation vers la table Role
```

**Explication de la relation `@ManyToOne`** :
- **Many** (plusieurs) utilisateurs → **One** (un) rôle
- Exemple : 1000 utilisateurs peuvent avoir le rôle "USER"
- En base de données, `User` aura une colonne `role_id` qui pointe vers la table `roles`

---

### 3. **Nouveau Repository `RoleRepository`**

```java
public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByName(String name);
}
```

**Rôle** : Permet de chercher et sauvegarder des rôles dans la base de données

---

### 4. **Classes DTO créées** (dto/LoginRequest.java & RegisterRequest.java)

**Avant** : On utilisait directement `User` dans le controller
```java
public ResponseEntity<?> register(@RequestBody User user)
```

**Après** : On utilise des DTOs (Data Transfer Objects)
```java
public ResponseEntity<?> register(@RequestBody RegisterRequest request)
```

**Pourquoi des DTOs ?**
- ✅ **Sécurité** : L'utilisateur ne peut pas envoyer un `id` ou un `role` personnalisé
- ✅ **Clarté** : On sait exactement quels champs sont nécessaires
- ✅ **Flexibilité** : `RegisterRequest` peut avoir des champs différents de `LoginRequest`

---

### 5. **DataInitializer** - Création automatique des rôles

```java
@Component
public class DataInitializer implements CommandLineRunner {
    // Crée automatiquement les rôles au démarrage
}
```

**Rôle** : Au démarrage de l'application, crée 3 rôles par défaut :
- `USER` - Utilisateur standard
- `ADMIN` - Administrateur
- `MODERATOR` - Modérateur

---

## 🗂️ Structure de la Base de Données

### Avant :
```
Table: users
+----+----------+----------+------+
| id | username | password | role |
+----+----------+----------+------+
| 1  | john     | xxxxxx   | USER |  ← Texte simple
| 2  | admin    | xxxxxx   | ADMIN|
+----+----------+----------+------+
```

### Après :
```
Table: roles
+----+----------+------------------+
| id | name     | description      |
+----+----------+------------------+
| 1  | USER     | Utilisateur...   |
| 2  | ADMIN    | Administrateur...|
| 3  | MODERATOR| Modérateur...    |
+----+----------+------------------+

Table: users
+----+----------+----------+---------+
| id | username | password | role_id |
+----+----------+----------+---------+
| 1  | john     | xxxxxx   | 1       |  ← Référence vers roles.id
| 2  | admin    | xxxxxx   | 2       |
+----+----------+----------+---------+
```

---

## 📚 Concepts Expliqués

### 1. **Séparation des Responsabilités (SRP)**
Chaque classe a UNE seule raison de changer :
- `User` : Gère les données de l'utilisateur
- `Role` : Gère les rôles et permissions
- `RegisterRequest` : Gère les données d'inscription
- `LoginRequest` : Gère les données de connexion

### 2. **Relations JPA**
- `@ManyToOne` : Plusieurs entités vers une seule
- `@JoinColumn` : Spécifie la colonne de jointure (foreign key)
- `FetchType.EAGER` : Charge la relation automatiquement

### 3. **DTOs (Data Transfer Objects)**
Des classes simples pour transférer des données entre :
- Le client (frontend) ↔ Le serveur (backend)
- Différentes couches de l'application

---

## 🚀 Comment Tester ?

### 1. Démarrer l'application
```bash
./mvnw spring-boot:run
```

Au démarrage, vous verrez :
```
✅ Rôle USER créé avec succès
✅ Rôle ADMIN créé avec succès
✅ Rôle MODERATOR créé avec succès
```

### 2. S'inscrire (Register)
```bash
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "username": "john",
  "password": "password123"
}
```

→ L'utilisateur sera créé avec le rôle "USER" par défaut

### 3. Se connecter (Login)
```bash
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "john",
  "password": "password123"
}
```

---

## 🎯 Prochaines Étapes (Pour aller plus loin)

### Étape 1 : Plusieurs rôles par utilisateur
Actuellement : Un utilisateur = Un rôle

Évolution possible : Un utilisateur peut avoir plusieurs rôles
```java
@ManyToMany
private Set<Role> roles; // USER peut être aussi MODERATOR
```

### Étape 2 : Ajouter des permissions
```java
@Entity
public class Permission {
    private String name; // "READ_USERS", "DELETE_POSTS"
}

@Entity
public class Role {
    @ManyToMany
    private Set<Permission> permissions;
}
```

### Étape 3 : Créer un service UserService
Au lieu d'avoir la logique dans le controller, la déplacer dans un service :
```java
@Service
public class UserService {
    public User registerUser(RegisterRequest request) {
        // Logique d'inscription ici
    }
}
```

---

## ❓ Questions Fréquentes

**Q : Pourquoi créer une classe Role au lieu de garder un String ?**
R : Pour éviter les erreurs de frappe, centraliser les informations, et faciliter l'évolution future.

**Q : Qu'est-ce qu'un DTO ?**
R : Un objet simple pour transférer des données. Il protège votre entité et rend l'API plus claire.

**Q : Que fait @ManyToOne ?**
R : Il crée une relation "plusieurs vers un" en base de données. Plusieurs utilisateurs peuvent avoir le même rôle.

**Q : Pourquoi DataInitializer ?**
R : Pour créer les rôles de base automatiquement. Sans cela, l'inscription échouerait car le rôle "USER" n'existerait pas.

---

## 📁 Fichiers Modifiés/Créés

### ✨ Nouveaux fichiers :
- `entity/Role.java` - Entité Role
- `repository/RoleRepository.java` - Repository pour Role
- `dto/LoginRequest.java` - DTO pour la connexion
- `dto/RegisterRequest.java` - DTO pour l'inscription
- `configuration/DataInitializer.java` - Initialisation des rôles

### ✏️ Fichiers modifiés :
- `entity/User.java` - Relation avec Role
- `controller/AuthController.java` - Utilisation des DTOs et RoleRepository
- `service/CustomUserDetailsService.java` - Accès au nom du rôle via `user.getRole().getName()`

---

**🎉 Félicitations ! Vous avez appliqué votre première séparation de classes selon les principes SOLID !**
