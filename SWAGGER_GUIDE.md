# Accès à Swagger UI

## Configuration complétée ✅

Swagger UI a été configuré avec succès dans votre projet Spring Boot.

## Comment accéder à Swagger

Après avoir démarré votre application Spring Boot, vous pouvez accéder à Swagger UI via les URLs suivantes :

### Interface Swagger UI (recommandé)
```
http://localhost:8080/swagger-ui.html
```
ou
```
http://localhost:8080/swagger-ui/index.html
```

### Documentation API (JSON)
```
http://localhost:8080/v3/api-docs
```

## Fonctionnalités disponibles

### 1. **Page d'accueil**
- **GET** `/` - Informations de base sur l'API

### 2. **Authentification**
- **POST** `/api/auth/register` - Inscription d'un nouvel utilisateur
- **POST** `/api/auth/login` - Connexion et obtention du token JWT

## Comment tester l'API avec Swagger

1. **Démarrez votre application Spring Boot**
   ```bash
   mvnw.cmd spring-boot:run
   ```

2. **Ouvrez votre navigateur** et accédez à `http://localhost:8080/swagger-ui.html`

3. **Testez l'inscription** :
   - Cliquez sur l'endpoint `/api/auth/register`
   - Cliquez sur "Try it out"
   - Entrez un nom d'utilisateur et un mot de passe :
     ```json
     {
       "username": "testuser",
       "password": "testpassword"
     }
     ```
   - Cliquez sur "Execute"

4. **Testez la connexion** :
   - Cliquez sur l'endpoint `/api/auth/login`
   - Cliquez sur "Try it out"
   - Entrez les mêmes identifiants
   - Cliquez sur "Execute"
   - Copiez le token JWT retourné

5. **Utilisez le token pour les endpoints protégés** :
   - Cliquez sur le bouton "Authorize" en haut de la page
   - Entrez : `Bearer votre-token-jwt`
   - Cliquez sur "Authorize"
   - Maintenant vous pouvez accéder aux endpoints protégés

## Configuration

Les configurations Swagger se trouvent dans :
- `OpenApiConfig.java` - Configuration personnalisée
- `application.properties` - Paramètres SpringDoc
- `SecurityConfig.java` - Autorisations pour les endpoints Swagger

## Dépendances ajoutées

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```
