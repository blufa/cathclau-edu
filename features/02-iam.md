> **Contexte :** ce fichier fait partie des specs de la plateforme SaaS scolaire. À lire avec `CLAUDE.md` (modèle de données + architecture Spring Modulith / hexagonale) et `features/00-conventions.md` (conventions transverses : format API, codes HTTP, sécurité).
> Implémente **TOUT** ce fichier : tous les endpoints, toutes les règles métier, tous les événements. Ne rien omettre.

# 2. Module `iam` — Utilisateurs, Sécurité & Multi-tenant 🟢

## 2.1 Authentification

**Endpoints**
- `POST   /api/v1/auth/login` — email + mot de passe → access token (JWT) + refresh token.
- `POST   /api/v1/auth/refresh` — renouveler l'access token.
- `POST   /api/v1/auth/logout` — invalider le refresh token.
- `POST   /api/v1/auth/mot-de-passe/oubli` — demande de réinitialisation (envoi lien/code).
- `POST   /api/v1/auth/mot-de-passe/reset` — réinitialisation via token.
- `POST   /api/v1/auth/mot-de-passe/changer` — changement par l'utilisateur connecté.
- `GET    /api/v1/auth/me` — profil + rôles + permissions + tenant.

**Règles métier**
- Le JWT embarque `tenant_id`, `user_id`, rôles et permissions. Le filtre alimente `TenantContext`.
- Mot de passe hashé (BCrypt/Argon2). Politique : longueur min, complexité, expiration optionnelle.
- Verrouillage après N échecs (anti-bruteforce), `derniere_connexion` mise à jour au succès.
- Résolution du tenant : sous-domaine OU claim JWT. Un utilisateur appartient à un seul tenant (sauf SUPER_ADMIN, hors tenant).

**Événements**
- `UtilisateurConnecte` → mise à jour `derniere_connexion`, audit.
- `MotDePasseReinitialise` → notification de sécurité.

## 2.2 Gestion des utilisateurs

**Endpoints**
- `POST   /api/v1/utilisateurs` — créer (admin du tenant).
- `GET    /api/v1/utilisateurs` — lister (filtres rôle, actif, recherche nom/email).
- `GET    /api/v1/utilisateurs/{id}` — détail.
- `PUT    /api/v1/utilisateurs/{id}` — modifier.
- `PATCH  /api/v1/utilisateurs/{id}/activer` / `/desactiver`.
- `DELETE /api/v1/utilisateurs/{id}` — soft delete.

**Règles métier**
- Email unique **par tenant**.
- Désactiver un utilisateur révoque ses sessions.
- La création d'un Élève / Parent / Enseignant peut créer le compte `Utilisateur` lié (relation `0..1`).

## 2.3 Rôles & permissions (RBAC)

**Endpoints**
- `GET    /api/v1/roles` / `POST` / `PUT /{id}` / `DELETE /{id}`.
- `GET    /api/v1/permissions` — catalogue (lecture seule, alimenté au boot).
- `POST   /api/v1/roles/{id}/permissions` — affecter des permissions (remplace `RolePermission`).
- `POST   /api/v1/utilisateurs/{id}/roles` — affecter des rôles (`UtilisateurRole`).
- `DELETE /api/v1/utilisateurs/{id}/roles/{roleId}`.

**Règles métier**
- Rôles standards : `SUPER_ADMIN, ADMIN_ECOLE, DIRECTEUR, COMPTABLE, SECRETAIRE, ENSEIGNANT, PARENT, ELEVE`.
- Permissions au format `ressource:action` (ex. `eleve:create`, `note:read`, `bulletin:publish`).
- `SUPER_ADMIN` : permissions plateforme, jamais scopé tenant.
- Rôles système non supprimables ; rôles personnalisés possibles par tenant.
- Vérification d'autorisation centralisée via `@PreAuthorize` + un `PermissionEvaluator`.

**Événements**
- `RoleAffecte` / `PermissionModifiee` → invalidation cache d'autorisation + audit.

---

