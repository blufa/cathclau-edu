> **Contexte :** ce fichier fait partie des specs de la plateforme SaaS scolaire. À lire avec `CLAUDE.md` (modèle de données + architecture Spring Modulith / hexagonale) et `features/00-conventions.md` (conventions transverses : format API, codes HTTP, sécurité).
> Implémente **TOUT** ce fichier : tous les endpoints, toutes les règles métier, tous les événements. Ne rien omettre.

# 1. Module `platform` — Plateforme & Abonnements 🟢

> Niveau éditeur de la plateforme. Accès `SUPER_ADMIN` uniquement (sauf consultation de son propre abonnement par l'`ADMIN_ECOLE`). **Ne jamais mélanger** ces tables avec les frais scolaires (`billing`/`payments`).

## 1.1 Gestion des tenants (écoles)

**Endpoints**
- `POST   /api/v1/platform/tenants` — créer une école (onboarding).
- `GET    /api/v1/platform/tenants` — lister/rechercher (filtres : statut, ville, plan).
- `GET    /api/v1/platform/tenants/{id}` — détail + abonnement courant + quota consommé.
- `PUT    /api/v1/platform/tenants/{id}` — mettre à jour (nom, branding, coordonnées, directeur).
- `PATCH  /api/v1/platform/tenants/{id}/statut` — activer / suspendre / archiver.
- `DELETE /api/v1/platform/tenants/{id}` — suppression logique (soft delete, jamais physique).

**Règles métier**
- La création d'un tenant crée automatiquement : 1 `Utilisateur` `ADMIN_ECOLE`, les rôles par défaut, les permissions standard, une `AnneeScolaire` courante optionnelle.
- `statut = SUSPENDU` bloque l'accès de **tous** les utilisateurs du tenant (filtre de sécurité global) sans supprimer les données.
- Le branding (logo, couleur) est exposé en lecture publique pour la page de login du tenant.

**Événements**
- `TenantCree` → `iam` provisionne l'admin + rôles ; `communication` envoie l'email de bienvenue.
- `TenantSuspendu` / `TenantReactive` → invalidation des sessions actives.

## 1.2 Catalogue des plans

**Endpoints**
- `GET    /api/v1/platform/plans` — liste publique (page tarifs).
- `POST   /api/v1/platform/plans` — créer (SUPER_ADMIN).
- `PUT    /api/v1/platform/plans/{id}` — modifier prix/limites.
- `PATCH  /api/v1/platform/plans/{id}/archiver` — désactiver un plan (conserve les abonnements existants).

**Règles métier**
- Plans : `STARTER` (≤500 élèves), `STANDARD` (≤2000), `PREMIUM` (illimité).
- Un plan archivé n'est plus souscriptible mais reste valide pour les abonnements en cours.
- Modifier le prix n'affecte pas les abonnements actifs avant renouvellement.

## 1.3 Cycle de vie de l'abonnement

**Endpoints**
- `POST   /api/v1/platform/abonnements` — souscrire (tenant + plan + périodicité).
- `GET    /api/v1/abonnements/courant` — l'abonnement actif du tenant connecté (ADMIN_ECOLE).
- `GET    /api/v1/platform/abonnements` — tous (SUPER_ADMIN, filtres statut/plan/échéance).
- `PATCH  /api/v1/platform/abonnements/{id}/suspendre` — suspension manuelle.
- `PATCH  /api/v1/platform/abonnements/{id}/annuler` — résiliation.
- `PATCH  /api/v1/abonnements/{id}/auto-renouvellement` — activer/désactiver.
- `POST   /api/v1/platform/abonnements/{id}/changer-plan` — upgrade/downgrade (crée `ChangementPlan`).

**Règles métier**
- Machine à états : `EN_ATTENTE → ACTIF → (SUSPENDU | EXPIRE | ANNULE)`. Transitions strictes (refuser un passage illégal en `422`).
- À la souscription : abonnement `EN_ATTENTE` + génération `FactureAbonnement` (étape 3 du cycle).
- Activation conditionnée au `PaiementAbonnement` `REUSSI`.
- À `date_fin` : si `auto_renouvellement` → régénérer facture ; sinon délai de grâce (paramétrable, ex. 7 jours) puis `SUSPENDU`.
- Changement de plan : calcul du **prorata** (`montant_proratise`) sur la période restante, ligne `ChangementPlan` (type UPGRADE/DOWNGRADE/RENOUVELLEMENT, date d'effet).
- **Contrôle de quota** : un service `QuotaService.verifierQuotaEleves(tenantId)` appelé avant toute création d'élève ; refuse en `403` si `count(eleves) >= plan.limite_eleves`.

**Événements**
- `AbonnementActive` → débloque le tenant.
- `AbonnementSuspendu` → bloque l'accès.
- `AbonnementBientotExpire` (J-7) → `communication` relance l'admin.
- `PlanChange` → recalcul du quota autorisé.

## 1.4 Facturation de l'abonnement

**Endpoints**
- `GET    /api/v1/platform/factures-abonnement` — liste (filtres statut, période, tenant).
- `GET    /api/v1/abonnements/factures` — factures du tenant connecté.
- `GET    /api/v1/factures-abonnement/{id}/pdf` — télécharger le PDF.
- `POST   /api/v1/platform/factures-abonnement/{id}/relancer` — renvoyer la relance.

**Règles métier**
- Statuts : `BROUILLON → EMISE → (PAYEE | PARTIELLE | IMPAYEE | ANNULEE)`.
- `montant_total = montant_ht + montant_tva` ; `solde_restant = total - montant_paye`.
- Génération PDF asynchrone (voir §11), URL persistée dans `fichier_pdf`.
- Numérotation `reference` unique et séquentielle par tenant éditeur.

## 1.5 Paiement de l'abonnement & reçus

**Endpoints**
- `POST   /api/v1/abonnements/paiements` — initier un paiement (canal Orange/MTN/Wave/Visa/MasterCard).
- `GET    /api/v1/abonnements/paiements/{id}` — statut.
- `GET    /api/v1/abonnements/recus/{id}/pdf` — reçu PDF.

**Règles métier**
- Flux identique aux paiements scolaires : `INITIE → EN_ATTENTE → (REUSSI | ECHOUE | REMBOURSE)`, confirmation par callback fournisseur (module `webhooks`).
- Au `REUSSI` : facture → `PAYEE`, abonnement → `ACTIF`, génération `RecuAbonnement` (PDF + email).

**Événements**
- `PaiementAbonnementReussi` → activation + reçu + notification.
- `PaiementAbonnementEchoue` → facture reste `IMPAYEE` + relance.


---

