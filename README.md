# 📚 Documentation du Projet Spring Boot - Système d'Authentification

## 📋 Table des Matières
1. [Vue d'ensemble du projet](#vue-densemble-du-projet)
2. [Technologies utilisées](#technologies-utilisées)
3. [Architecture du projet](#architecture-du-projet)
4. [Structure des fichiers](#structure-des-fichiers)
5. [Les Entités et leurs Relations](#les-entités-et-leurs-relations)
6. [Couche Configuration](#couche-configuration)
7. [Couche Controller](#couche-controller)
8. [Couche Service](#couche-service)
9. [Couche Repository](#couche-repository)
10. [Flux d'authentification](#flux-dauthentification)
11. [API Endpoints](#api-endpoints)
12. [Comment utiliser l'application](#comment-utiliser-lapplication)

---

## 🎯 Vue d'ensemble du projet

Ce projet est une **API REST Spring Boot** qui implémente un système d'**authentification et de gestion des utilisateurs** avec les fonctionnalités suivantes :

- ✅ **Inscription d'utilisateurs** (Register)
- ✅ **Connexion** (Login) avec génération de **token JWT**
- ✅ **Gestion CRUD des utilisateurs** (Create, Read, Update, Delete)
- ✅ **Sécurité basée sur les rôles** (ADMIN, USER)
- ✅ **Documentation API automatique** avec Swagger/OpenAPI

### 🔑 Concept clé : JWT (JSON Web Token)

Le projet utilise **JWT** pour l'authentification :
1. L'utilisateur s'inscrit ou se connecte
2. Le serveur génère un **token JWT**
3. L'utilisateur envoie ce token dans chaque requête
4. Le serveur vérifie le token et autorise ou refuse l'accès

---

## 🛠️ Technologies utilisées

| Technologie | Version | Description |
|------------|---------|-------------|
| **Java** | 17 | Langage de programmation |
| **Spring Boot** | 4.0.2 | Framework backend |
| **Spring Security** | - | Sécurité et authentification |
| **Spring Data JPA** | - | ORM pour la base de données |
| **PostgreSQL** | - | Base de données relationnelle |
| **JWT (jjwt)** | 0.11.5 | Gestion des tokens JWT |
| **Lombok** | - | Réduction du code boilerplate |
| **SpringDoc OpenAPI** | 2.3.0 | Documentation Swagger |
| **Maven** | - | Gestionnaire de dépendances |

---

## 🏗️ Architecture du projet

Le projet suit une **architecture en couches** (Layered Architecture) :

```
┌─────────────────────────────────────────────────────────────┐
│                    🌐 CLIENT (Navigateur/Postman)           │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   🔒 FILTRE JWT (JwtFilter)                 │
│        Intercepte les requêtes et vérifie le token          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                 🎮 CONTROLLERS (REST API)                   │
│   AuthController │ UserController │ HomeController          │
│   Reçoit les requêtes HTTP et retourne les réponses         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    🔧 SERVICES                              │
│         UserService │ CustomUserDetailsService              │
│         Contient la logique métier                          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                  📦 REPOSITORIES                            │
│   UserRepository │ RoleRepository │ CredentialsRepository   │
│   Communication avec la base de données                     │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                  🗄️ BASE DE DONNÉES (PostgreSQL)           │
│            Tables: users, roles, credentials                │
└─────────────────────────────────────────────────────────────┘
```

---

## 📁 Structure des fichiers

```
src/main/java/com/example/project/
├── 📄 ProjectApplication.java          # Point d'entrée de l'application
│
├── 📁 configuration/                   # Configuration de l'application
│   ├── SecurityConfig.java             # Configuration Spring Security
│   ├── JwtUtils.java                   # Utilitaires pour JWT
│   ├── OpenApiConfig.java              # Configuration Swagger
│   └── DataInitializer.java            # Initialisation des données
│
├── 📁 controller/                      # Contrôleurs REST
│   ├── AuthController.java             # Endpoints d'authentification
│   ├── UserController.java             # CRUD utilisateurs
│   └── HomeController.java             # Page d'accueil
│
├── 📁 dto/                             # Data Transfer Objects
│   ├── LoginRequest.java               # Requête de connexion
│   └── RegisterRequest.java            # Requête d'inscription
│
├── 📁 entity/                          # Entités JPA (tables BD)
│   ├── User.java                       # Entité utilisateur
│   ├── Role.java                       # Entité rôle
│   ├── RoleType.java                   # Enum des types de rôles
│   └── Credentials.java                # Entité identifiants
│
├── 📁 filter/                          # Filtres HTTP
│   └── JwtFilter.java                  # Filtre de vérification JWT
│
├── 📁 repository/                      # Accès à la base de données
│   ├── UserRepository.java             # Repository utilisateurs
│   ├── RoleRepository.java             # Repository rôles
│   └── CredentialsRepository.java      # Repository identifiants
│
└── 📁 service/                         # Logique métier
    ├── UserService.java                # Service utilisateurs
    └── CustomUserDetailsService.java   # Service Spring Security
```

---

## 🔗 Les Entités et leurs Relations

### 📊 Diagramme des Relations (ERD)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           DIAGRAMME DE RELATIONS                            │
└─────────────────────────────────────────────────────────────────────────────┘

   ┌──────────────────┐         ┌──────────────────┐         ┌──────────────────┐
   │      ROLE        │         │       USER       │         │   CREDENTIALS    │
   ├──────────────────┤         ├──────────────────┤         ├──────────────────┤
   │ id (PK)          │◄───┐    │ id (PK)          │◄───┐    │ id (PK)          │
   │ name (RoleType)  │    │    │ username         │    │    │ email            │
   │ description      │    │    │ role_id (FK)─────┤────┘    │ phoneNumber      │
   └──────────────────┘    │    │                  │         │ password         │
                           │    └──────────────────┘         │ user_id (FK)─────┤────┐
                           │            ▲                    └──────────────────┘    │
                           │            │                             │              │
                           │            └─────────────────────────────┘              │
                           │                    1:1 (One-to-One)                     │
                           │                                                         │
                           └─────────────────────────────────────────────────────────┘
                                           N:1 (Many-to-One)


   ┌──────────────────────────────────────────────────────────────────────────────┐
   │                              ENUM RoleType                                   │
   ├──────────────────────────────────────────────────────────────────────────────┤
   │  ADMIN ─── "Administrateur avec toutes les permissions"                      │
   │  USER  ─── "Utilisateur standard avec permissions de base"                   │
   └──────────────────────────────────────────────────────────────────────────────┘
```

### 📝 Explication détaillée des Relations

#### 1️⃣ Relation **User ↔ Role** (Many-to-One / N:1)

```java
// Dans User.java
@ManyToOne(fetch = FetchType.EAGER)
@JoinColumn(name = "role_id", nullable = false)
private Role role;
```

| Aspect | Description |
|--------|-------------|
| **Type** | Many-to-One (Plusieurs utilisateurs vers un rôle) |
| **Signification** | Plusieurs utilisateurs peuvent avoir le MÊME rôle |
| **Exemple** | 100 utilisateurs peuvent avoir le rôle "USER" |
| **Colonne** | `role_id` dans la table `users` |
| **FetchType.EAGER** | Le rôle est chargé automatiquement avec l'utilisateur |

#### 2️⃣ Relation **User ↔ Credentials** (One-to-One / 1:1)

```java
// Dans User.java
@OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
private Credentials credentials;

// Dans Credentials.java
@OneToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "user_id", nullable = false, unique = true)
private User user;
```

| Aspect | Description |
|--------|-------------|
| **Type** | One-to-One (Un utilisateur, un ensemble d'identifiants) |
| **Propriétaire** | `Credentials` possède la clé étrangère (`user_id`) |
| **mappedBy** | La relation est gérée par le champ `user` dans Credentials |
| **cascade = CascadeType.ALL** | Les opérations sur User se propagent à Credentials |
| **orphanRemoval = true** | Si on détache les credentials, ils sont supprimés |

#### 3️⃣ L'Enum **RoleType**

```java
public enum RoleType {
    ADMIN("Administrateur avec toutes les permissions"),
    USER("Utilisateur standard avec permissions de base");
}
```

| Aspect | Description |
|--------|-------------|
| **Pourquoi un Enum ?** | Garantit que seules les valeurs ADMIN et USER sont possibles |
| **Avantages** | Évite les erreurs de typo, auto-complétion, type-safe |
| **Stockage** | Sauvegardé en tant que String en base ("ADMIN", "USER") |

---

### 📊 Tables en Base de Données

#### Table `users`
| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Identifiant unique |
| username | VARCHAR | UNIQUE, NOT NULL | Nom d'utilisateur |
| role_id | BIGINT | FOREIGN KEY → roles(id), NOT NULL | Référence au rôle |

#### Table `roles`
| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Identifiant unique |
| name | VARCHAR | UNIQUE, NOT NULL | Type de rôle (ADMIN/USER) |
| description | VARCHAR(500) | - | Description du rôle |

#### Table `credentials`
| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Identifiant unique |
| email | VARCHAR | UNIQUE, NOT NULL | Email de l'utilisateur |
| phone_number | VARCHAR | UNIQUE | Numéro de téléphone (optionnel) |
| password | VARCHAR | NOT NULL | Mot de passe encodé (BCrypt) |
| user_id | BIGINT | FOREIGN KEY → users(id), UNIQUE, NOT NULL | Référence à l'utilisateur |

---

## ⚙️ Couche Configuration

### 🔐 SecurityConfig.java

**Rôle** : Configure la sécurité de l'application avec Spring Security.

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    // ...
}
```

| Annotation | Description |
|------------|-------------|
| `@Configuration` | Indique une classe de configuration Spring |
| `@EnableWebSecurity` | Active Spring Security |
| `@EnableMethodSecurity` | Active la sécurité au niveau des méthodes (@PreAuthorize) |

**Fonctionnalités principales** :
- **PasswordEncoder** : Encode les mots de passe avec BCrypt
- **AuthenticationManager** : Gère l'authentification
- **SecurityFilterChain** : Définit les règles de sécurité
  - CSRF désactivé (API REST stateless)
  - Sessions stateless (pas de session côté serveur)
  - JwtFilter ajouté avant le filtre standard

### 🔑 JwtUtils.java

**Rôle** : Gère la création et validation des tokens JWT.

**Méthodes principales** :
| Méthode | Description |
|---------|-------------|
| `generateToken(username)` | Crée un nouveau token JWT |
| `extractUsername(token)` | Extrait le nom d'utilisateur du token |
| `validateToken(token, userDetails)` | Vérifie si le token est valide |
| `isTokenExpired(token)` | Vérifie si le token a expiré |

### 🚀 DataInitializer.java

**Rôle** : Initialise les données au démarrage de l'application.

Au démarrage, cette classe :
1. Vérifie si le rôle USER existe, sinon le crée
2. Vérifie si le rôle ADMIN existe, sinon le crée

### 📖 OpenApiConfig.java

**Rôle** : Configure Swagger/OpenAPI pour la documentation de l'API.

Configure :
- Titre et description de l'API
- Schéma d'authentification Bearer JWT
- Interface accessible sur `/swagger-ui.html`

---

## 🎮 Couche Controller

### AuthController.java

**Base URL** : `/api/auth`

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/register` | POST | Inscription d'un nouvel utilisateur |
| `/login` | POST | Connexion et obtention du token JWT |

**Flux d'inscription** :
1. Vérifie que username, email et password sont fournis
2. Vérifie que le username n'est pas déjà pris
3. Vérifie que l'email n'est pas déjà utilisé
4. Vérifie que le téléphone n'est pas déjà utilisé (si fourni)
5. Récupère le rôle (USER par défaut)
6. Crée l'utilisateur avec ses credentials
7. Encode le mot de passe avec BCrypt
8. Sauvegarde en base de données

**Flux de connexion** :
1. Vérifie les identifiants
2. Authentifie via AuthenticationManager
3. Génère un token JWT
4. Retourne le token au client

### UserController.java

**Base URL** : `/api/users`

| Endpoint | Méthode | Accès | Description |
|----------|---------|-------|-------------|
| `/` | GET | ADMIN | Liste tous les utilisateurs |
| `/{id}` | GET | ADMIN | Récupère un utilisateur par ID |
| `/` | POST | ADMIN | Crée un nouvel utilisateur |
| `/{id}` | PUT | ADMIN | Modifie un utilisateur |
| `/{id}` | DELETE | ADMIN | Supprime un utilisateur |

### HomeController.java

**Base URL** : `/`

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/` | GET | Page d'accueil avec informations sur l'API |

---

## 🔧 Couche Service

### UserService.java

**Rôle** : Contient la logique métier pour la gestion des utilisateurs.

| Méthode | Description |
|---------|-------------|
| `getAllUsers()` | Récupère tous les utilisateurs |
| `getUserById(id)` | Récupère un utilisateur par ID |
| `createUser(user)` | Crée un nouvel utilisateur |
| `updateUser(id, user)` | Met à jour un utilisateur |
| `deleteUser(id)` | Supprime un utilisateur |

### CustomUserDetailsService.java

**Rôle** : Implémente `UserDetailsService` de Spring Security.

Cette classe :
1. Charge un utilisateur par son username
2. Récupère ses credentials (mot de passe)
3. Crée les autorités (rôles) sous forme `ROLE_ADMIN` ou `ROLE_USER`
4. Retourne un objet UserDetails utilisé par Spring Security

---

## 📦 Couche Repository

Les repositories héritent de `JpaRepository` et fournissent des méthodes CRUD automatiques.

### UserRepository
```java
User findByUsername(String username);
```

### RoleRepository
```java
Role findByName(RoleType name);
```

### CredentialsRepository
```java
Credentials findByEmail(String email);
Credentials findByPhoneNumber(String phoneNumber);
```

---

## 🔄 Flux d'authentification

### 1. Inscription

```
┌─────────┐    POST /api/auth/register    ┌───────────────┐
│ CLIENT  │ ─────────────────────────────►│ AuthController│
└─────────┘   {username, email, password} └───────┬───────┘
                                                  │
                                                  ▼
                                          ┌───────────────┐
                                          │  Validations  │
                                          └───────┬───────┘
                                                  │
                                                  ▼
                                          ┌───────────────┐
                                          │PasswordEncoder│ (BCrypt)
                                          └───────┬───────┘
                                                  │
                                                  ▼
                                          ┌───────────────┐
                                          │ UserRepository│
                                          │    .save()    │
                                          └───────┬───────┘
                                                  │
                                                  ▼
┌─────────┐        User créé              ┌───────────────┐
│ CLIENT  │ ◄─────────────────────────────│   Response    │
└─────────┘                               └───────────────┘
```

### 2. Connexion

```
┌─────────┐     POST /api/auth/login      ┌───────────────┐
│ CLIENT  │ ─────────────────────────────►│ AuthController│
└─────────┘   {username, password}        └───────┬───────┘
                                                  │
                                                  ▼
                                          ┌───────────────────────┐
                                          │ AuthenticationManager │
                                          └───────────┬───────────┘
                                                      │
                                                      ▼
                                          ┌───────────────────────┐
                                          │CustomUserDetailsService│
                                          │   loadUserByUsername() │
                                          └───────────┬───────────┘
                                                      │
                                                      ▼
                                          ┌───────────────┐
                                          │   JwtUtils    │
                                          │generateToken()│
                                          └───────┬───────┘
                                                  │
                                                  ▼
┌─────────┐      {token, type: "Bearer"}  ┌───────────────┐
│ CLIENT  │ ◄─────────────────────────────│   Response    │
└─────────┘                               └───────────────┘
```

### 3. Requête authentifiée

```
┌─────────┐   GET /api/users              ┌───────────────┐
│ CLIENT  │ ─────────────────────────────►│   JwtFilter   │
└─────────┘  Header: Authorization:       └───────┬───────┘
             Bearer <token>                       │
                                                  ▼
                                          ┌───────────────┐
                                          │   JwtUtils    │
                                          │extractUsername│
                                          │validateToken()│
                                          └───────┬───────┘
                                                  │
                                                  ▼
                                          ┌───────────────────────┐
                                          │CustomUserDetailsService│
                                          │   loadUserByUsername() │
                                          └───────────┬───────────┘
                                                      │
                                                      ▼
                                          ┌───────────────┐
                                          │@PreAuthorize  │
                                          │hasRole('ADMIN')│
                                          └───────┬───────┘
                                                  │
                                                  ▼
                                          ┌───────────────┐
                                          │UserController │
                                          │ getAllUsers() │
                                          └───────┬───────┘
                                                  │
                                                  ▼
┌─────────┐       Liste des users         ┌───────────────┐
│ CLIENT  │ ◄─────────────────────────────│   Response    │
└─────────┘                               └───────────────┘
```

---

## 📡 API Endpoints

### Authentification

#### POST `/api/auth/register`
**Corps de la requête** :
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "phoneNumber": "+33612345678",
  "password": "password123",
  "roleType": "USER"
}
```

**Réponse** (200 OK) :
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
    "phoneNumber": "+33612345678"
  }
}
```

#### POST `/api/auth/login`
**Corps de la requête** :
```json
{
  "username": "john_doe",
  "password": "password123"
}
```

**Réponse** (200 OK) :
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer"
}
```

### Gestion des utilisateurs (nécessite token ADMIN)

#### GET `/api/users`
**Header** : `Authorization: Bearer <token>`

**Réponse** : Liste de tous les utilisateurs

#### GET `/api/users/{id}`
**Réponse** : Détails d'un utilisateur

#### POST `/api/users`
**Corps** : Données du nouvel utilisateur

#### PUT `/api/users/{id}`
**Corps** : Données à mettre à jour

#### DELETE `/api/users/{id}`
**Réponse** : 204 No Content

---

## 🚀 Comment utiliser l'application

### 1. Prérequis

- Java 17+
- Maven
- PostgreSQL (avec une base `project_spring`)

### 2. Configuration

Modifier `src/main/resources/application.properties` :
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/project_spring
spring.datasource.username=postgres
spring.datasource.password=votre_mot_de_passe
```

### 3. Lancer l'application

```bash
./mvnw spring-boot:run
```

### 4. Accéder à Swagger

Ouvrir : http://localhost:8080/swagger-ui.html

### 5. Tester l'API

1. **S'inscrire** : POST `/api/auth/register`
2. **Se connecter** : POST `/api/auth/login` → récupérer le token
3. **Utiliser le token** : Ajouter `Authorization: Bearer <token>` dans les headers

---

## 📌 Oui, SecurityConfig.java est bien utilisé !

Le fichier `SecurityConfig.java` est **essentiel** au projet car :

1. ✅ Il configure **Spring Security** pour l'application
2. ✅ Il définit le **PasswordEncoder** (BCrypt) utilisé partout
3. ✅ Il crée l'**AuthenticationManager** utilisé dans AuthController
4. ✅ Il configure la **chaîne de filtres de sécurité**
5. ✅ Il intègre le **JwtFilter** pour la vérification des tokens

Sans ce fichier, l'authentification JWT ne fonctionnerait pas !

---

## 🎓 Résumé pour les débutants

| Concept | Ce que c'est | Fichier(s) |
|---------|--------------|------------|
| **Entité** | Représente une table en BD | User.java, Role.java, Credentials.java |
| **Repository** | Accès à la BD | *Repository.java |
| **Service** | Logique métier | UserService.java |
| **Controller** | Points d'entrée API | *Controller.java |
| **DTO** | Objets de transfert | LoginRequest.java, RegisterRequest.java |
| **JWT** | Token d'authentification | JwtUtils.java, JwtFilter.java |
| **Security** | Configuration sécurité | SecurityConfig.java |

---

**Auteur** : Projet réalisé dans le cadre du cours "Introduction to Software Architecture"  
**Version** : 1.0.0
