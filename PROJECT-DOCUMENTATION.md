# Documentation Technique Complete du Projet

**Spring Boot + PostgreSQL + RabbitMQ + MailHog + JWT + Nginx + Docker**

**M1 Informatique - Architecture & Services**

---

# TABLE DES MATIERES

1. [Presentation generale](#1-presentation-generale)
2. [Architecture du projet](#2-architecture-du-projet)
3. [Structure des fichiers](#3-structure-des-fichiers)
4. [Les dependances Maven (pom.xml)](#4-les-dependances-maven-pomxml)
5. [Configuration (application.properties)](#5-configuration-applicationproperties)
6. [Couche Entity (les tables de la base de donnees)](#6-couche-entity)
7. [Couche Repository (acces a la base de donnees)](#7-couche-repository)
8. [Couche DTO (les objets de transfert)](#8-couche-dto)
9. [Couche Service (la logique metier)](#9-couche-service)
10. [Couche Controller (les endpoints API)](#10-couche-controller)
11. [Securite : JWT + Spring Security](#11-securite-jwt--spring-security)
12. [Messagerie asynchrone : RabbitMQ](#12-messagerie-asynchrone-rabbitmq)
13. [Verification d'e-mail : le flux complet](#13-verification-demail-le-flux-complet)
14. [API Gateway : Nginx](#14-api-gateway-nginx)
15. [Conteneurisation : Docker](#15-conteneurisation-docker)
16. [Documentation API : Swagger / OpenAPI](#16-documentation-api-swagger--openapi)
17. [Comment lancer le projet](#17-comment-lancer-le-projet)
18. [Comment tester le projet](#18-comment-tester-le-projet)
19. [Resume pour le pitch](#19-resume-pour-le-pitch)

---

# 1. Presentation generale

## Qu'est-ce qu'on a construit ?

Une **API REST complete** avec :

- **Inscription** d'utilisateurs avec verification d'e-mail par lien
- **Authentification** par token JWT (JSON Web Token)
- **Gestion des utilisateurs** (CRUD) protegee par roles (ADMIN/USER)
- **Messagerie asynchrone** via RabbitMQ pour l'envoi d'e-mails
- **API Gateway** Nginx qui valide le JWT avant de transmettre au backend
- **Tout conteneurise** dans Docker (base de donnees, backend, messagerie, mail, proxy)

## Les 5 services Docker

| Service | Image | Port | Role |
|---------|-------|------|------|
| **PostgreSQL** | `postgres:16-alpine` | 5432 | Base de donnees relationnelle |
| **RabbitMQ** | `rabbitmq:3-management` | 5672 + 15672 | Messagerie asynchrone entre services |
| **MailHog** | `mailhog/mailhog` | 1025 + 8025 | Faux serveur SMTP (capture les e-mails) |
| **Backend** | Build depuis Dockerfile | 8080 | API Spring Boot (notre application) |
| **Nginx** | `nginx:alpine` | 80 | Reverse proxy + validation JWT |

---

# 2. Architecture du projet

## Vue globale

```
Client (Navigateur / Postman / curl)
    |
    v
[NGINX - Port 80] ---- API Gateway / Reverse Proxy
    |
    |-- Routes publiques (/api/auth/*) --> passent directement
    |-- Routes protegees (/api/users/*) --> Nginx verifie le JWT d'abord
    |       |
    |       |--> Subrequest vers /api/auth/validate
    |       |--> Si 200 (JWT valide) --> transmet au backend
    |       |--> Si 401 (JWT invalide) --> bloque la requete
    |
    v
[BACKEND Spring Boot - Port 8080]
    |
    |-- AuthController (/api/auth/*)
    |       |-- POST /register  --> cree user + publie evenement RabbitMQ
    |       |-- POST /login     --> retourne un JWT
    |       |-- GET  /verify    --> verifie le token e-mail
    |       |-- GET  /validate  --> valide un JWT (utilise par Nginx)
    |
    |-- UserController (/api/users/*)
    |       |-- GET    /         --> liste tous les users (ADMIN)
    |       |-- GET    /{id}     --> un user par ID (ADMIN)
    |       |-- POST   /         --> cree un user (ADMIN)
    |       |-- PUT    /{id}     --> modifie un user (ADMIN)
    |       |-- DELETE /{id}     --> supprime un user (ADMIN)
    |
    v
[PostgreSQL - Port 5432] ---- Base de donnees
    Tables : users, credentials, roles, verification_tokens

    |
[RabbitMQ - Port 5672] ---- Messagerie
    |
    v
[NotificationListener] ---- consomme les evenements
    |
    v
[MailHog - Port 1025] ---- recoit les e-mails (Port 8025 = interface web)
```

## Flux d'inscription complet

```
1. Client envoie POST /api/auth/register {username, email, password}
2. AuthController :
   - Cree User (verified=false) + Credentials (password hashee BCrypt) dans PostgreSQL
   - Genere un token UUID + stocke son hash BCrypt dans VerificationToken
   - Publie un evenement UserRegisteredEvent dans RabbitMQ
   - Repond 201 Created
3. RabbitMQ route le message vers la queue "notification.user-registered"
4. NotificationListener recoit le message automatiquement
5. Il construit le lien /verify?tokenId=xxx&t=yyy et envoie un e-mail via MailHog
6. L'utilisateur ouvre l'e-mail dans MailHog (http://localhost:8025) et clique le lien
7. AuthController.verify() :
   - Retrouve le token en base par tokenId
   - Verifie qu'il n'est pas expire (30 min)
   - Compare BCrypt(t) avec le hash stocke
   - Passe verified=true et supprime le token (usage unique)
```

---

# 3. Structure des fichiers

```
project/
|-- pom.xml                          <-- Dependances Maven
|-- Dockerfile                       <-- Image Docker du backend
|-- docker-compose.yml               <-- Orchestre tous les containers
|-- nginx.conf                       <-- Configuration du reverse proxy
|-- .dockerignore                    <-- Fichiers exclus du build Docker
|-- test-services.bat                <-- Script de test automatique
|
|-- src/main/java/com/example/project/
|   |-- ProjectApplication.java      <-- Point d'entree Spring Boot
|   |
|   |-- entity/                      <-- Les tables de la BDD (JPA)
|   |   |-- User.java               <-- Table "users"
|   |   |-- Credentials.java        <-- Table "credentials" (email, password)
|   |   |-- Role.java               <-- Table "roles" (ADMIN, USER)
|   |   |-- RoleType.java           <-- Enum des roles possibles
|   |   |-- VerificationToken.java  <-- Table "verification_tokens"
|   |
|   |-- repository/                  <-- Acces a la BDD (Spring Data JPA)
|   |   |-- UserRepository.java
|   |   |-- CredentialsRepository.java
|   |   |-- RoleRepository.java
|   |   |-- VerificationTokenRepository.java
|   |
|   |-- dto/                         <-- Objets de transfert de donnees
|   |   |-- RegisterRequest.java     <-- Body du POST /register
|   |   |-- LoginRequest.java        <-- Body du POST /login
|   |   |-- UserRegisteredEvent.java <-- Message RabbitMQ
|   |   |-- VerifyTokenResponse.java <-- Reponse du GET /validate
|   |
|   |-- service/                     <-- Logique metier
|   |   |-- UserService.java         <-- CRUD utilisateurs
|   |   |-- CustomUserDetailsService.java  <-- Charge un user pour Spring Security
|   |   |-- NotificationListener.java      <-- Consomme RabbitMQ + envoie e-mail
|   |
|   |-- controller/                  <-- Endpoints REST
|   |   |-- AuthController.java      <-- /api/auth/* (register, login, verify, validate)
|   |   |-- UserController.java      <-- /api/users/* (CRUD)
|   |   |-- HomeController.java      <-- / (page d'accueil)
|   |
|   |-- configuration/               <-- Configuration Spring
|   |   |-- SecurityConfig.java      <-- Spring Security + BCrypt + JWT filter
|   |   |-- JwtUtils.java           <-- Generation et validation des tokens JWT
|   |   |-- RabbitMQConfig.java      <-- Exchange, Queue, DLQ, Binding RabbitMQ
|   |   |-- OpenApiConfig.java       <-- Configuration Swagger/OpenAPI
|   |   |-- DataInitializer.java     <-- Cree les roles ADMIN/USER au demarrage
|   |
|   |-- filter/
|       |-- JwtFilter.java          <-- Intercepte chaque requete pour verifier le JWT
|
|-- src/main/resources/
    |-- application.properties       <-- Configuration principale (dev local)
    |-- application-docker.properties <-- Configuration Docker (remplace localhost)
    |-- data.sql                     <-- Migration SQL (ajoute colonnes manquantes)
```

---

# 4. Les dependances Maven (pom.xml)

Le fichier `pom.xml` declare toutes les bibliotheques utilisees par le projet.

| Dependance | Ce qu'elle apporte |
|------------|-------------------|
| `spring-boot-starter-webmvc` | Serveur web + API REST (controllers, @GetMapping, etc.) |
| `spring-boot-starter-data-jpa` | ORM Hibernate pour mapper les classes Java vers les tables SQL |
| `spring-boot-starter-jdbc` | Connexion a la base de donnees via JDBC |
| `spring-boot-starter-security` | Authentification, autorisation, BCrypt, filtres de securite |
| `spring-boot-starter-amqp` | Client RabbitMQ : `RabbitTemplate` pour publier, `@RabbitListener` pour consommer |
| `spring-boot-starter-mail` | `JavaMailSender` pour envoyer des e-mails |
| `spring-boot-devtools` | Rechargement automatique du code pendant le developpement |
| `postgresql` | Driver JDBC pour PostgreSQL |
| `lombok` | Genere automatiquement getters, setters, constructeurs avec des annotations |
| `jjwt-api` + `jjwt-impl` + `jjwt-jackson` | Librairie pour creer et valider les tokens JWT |
| `springdoc-openapi-starter-webmvc-ui` | Swagger UI pour documenter et tester l'API |

### Version de Spring Boot : 4.0.2

Le parent `spring-boot-starter-parent:4.0.2` gere les versions de toutes les dependances Spring.
Il suffit de declarer le nom de la dependance, Spring Boot choisit la version compatible.

---

# 5. Configuration (application.properties)

Ce fichier configure tout le comportement de l'application.

### `application.properties` (mode local)

```properties
# === SERVEUR ===
server.port=8080                                    # Port de l'API

# === BASE DE DONNEES (PostgreSQL local) ===
spring.datasource.url=jdbc:postgresql://localhost:5432/project_spring
spring.datasource.username=postgres
spring.datasource.password=dhia
spring.jpa.hibernate.ddl-auto=update               # Cree/modifie les tables automatiquement
spring.jpa.show-sql=true                            # Affiche les requetes SQL dans la console
spring.sql.init.mode=always                         # Execute data.sql a chaque demarrage
spring.jpa.defer-datasource-initialization=true     # data.sql execute APRES la creation des tables

# === JWT ===
app.secret-key=5367566B5970...                      # Cle secrete pour signer les tokens
app.expiration-time=900000                          # Duree de vie du JWT : 15 minutes (en ms)

# === SWAGGER ===
springdoc.swagger-ui.enabled=true                   # Active l'interface Swagger

# === RABBITMQ ===
spring.rabbitmq.host=localhost                      # Adresse du serveur RabbitMQ
spring.rabbitmq.port=5672                           # Port AMQP
app.mq.exchange=auth.events                         # Nom de l'exchange
app.mq.rk.user-registered=auth.user-registered      # Routing key pour les inscriptions
app.mq.queue.user-registered=notification.user-registered  # Nom de la queue
app.mq.queue.user-registered-dlq=notification.user-registered.dlq  # Dead Letter Queue

# === MAILHOG ===
spring.mail.host=localhost                          # Serveur SMTP
spring.mail.port=1025                               # Port SMTP de MailHog

# === TOKEN DE VERIFICATION ===
app.verification-token.expiration-minutes=30         # Le token expire apres 30 minutes
```

### `application-docker.properties` (mode Docker)

Quand l'application tourne dans un container Docker, elle ne peut plus utiliser `localhost`.
Elle doit utiliser les **noms de service** definis dans `docker-compose.yml` :

```properties
spring.datasource.url=jdbc:postgresql://postgres:5432/project_spring   # "postgres" = nom du service
spring.rabbitmq.host=rabbitmq                                          # "rabbitmq" = nom du service
spring.mail.host=mailhog                                               # "mailhog"  = nom du service
```

Spring Boot active ce profil avec `--spring.profiles.active=docker` dans le Dockerfile.

### `data.sql` - Migration SQL

Ce script s'execute a chaque demarrage pour ajouter les colonnes `enabled` et `verified`
a la table `users` si elles n'existent pas encore :

```sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE users ADD COLUMN IF NOT EXISTS verified BOOLEAN NOT NULL DEFAULT false;
```

---

# 6. Couche Entity

Les entites sont les classes Java qui representent les **tables de la base de donnees**.
JPA (Java Persistence API) via Hibernate convertit automatiquement ces classes en tables SQL.

## 6.1 User.java → Table `users`

**Role :** Represente un utilisateur de l'application.

| Champ | Type | Colonne SQL | Description |
|-------|------|-------------|-------------|
| `id` | Long | `id BIGSERIAL PRIMARY KEY` | ID auto-genere |
| `username` | String | `username VARCHAR UNIQUE NOT NULL` | Nom unique de l'utilisateur |
| `enabled` | Boolean | `enabled BOOLEAN DEFAULT true` | Compte actif ou desactive |
| `verified` | Boolean | `verified BOOLEAN DEFAULT false` | E-mail verifie ou non |
| `role` | Role | `role_id BIGINT REFERENCES roles(id)` | Relation vers la table roles |
| `credentials` | Credentials | -- (relation inverse) | Relation 1:1 vers credentials |

**Annotations importantes :**
- `@Entity` → dit a JPA que cette classe est une table
- `@Table(name = "users")` → nom de la table en base
- `@Data` (Lombok) → genere getters, setters, toString, equals, hashCode
- `@ManyToOne` → plusieurs users peuvent avoir le meme role
- `@OneToOne(mappedBy = "user", cascade = CascadeType.ALL)` → quand on sauvegarde un User, ses Credentials sont aussi sauvegardes

## 6.2 Credentials.java → Table `credentials`

**Role :** Stocke les identifiants sensibles (email, mot de passe), separes de User pour des raisons de securite.

| Champ | Type | Description |
|-------|------|-------------|
| `id` | Long | ID auto-genere |
| `email` | String | E-mail unique de l'utilisateur |
| `phoneNumber` | String | Telephone (optionnel, unique) |
| `password` | String | Mot de passe hashe en BCrypt |
| `user` | User | Relation 1:1 inverse vers User |

**Point de securite :** `@JsonProperty(access = WRITE_ONLY)` sur le champ `password` → on peut recevoir le mot de passe dans les requetes POST, mais il n'est **jamais renvoye** dans les reponses JSON.

## 6.3 Role.java → Table `roles`

**Role :** Represente un role utilisateur (ADMIN ou USER).

| Champ | Type | Description |
|-------|------|-------------|
| `id` | Long | ID auto-genere |
| `name` | RoleType (enum) | `ADMIN` ou `USER` |
| `description` | String | Description textuelle du role |

`@Enumerated(EnumType.STRING)` → stocke le texte "ADMIN" ou "USER" en base (pas un nombre).

## 6.4 RoleType.java (Enum)

**Role :** Definit les roles autorises. Garantit qu'on ne peut pas creer un role invalide.

```java
public enum RoleType {
    ADMIN("Administrateur avec toutes les permissions"),
    USER("Utilisateur standard avec permissions de base");
}
```

Chaque valeur a une description accessible via `getDescription()`.

## 6.5 VerificationToken.java → Table `verification_tokens`

**Role :** Stocke les tokens de verification d'e-mail de maniere securisee.

| Champ | Type | Description |
|-------|------|-------------|
| `id` | Long | ID auto-genere |
| `tokenId` | String (UUID) | Identifiant PUBLIC du token (pour le retrouver en base) |
| `tokenHash` | String | Hash BCrypt du vrai token (le secret n'est JAMAIS stocke en clair) |
| `expiresAt` | LocalDateTime | Date d'expiration (30 min apres creation) |
| `user` | User | L'utilisateur lie a ce token |

### Pourquoi un hash et pas le token en clair ?

C'est le **meme principe que les mots de passe** :

```
A L'INSCRIPTION :
  tokenClear = UUID.randomUUID()           → ex: "abc-123-def"
  tokenHash  = BCrypt.encode(tokenClear)   → ex: "$2a$10$xK3..."
  On stocke tokenHash en base. On envoie tokenClear dans l'e-mail.

A LA VERIFICATION :
  L'utilisateur clique le lien qui contient t=abc-123-def
  On fait : BCrypt.matches("abc-123-def", "$2a$10$xK3...") → true
```

**Si la base de donnees est compromise**, l'attaquant ne voit que les hash. Il ne peut **pas** retrouver les tokens en clair. Les tokens sont inutilisables.

### Pourquoi deux identifiants (tokenId et tokenClear) ?

- `tokenId` = cle de recherche (on fait `SELECT WHERE token_id = ?`)
- `tokenClear` = le secret (pour prouver l'identite)
- On ne peut **pas** chercher par hash BCrypt car le meme texte produit un hash different a chaque fois

---

# 7. Couche Repository

Les repositories sont des **interfaces** que Spring Data JPA implemente automatiquement.
On declare les methodes, Spring genere les requetes SQL.

## 7.1 UserRepository.java

```java
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);  // → SELECT * FROM users WHERE username = ?
}
```

`JpaRepository<User, Long>` fournit automatiquement : `save()`, `findById()`, `findAll()`, `delete()`, `count()`, etc.

## 7.2 CredentialsRepository.java

```java
public interface CredentialsRepository extends JpaRepository<Credentials, Long> {
    Credentials findByEmail(String email);              // Chercher par email
    Credentials findByPhoneNumber(String phoneNumber);  // Chercher par telephone
}
```

## 7.3 RoleRepository.java

```java
public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByName(RoleType name);  // Chercher par type de role (ADMIN ou USER)
}
```

## 7.4 VerificationTokenRepository.java

```java
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    VerificationToken findByTokenId(String tokenId);  // Retrouver un token par son ID public
    void deleteByTokenId(String tokenId);              // Supprimer un token (usage unique)
}
```

---

# 8. Couche DTO

Les DTO (Data Transfer Objects) sont des objets simples utilises pour **transporter des donnees**
entre le client et le serveur, ou entre services. Ils evitent d'exposer les entites directement.

## 8.1 RegisterRequest.java

**Utilise par :** `POST /api/auth/register`

```java
public class RegisterRequest {
    private String username;     // Nom d'utilisateur souhaite
    private String email;        // Adresse e-mail
    private String phoneNumber;  // Telephone (optionnel)
    private String password;     // Mot de passe en clair (sera hashe par le serveur)
    private RoleType roleType;   // Role souhaite (optionnel, defaut = USER)
}
```

## 8.2 LoginRequest.java

**Utilise par :** `POST /api/auth/login`

```java
public class LoginRequest {
    private String username;  // Nom d'utilisateur
    private String password;  // Mot de passe
}
```

## 8.3 UserRegisteredEvent.java

**Utilise par :** RabbitMQ (message qui transite entre AuthController et NotificationListener)

```java
public class UserRegisteredEvent {
    private String eventId;       // UUID unique de cet evenement (tracabilite)
    private Long userId;          // ID de l'utilisateur inscrit
    private String email;         // E-mail du destinataire
    private String tokenId;       // ID public du token (pour le lien de verification)
    private String tokenClear;    // Token en clair (pour le lien de verification)
    private Instant occurredAt;   // Horodatage de l'evenement
}
```

**Pourquoi ne pas envoyer l'entite User ?** On minimise les donnees envoyees (principe de moindre privilege).
On n'envoie que ce dont le NotificationListener a besoin.

## 8.4 VerifyTokenResponse.java

**Utilise par :** `GET /api/auth/validate` (reponse pour Nginx)

```java
public class VerifyTokenResponse {
    private boolean valid;    // Le JWT est-il valide ?
    private String username;  // Quel utilisateur ?
    private String message;   // Message descriptif
}
```

---

# 9. Couche Service

Les services contiennent la **logique metier**. Ils font le lien entre les controllers et les repositories.

## 9.1 UserService.java

**Role :** Logique CRUD pour les utilisateurs.

| Methode | Ce qu'elle fait |
|---------|----------------|
| `getAllUsers()` | Retourne tous les utilisateurs |
| `getUserById(id)` | Retourne un utilisateur par ID (ou erreur 404) |
| `createUser(user)` | Cree un utilisateur : verifie l'unicite du username/email/phone, encode le password en BCrypt, attribue le role |
| `updateUser(id, user)` | Modifie un utilisateur existant : memes verifications + mise a jour des champs |
| `deleteUser(id)` | Supprime un utilisateur par ID |

**Validations dans chaque methode :**
- Username pas deja pris
- Email pas deja pris
- Telephone pas deja pris (s'il est fourni)
- Role existe dans la base

## 9.2 CustomUserDetailsService.java

**Role :** Charge un utilisateur depuis la base de donnees pour que Spring Security puisse l'authentifier.

```java
public UserDetails loadUserByUsername(String username) {
    User user = userRepository.findByUsername(username);
    // Retourne un objet UserDetails avec : username, password hashee, role
    // Spring Security utilise cet objet pour verifier le mot de passe et les autorisations
}
```

Spring Security appelle cette methode automatiquement lors du login.
Le role est prefixe par "ROLE_" (convention Spring) : `ROLE_ADMIN`, `ROLE_USER`.

## 9.3 NotificationListener.java

**Role :** Consomme les evenements RabbitMQ et envoie des e-mails.

C'est **le coeur du decouplage** : cette classe ecoute RabbitMQ et envoie les e-mails **sans que AuthController ne le sache**.

```java
@RabbitListener(queues = "${app.mq.queue.user-registered}")
public void handleUserRegistered(UserRegisteredEvent event) {
    // 1. Construire le lien : http://localhost:8080/api/auth/verify?tokenId=xxx&t=yyy
    // 2. Creer un SimpleMailMessage (destinataire, sujet, corps)
    // 3. mailSender.send(message)  → envoie via SMTP vers MailHog
}
```

**`@RabbitListener`** : Spring ecoute automatiquement la queue. Quand un message arrive, il le convertit de JSON vers `UserRegisteredEvent` et appelle la methode.

**En cas d'erreur :**
1. RabbitMQ retente automatiquement
2. Si ca echoue encore → message envoye dans la **DLQ** (Dead Letter Queue)
3. On peut inspecter les messages en erreur dans http://localhost:15672

---

# 10. Couche Controller

Les controllers recoivent les requetes HTTP et retournent des reponses JSON.

## 10.1 AuthController.java

4 endpoints dans `/api/auth/` :

### POST /api/auth/register — Inscription

```
Entree : { username, email, password, phoneNumber?, roleType? }
Sortie : 201 Created { message, userId, username, verified: false }
```

Etapes :
1. **Validation** : username/email/phone pas deja utilises
2. **Creation User** : `verified = false`, role = USER par defaut
3. **Creation Credentials** : password hashee avec BCrypt
4. **Generation token** : `tokenId` (UUID) + `tokenClear` (UUID) → stocke `BCrypt(tokenClear)` en base
5. **Publication RabbitMQ** : `rabbitTemplate.convertAndSend(exchange, routingKey, event)` — **une seule ligne** qui fait tout le decouplage
6. **Reponse 201**

### POST /api/auth/login — Connexion

```
Entree : { username, password }
Sortie : 200 OK { token: "eyJhbGci...", type: "Bearer" }
```

Utilise `AuthenticationManager` de Spring Security pour verifier le username/password.
Si valide, genere un JWT avec `JwtUtils.generateToken(username)`.

### GET /api/auth/verify?tokenId=...&t=... — Verification e-mail

```
Entree : tokenId (query param) + t (query param)
Sortie : 200 OK { message, username, verified: true } ou 400 si invalide/expire
```

Etapes :
1. Retrouver le token en base par `tokenId`
2. Verifier l'expiration (`expiresAt < now` → rejete)
3. Comparer BCrypt : `passwordEncoder.matches(t, tokenHash)`
4. Si OK → `user.setVerified(true)` + sauvegarder
5. Supprimer le token (usage unique = **one-shot**)

`@Transactional` car on fait un UPDATE + un DELETE dans la meme operation.
**Idempotent** : si le compte est deja verifie, retourne un message sans erreur.

### GET /api/auth/validate — Validation JWT (pour Nginx)

```
Entree : Header "Authorization: Bearer <jwt>"
Sortie : 200 si valide, 401 si invalide
```

Nginx appelle cet endpoint en **subrequest** avant de transmettre les requetes protegees.
Il extrait le JWT du header, verifie la signature et l'expiration.

## 10.2 UserController.java

5 endpoints CRUD dans `/api/users/` — tous proteges par `@PreAuthorize("hasRole('ADMIN')")` :

| Methode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/users` | Liste tous les utilisateurs |
| GET | `/api/users/{id}` | Un utilisateur par ID |
| POST | `/api/users` | Creer un utilisateur |
| PUT | `/api/users/{id}` | Modifier un utilisateur |
| DELETE | `/api/users/{id}` | Supprimer un utilisateur |

`@PreAuthorize("hasRole('ADMIN')")` → seuls les utilisateurs avec le role ADMIN peuvent acceder a ces endpoints.

## 10.3 HomeController.java

Un seul endpoint `GET /` qui retourne un message de bienvenue et la liste des endpoints disponibles. Utile pour verifier que l'application fonctionne.

---

# 11. Securite : JWT + Spring Security

## Comment fonctionne l'authentification JWT ?

```
1. L'utilisateur envoie POST /api/auth/login { username, password }
2. Spring Security verifie le password avec BCrypt
3. Si OK, JwtUtils genere un token JWT signe avec la cle secrete
4. Le token est retourne au client : { token: "eyJ...", type: "Bearer" }

5. Pour chaque requete suivante, le client envoie le token dans le header :
   Authorization: Bearer eyJ...

6. JwtFilter intercepte la requete AVANT qu'elle arrive au controller
7. Il extrait le JWT, verifie la signature et l'expiration
8. Si valide → l'utilisateur est authentifie pour cette requete
9. Si invalide → erreur 401
```

## JwtUtils.java — Generation et validation des tokens

| Methode | Role |
|---------|------|
| `generateToken(username)` | Cree un JWT avec le username, la date de creation et l'expiration (15 min) |
| `extractUsername(token)` | Lit le claim "sub" du JWT pour obtenir le username |
| `validateToken(token, userDetails)` | Verifie la signature + l'expiration + que le username correspond |
| `isTokenExpired(token)` | Verifie si la date d'expiration est passee |

Le JWT est signe avec l'algorithme **HMAC-SHA256** et la cle secrete stockee dans `application.properties`.

## JwtFilter.java — Le filtre qui intercepte chaque requete

C'est un `OncePerRequestFilter` : il s'execute **une seule fois par requete HTTP**.

```
Pour chaque requete :
1. Lire le header "Authorization"
2. Si pas de header ou pas "Bearer " → passer au filtre suivant (pas d'auth)
3. Extraire le JWT (apres "Bearer ")
4. Extraire le username du JWT
5. Charger le UserDetails depuis la base
6. Valider le token
7. Si valide → creer un UsernamePasswordAuthenticationToken et le mettre dans le SecurityContext
8. Le controller peut maintenant savoir QUI est l'utilisateur
```

## SecurityConfig.java — Configuration Spring Security

```java
http
    .csrf(disable)                        // Desactive CSRF (on utilise JWT, pas des sessions)
    .sessionManagement(STATELESS)         // Pas de sessions serveur (tout est dans le JWT)
    .authorizeHttpRequests(permitAll)     // Toutes les requetes sont autorisees par defaut
    .addFilterBefore(JwtFilter, ...)      // Ajoute le JwtFilter AVANT le filtre Spring Security
```

`@EnableMethodSecurity` active `@PreAuthorize` sur les controllers (securite au niveau des methodes).
`BCryptPasswordEncoder` est le bean utilise pour hasher les mots de passe et les tokens.

---

# 12. Messagerie asynchrone : RabbitMQ

## Pourquoi utiliser RabbitMQ ?

| Sans RabbitMQ (couplage fort) | Avec RabbitMQ (decouplage) |
|-------------------------------|----------------------------|
| `register()` appelle directement `sendEmail()` | `register()` publie un evenement, c'est tout |
| Si l'envoi de mail echoue, l'inscription echoue | Si l'envoi de mail echoue, l'inscription reussit quand meme |
| On ne peut pas ajouter d'autres actions sans modifier register() | On peut ajouter des listeners (SMS, analytics) sans toucher a register() |
| Pas de resilience | Les messages restent dans la queue en cas de panne |

## RabbitMQConfig.java — La plomberie

5 beans declares :

### 1. TopicExchange `auth.events`

Le "bureau de tri postal". Recoit les messages et les route vers les bonnes queues selon la **routing key**.
Type "topic" = routage par pattern (ex: `auth.user-registered`).

### 2. Queue `notification.user-registered` (principale)

La "boite aux lettres" du service Notification. Configuree avec :
- `x-dead-letter-exchange = ""` → en cas d'erreur, utiliser l'exchange par defaut
- `x-dead-letter-routing-key = "notification.user-registered.dlq"` → rediriger vers la DLQ

### 3. Queue `notification.user-registered.dlq` (Dead Letter Queue)

Les messages en erreur arrivent ici. On peut les inspecter dans http://localhost:15672.

### 4. Binding

La "regle de tri" : quand un message avec la routing key `auth.user-registered` arrive sur l'exchange `auth.events`, il est envoye dans la queue `notification.user-registered`.

### 5. Jackson2JsonMessageConverter

Convertit les objets Java en JSON et inversement. Sans ca, les messages seraient en binaire (illisible).
Grace a ce bean, on peut voir les messages au format JSON dans le panneau RabbitMQ.

## Le flux dans RabbitMQ

```
AuthController                    RabbitMQ                         NotificationListener
     |                               |                                    |
     |--- convertAndSend(event) ---->|                                    |
     |                               |--- route vers queue ------------>|
     |                               |    (auth.user-registered)         |
     |                               |                                    |
     |                               |              handleUserRegistered(event)
     |                               |                                    |
     |                               |                    envoie l'e-mail via MailHog
     |                               |                                    |
     |                               |     SI ERREUR :                    |
     |                               |<--- message va dans la DLQ -------|
```

---

# 13. Verification d'e-mail : le flux complet

## Etape 1 : Inscription

Le client envoie :
```json
POST /api/auth/register
{ "username": "alice", "email": "alice@example.com", "password": "secret123" }
```

Le serveur :
1. Cree `User(username=alice, verified=false)`
2. Cree `Credentials(email=alice@example.com, password=BCrypt(secret123))`
3. Genere `tokenId = "tok-abc"` et `tokenClear = "sec-xyz"`
4. Stocke `VerificationToken(tokenId=tok-abc, tokenHash=BCrypt(sec-xyz), expiresAt=+30min)`
5. Publie `UserRegisteredEvent` dans RabbitMQ

## Etape 2 : E-mail

Le `NotificationListener` recoit l'evenement et envoie un e-mail contenant :
```
Cliquez ici pour verifier votre compte :
http://localhost:8080/api/auth/verify?tokenId=tok-abc&t=sec-xyz
```

L'e-mail est visible dans **MailHog** : http://localhost:8025

## Etape 3 : Verification

L'utilisateur clique le lien. Le serveur :
1. Cherche le token par `tokenId = tok-abc`
2. Verifie que `expiresAt` n'est pas depasse
3. Compare : `BCrypt.matches("sec-xyz", tokenHash)` → `true`
4. Met `user.verified = true`
5. Supprime le token (il ne peut plus etre reutilise)

**Resultat :** Le compte est maintenant actif et verifie.

---

# 14. API Gateway : Nginx

## Pourquoi Nginx ?

Nginx sert de **point d'entree unique** sur le port 80. Les avantages :

1. **Securite** : Nginx valide le JWT **avant** de transmettre au backend
2. **Routage** : Un seul point d'entree pour tous les services
3. **Decouplage** : Le client ne connait pas l'adresse reelle du backend

## Comment fonctionne `auth_request` ?

```
Client envoie : GET /api/users (avec JWT dans le header)
    |
    v
Nginx recoit la requete
    |
    |--> SUBREQUEST interne vers /_validate_jwt
    |       |
    |       |--> proxy_pass vers http://backend:8080/api/auth/validate
    |       |--> transmet le header Authorization (le JWT)
    |       |
    |       |<-- Spring Boot repond 200 (JWT valide) ou 401 (invalide)
    |
    |--> Si 200 : proxy_pass vers http://backend:8080/api/users (la vraie requete)
    |--> Si 401 : retourne {"error": "Unauthorized"} au client
```

## Routes definies dans nginx.conf

| Route | Type | Auth requise ? |
|-------|------|---------------|
| `/api/auth/*` | Publique | Non |
| `/swagger-ui.html`, `/swagger-ui/*` | Publique | Non |
| `/v3/api-docs` | Publique | Non |
| `/` | Publique | Non |
| `/api/users/*` | Protegee | Oui (JWT valide par auth_request) |

## Configuration upstream

```nginx
upstream spring_backend {
    server backend:8080;   # "backend" = nom du service dans docker-compose.yml
}
```

Docker resout automatiquement le nom `backend` vers l'IP du container.

---

# 15. Conteneurisation : Docker

## Dockerfile — Comment construire l'image du backend

On utilise un **multi-stage build** (2 etapes) :

```
ETAPE 1 (build) — Image JDK (grosse, ~500 MB)
    ├── Copier pom.xml + mvnw
    ├── Telecharger les dependances Maven (cache Docker)
    ├── Copier le code source
    └── Compiler avec : mvnw package -DskipTests

ETAPE 2 (run) — Image JRE (legere, ~200 MB)
    ├── Copier le JAR compile depuis l'etape 1
    └── Lancer avec : java -jar app.jar --spring.profiles.active=docker
```

**Avantage du multi-stage :** L'image finale ne contient que le JAR et Java (pas Maven, pas le code source).

## docker-compose.yml — Orchestration des 5 services

```
                   depends_on
postgres ◄──────────── backend ──────────► rabbitmq
(healthcheck)         (port 8080)        (healthcheck)
                        |
                        ▼
                      mailhog
                        |
                        ▼
                      nginx ──────────► backend
                    (port 80)
```

**Ordre de demarrage :**
1. PostgreSQL demarre et attend d'etre "healthy" (healthcheck: `pg_isready`)
2. RabbitMQ demarre et attend d'etre "healthy" (healthcheck: `rabbitmq-diagnostics`)
3. MailHog demarre
4. **Backend** attend que PostgreSQL ET RabbitMQ soient healthy, puis demarre
5. **Nginx** attend que le backend soit lance, puis demarre

**`depends_on` avec `condition: service_healthy`** garantit que le backend ne demarre pas avant que la base de donnees et RabbitMQ soient prets.

## .dockerignore

Exclut les fichiers inutiles du build Docker :
```
target/      # Les fichiers compiles (on recompile dans Docker)
.git/        # L'historique Git
.idea/       # Les fichiers IntelliJ
```

## application-docker.properties

Quand Spring Boot tourne dans Docker, il utilise les **noms de service** au lieu de `localhost` :

| En local (application.properties) | En Docker (application-docker.properties) |
|----------------------------------|------------------------------------------|
| `localhost:5432` (PostgreSQL) | `postgres:5432` |
| `localhost:5672` (RabbitMQ) | `rabbitmq:5672` |
| `localhost:1025` (MailHog) | `mailhog:1025` |

---

# 16. Documentation API : Swagger / OpenAPI

## OpenApiConfig.java

Configure l'interface Swagger avec :
- Le titre de l'API : "API Spring Boot - Authentication"
- Le support de l'authentification Bearer JWT dans l'interface Swagger
- L'utilisateur peut entrer son JWT dans Swagger et tester les endpoints proteges

## Comment acceder a Swagger ?

- **Directement** : http://localhost:8080/swagger-ui.html
- **Via Nginx** : http://localhost/swagger-ui.html

## Annotations utilisees dans les controllers

| Annotation | Role |
|-----------|------|
| `@Tag(name = "...")` | Groupe les endpoints par categorie dans Swagger |
| `@Operation(summary = "...")` | Description courte de l'endpoint |
| `@ApiResponse(responseCode = "200")` | Documente les reponses possibles |
| `@Schema` | Decrit la structure des objets JSON |

---

# 17. Comment lancer le projet

## Prerequis

- **Docker Desktop** installe et lance
- **Java 17+** (seulement si vous lancez le backend en local hors Docker)

## Methode 1 : Tout dans Docker (recommandee)

```bash
# Depuis le dossier du projet
docker-compose up -d --build

# Verifier que tout tourne
docker-compose ps

# Voir les logs du backend
docker-compose logs -f backend
```

Attendez de voir `Started ProjectApplication` dans les logs.

## Methode 2 : Script automatique

Double-cliquez sur `test-services.bat` — il fait tout automatiquement.

## Methode 3 : Backend en local + services dans Docker

```bash
# Lancer seulement PostgreSQL, RabbitMQ, MailHog
docker-compose up -d postgres rabbitmq mailhog

# Lancer Spring Boot depuis IntelliJ (Run ProjectApplication)
```

---

# 18. Comment tester le projet

## URLs des interfaces web

| Service | URL | Login |
|---------|-----|-------|
| Swagger (API docs) | http://localhost:8080/swagger-ui.html | — |
| MailHog (e-mails) | http://localhost:8025 | — |
| RabbitMQ (messagerie) | http://localhost:15672 | guest / guest |
| Nginx (proxy) | http://localhost | — |

## Test 1 : Inscription

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","password":"pass123"}'
```

Reponse attendue : `201 Created` avec `verified: false`

## Test 2 : Verifier l'e-mail

1. Ouvrez http://localhost:8025 (MailHog)
2. Vous voyez un e-mail adresse a `alice@example.com`
3. Cliquez dessus et copiez le lien de verification
4. Collez le lien dans le navigateur

Reponse attendue : `200 OK` avec `verified: true`

## Test 3 : Connexion

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"pass123"}'
```

Reponse attendue : `200 OK` avec un token JWT

## Test 4 : Requete protegee (via Nginx)

```bash
curl http://localhost/api/users \
  -H "Authorization: Bearer VOTRE_TOKEN_JWT"
```

Sans token → `401 Unauthorized`
Avec un token ADMIN valide → liste des utilisateurs

## Test 5 : Verifier RabbitMQ

1. Ouvrez http://localhost:15672 (guest / guest)
2. Onglet **Exchanges** → vous voyez `auth.events`
3. Onglet **Queues** → vous voyez `notification.user-registered` et la DLQ

## Test 6 : Tester la DLQ

```bash
docker-compose stop mailhog            # Arreter MailHog
# Faire un POST /register              # Le listener echouera
# Ouvrir http://localhost:15672         # Le message est dans la DLQ
docker-compose start mailhog            # Relancer MailHog
```

---

# 19. Resume pour le pitch

## Les 6 points cles a expliquer

1. **"On ne stocke JAMAIS le token en clair en base."**
   → Meme principe que les mots de passe : on stocke le hash BCrypt. Si la base fuit, les tokens sont inutilisables.

2. **"Auth ne sait pas comment l'e-mail est envoye."**
   → C'est le decouplage par messagerie. AuthController publie un fait dans RabbitMQ, point final. NotificationListener s'occupe du reste independamment.

3. **"Si le service mail est en panne, rien n'est perdu."**
   → Les messages s'accumulent dans la queue RabbitMQ et sont traites des que le service revient. C'est la resilience.

4. **"Les messages en erreur vont dans la DLQ."**
   → On peut les voir dans le panneau RabbitMQ et les retraiter. C'est l'observabilite.

5. **"Nginx valide le JWT avant de transmettre au backend."**
   → Le backend ne recoit que des requetes deja authentifiees. C'est la separation des responsabilites (API Gateway pattern).

6. **"Tout tourne dans Docker avec une seule commande."**
   → `docker-compose up -d --build` lance PostgreSQL, RabbitMQ, MailHog, le backend Spring Boot et Nginx. Rien a installer manuellement.

## Resume des fichiers

| Fichier | Lignes | Ce qu'il fait |
|---------|--------|---------------|
| `User.java` | 87 | Entite utilisateur (username, enabled, verified, role) |
| `Credentials.java` | 84 | Entite identifiants (email, phone, password hashee) |
| `Role.java` | 49 | Entite role (ADMIN/USER avec enum) |
| `VerificationToken.java` | 75 | Token de verification (tokenId, tokenHash, expiresAt) |
| `AuthController.java` | 363 | 4 endpoints : register, login, verify, validate |
| `UserController.java` | 162 | CRUD utilisateurs (protege par role ADMIN) |
| `NotificationListener.java` | 100 | Ecoute RabbitMQ et envoie les e-mails |
| `UserService.java` | 190 | Logique CRUD avec validations |
| `RabbitMQConfig.java` | 156 | Exchange, queues, DLQ, binding, JSON converter |
| `SecurityConfig.java` | 54 | BCrypt, JWT filter, session stateless |
| `JwtUtils.java` | 258 | Generation et validation des tokens JWT |
| `JwtFilter.java` | 65 | Intercepte chaque requete pour verifier le JWT |
| `nginx.conf` | 141 | Reverse proxy avec auth_request pour valider JWT |
| `docker-compose.yml` | 141 | 5 services : postgres, rabbitmq, mailhog, backend, nginx |
| `Dockerfile` | 46 | Multi-stage build du backend |
| `application.properties` | 54 | Configuration complete (BDD, JWT, RabbitMQ, Mail) |
