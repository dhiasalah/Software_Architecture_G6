# 🔧 SOLUTION : Erreur JWT Signature Does Not Match

## ❌ Problème rencontré

Vous obtenez cette erreur dans les logs :
```
JWT signature does not match locally computed signature. JWT validity cannot be asserted and should not be trusted.
```

## ✅ Solution en 3 étapes

### Étape 1 : Reconnectez-vous
Envoyez une requête de login pour obtenir un **nouveau token** :

**URL** : `POST http://localhost:8080/api/auth/login`

**Body** :
```json
{
  "username": "admin",
  "password": "AdminPass123!"
}
```

**Réponse** :
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTcwNzQxMjgwMCwiZXhwIjoxNzA3NDEzNzAwfQ.abc123...",
  "type": "Bearer"
}
```

### Étape 2 : Copiez le nouveau token
Copiez **uniquement** le token (la longue chaîne de caractères après `"token": "`), **SANS** les guillemets.

### Étape 3 : Utilisez ce nouveau token
Dans Postman, pour toutes vos requêtes protégées :
1. Allez dans l'onglet **Headers**
2. Ajoutez :
   - **Key** : `Authorization`
   - **Value** : `Bearer ` + le token copié

**Exemple** :
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTcwNzQxMjgwMCwiZXhwIjoxNzA3NDEzNzAwfQ.abc123...
```

## 🤔 Pourquoi cette erreur ?

Cette erreur se produit quand :
- ✅ **Vous avez redémarré l'application** : Les anciens tokens ne sont plus valides
- ✅ **Le token a expiré** : Durée de vie = 15 minutes (900000 ms)
- ❌ **Le token est mal copié** : Espaces ou caractères en trop

## 💡 Astuce : Automatiser dans Postman

Pour éviter de copier/coller le token à chaque fois :

1. **Créez un environnement Postman** :
   - Cliquez sur "Environments" (icône œil en haut à droite)
   - Créez un nouvel environnement (ex: "Dev")
   - Ajoutez une variable `token`

2. **Après chaque login, sauvegardez le token automatiquement** :
   - Dans votre requête `/login`, allez dans l'onglet **Tests**
   - Ajoutez ce script :
   ```javascript
   var jsonData = pm.response.json();
   pm.environment.set("token", jsonData.token);
   ```

3. **Utilisez la variable dans vos requêtes** :
   - Header : `Authorization`
   - Value : `Bearer {{token}}`

Maintenant, le token sera automatiquement mis à jour après chaque login ! 🎉

## 🔍 Vérification

Pour vérifier que tout fonctionne :

1. **Login** : `POST /api/auth/login`
2. **Testez** : `GET /api/users` avec le header Authorization
3. **Succès** : Vous devriez voir la liste des utilisateurs (code 200)

Si vous obtenez encore une erreur 403 ou 500, recommencez depuis l'étape 1.

## 📞 Besoin d'aide ?

Si le problème persiste :
1. Vérifiez que PostgreSQL est démarré
2. Vérifiez que l'application Spring Boot fonctionne (port 8080)
3. Consultez le fichier `POSTMAN_EXAMPLES.md` pour plus d'exemples
