# 🎨 Architecture Visuelle du Projet

## 📊 Diagramme de Classes

```
┌─────────────────────────────┐
│         Role                │
├─────────────────────────────┤
│ - id: Long                  │
│ - name: String              │
│ - description: String       │
└─────────────────────────────┘
            ▲
            │
            │ @ManyToOne
            │ (Plusieurs Users → Un Role)
            │
┌─────────────────────────────┐
│         User                │
├─────────────────────────────┤
│ - id: Long                  │
│ - username: String          │
│ - password: String          │
│ - role: Role                │ ◄── Relation vers Role
└─────────────────────────────┘
```

---

## 🔄 Flux de Données (Inscription)

```
1. CLIENT (Frontend)
   │
   │ POST /api/auth/register
   │ { "username": "john", "password": "pass" }
   │
   ▼
2. AuthController
   │ Reçoit RegisterRequest
   │
   ├──► Vérifie si username existe déjà (UserRepository)
   │
   ├──► Encode le mot de passe (PasswordEncoder)
   │
   ├──► Cherche le rôle "USER" (RoleRepository)
   │
   ├──► Crée un nouvel User avec le Role
   │
   └──► Sauvegarde dans la base (UserRepository)
         │
         ▼
3. BASE DE DONNÉES
   ├── Table users : Nouvel utilisateur créé
   └── Table roles : Rôle "USER" associé
```

---

## 🏗️ Architecture en Couches

```
┌────────────────────────────────────────┐
│           COUCHE CLIENT                │
│     (Postman, Frontend, etc.)          │
└────────────────────────────────────────┘
                 │
                 │ HTTP Requests
                 ▼
┌────────────────────────────────────────┐
│      COUCHE CONTROLLER                 │
│  ┌──────────────────────────────┐      │
│  │   AuthController             │      │
│  │   - register()               │      │
│  │   - login()                  │      │
│  └──────────────────────────────┘      │
└────────────────────────────────────────┘
                 │
                 │ Utilise DTOs
                 ▼
┌────────────────────────────────────────┐
│         COUCHE DTO                     │
│  ┌────────────┐  ┌─────────────┐      │
│  │ Register   │  │   Login     │      │
│  │ Request    │  │   Request   │      │
│  └────────────┘  └─────────────┘      │
└────────────────────────────────────────┘
                 │
                 │ Transformé en Entities
                 ▼
┌────────────────────────────────────────┐
│       COUCHE ENTITY                    │
│  ┌────────────┐  ┌─────────────┐      │
│  │   User     │  │    Role     │      │
│  └────────────┘  └─────────────┘      │
└────────────────────────────────────────┘
                 │
                 │ Persisté via Repositories
                 ▼
┌────────────────────────────────────────┐
│    COUCHE REPOSITORY (DAO)             │
│  ┌────────────┐  ┌─────────────┐      │
│  │   User     │  │    Role     │      │
│  │ Repository │  │ Repository  │      │
│  └────────────┘  └─────────────┘      │
└────────────────────────────────────────┘
                 │
                 │ SQL Queries
                 ▼
┌────────────────────────────────────────┐
│      BASE DE DONNÉES                   │
│    ┌─────────┐  ┌──────────┐          │
│    │  users  │  │  roles   │          │
│    └─────────┘  └──────────┘          │
└────────────────────────────────────────┘
```

---

## 🔐 Flux d'Authentification (Login)

```
CLIENT
  │
  │ 1. Envoie username + password
  │
  ▼
AuthController.login()
  │
  │ 2. Crée UsernamePasswordAuthenticationToken
  │
  ▼
AuthenticationManager
  │
  │ 3. Demande les détails de l'utilisateur
  │
  ▼
CustomUserDetailsService.loadUserByUsername()
  │
  │ 4. Cherche l'utilisateur dans la DB
  │
  ▼
UserRepository.findByUsername()
  │
  │ 5. Retourne User avec Role chargé (EAGER)
  │
  ▼
CustomUserDetailsService
  │
  │ 6. Extrait user.getRole().getName()
  │    → Convertit en UserDetails Spring Security
  │
  ▼
AuthenticationManager
  │
  │ 7. Vérifie le mot de passe
  │    (Compare avec BCrypt)
  │
  ▼
AuthController.login()
  │
  │ 8. Génère JWT Token
  │
  ▼
CLIENT
  │
  │ 9. Reçoit { "token": "eyJ...", "type": "Bearer" }
  │
  └─► Utilise ce token pour les requêtes suivantes
```

---

## 📦 Séparation des Responsabilités

### Avant la Séparation ❌
```
┌──────────────────────────┐
│         User             │
│ - Données utilisateur    │
│ - Gestion des rôles      │
│ - Logique métier         │
│ - Validation             │
└──────────────────────────┘
    ↑
    Trop de responsabilités !
```

### Après la Séparation ✅
```
┌──────────────────┐     ┌──────────────────┐
│      User        │────►│      Role        │
│ - id             │     │ - id             │
│ - username       │     │ - name           │
│ - password       │     │ - description    │
│ - role (FK)      │     └──────────────────┘
└──────────────────┘           │
        │                      │
        │                      │
        ▼                      ▼
┌──────────────────┐     ┌──────────────────┐
│ RegisterRequest  │     │ RoleRepository   │
│ - username       │     │ - findByName()   │
│ - password       │     │ - save()         │
└──────────────────┘     └──────────────────┘
```

**Avantages** :
- ✅ Chaque classe a une seule responsabilité
- ✅ Facile à tester
- ✅ Facile à modifier
- ✅ Code plus lisible

---

## 🗃️ Relations en Base de Données

### Relation ManyToOne expliquée

```
Table: roles
+----+-----------+
| id | name      |
+----+-----------+
| 1  | USER      |  ◄──┐
| 2  | ADMIN     |  ◄──┼──┐
| 3  | MODERATOR |     │  │
+----+-----------+     │  │
                       │  │
Table: users           │  │
+----+----------+---------+
| id | username | role_id |
+----+----------+---------+
| 1  | john     | 1       │──┘  (john est USER)
| 2  | admin    | 2       │─────┘  (admin est ADMIN)
| 3  | mary     | 1       │        (mary est USER)
| 4  | bob      | 1       │        (bob est USER)
+----+----------+---------+

→ Plusieurs utilisateurs peuvent avoir le MÊME rôle
→ C'est la relation ManyToOne
```

---

## 🎯 Évolution Future

### Étape 1 : ManyToMany (Plusieurs rôles par utilisateur)

```
Table: users              Table: user_roles           Table: roles
+----+----------+         +─────────+──────────+      +────+───────+
| id | username |         | user_id | role_id  |      | id | name  |
+----+----------+         +─────────+──────────+      +────+───────+
| 1  | john     |◄────────| 1       | 1        |──────►| 1  | USER  |
+----+----------+         | 1       | 3        |──┐   +────+───────+
                          +─────────+──────────+  │   | 2  | ADMIN |
                                                   │   +────+───────+
                                                   └───►| 3  | MOD   |
                                                       +────+───────+
→ john peut être USER ET MODERATOR en même temps
```

### Étape 2 : Ajouter des Permissions

```
Role ──► Permission
  │         │
  │         ├── READ_USERS
  │         ├── CREATE_POSTS
  │         ├── DELETE_POSTS
  │         └── MANAGE_ROLES
  │
  ├── USER (READ_USERS, CREATE_POSTS)
  ├── MODERATOR (+ DELETE_POSTS)
  └── ADMIN (+ MANAGE_ROLES)
```

---

## 📖 Vocabulaire Technique

| Terme | Signification | Exemple |
|-------|---------------|---------|
| **Entity** | Classe qui représente une table en base | `User`, `Role` |
| **DTO** | Objet pour transférer des données | `RegisterRequest`, `LoginRequest` |
| **Repository** | Interface pour accéder à la base | `UserRepository`, `RoleRepository` |
| **Controller** | Gère les requêtes HTTP | `AuthController` |
| **Service** | Contient la logique métier | `CustomUserDetailsService` |
| **@ManyToOne** | Relation plusieurs vers un | Plusieurs Users → Un Role |
| **@JoinColumn** | Colonne de jointure (FK) | `role_id` dans `users` |
| **FetchType.EAGER** | Charge immédiatement la relation | `Role` chargé avec `User` |
| **FetchType.LAZY** | Charge à la demande | Relation chargée plus tard |

---

**🎓 Ce diagramme vous aide à visualiser l'architecture complète de votre application !**
