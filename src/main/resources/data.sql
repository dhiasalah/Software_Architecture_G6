-- ============================================================
-- Script de migration : ajouter les colonnes enabled et verified
-- a la table users existante.
--
-- Spring Boot execute ce fichier automatiquement au demarrage
-- si spring.sql.init.mode=always dans application.properties
--
-- IF NOT EXISTS evite les erreurs si les colonnes existent deja
-- ============================================================

-- Ajouter la colonne 'enabled' si elle n'existe pas
ALTER TABLE users ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT true;

-- Ajouter la colonne 'verified' si elle n'existe pas
ALTER TABLE users ADD COLUMN IF NOT EXISTS verified BOOLEAN NOT NULL DEFAULT false;
