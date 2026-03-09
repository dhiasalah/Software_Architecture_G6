# ==============================================================================
# DOCKERFILE - Backend Spring Boot
# ==============================================================================
#
# Ce fichier décrit comment construire l'image Docker du backend.
# On utilise un "multi-stage build" :
#   1. Étape 1 (build) : compile le projet avec Maven
#   2. Étape 2 (run)   : lance le JAR compilé avec Java
#
# Pourquoi multi-stage ?
#   → L'image finale est petite (pas de Maven, pas de code source)
#   → Seulement le JAR + Java Runtime
# ==============================================================================

# ---- ÉTAPE 1 : Compilation avec Maven ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copier les fichiers Maven (pour télécharger les dépendances d'abord = cache Docker)
COPY pom.xml mvnw ./
COPY .mvn .mvn

# Rendre le wrapper Maven exécutable et télécharger les dépendances
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copier le code source
COPY src src

# Compiler le projet (sans lancer les tests pour aller plus vite)
RUN ./mvnw package -DskipTests -B

# ---- ÉTAPE 2 : Image finale légère ----
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copier le JAR compilé depuis l'étape 1
COPY --from=build /app/target/*.jar app.jar

# Port exposé (Spring Boot écoute sur 8080)
EXPOSE 8080

# Lancer l'application avec le profil "docker"
# Le profil "docker" utilise application-docker.properties
# qui contient les URLs internes Docker (postgres, rabbitmq, mailhog)
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
