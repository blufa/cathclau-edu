# Spécifications fonctionnelles complètes — Plateforme SaaS de Gestion d'Établissements Scolaires

> **Destiné à Claude Code.** Ce document liste **toutes** les fonctionnalités à implémenter, module par module, en complément du `CLAUDE.md` (modèle de données + architecture Spring Modulith / hexagonale déjà définis). Pour chaque fonctionnalité : endpoints REST, règles métier, événements applicatifs.
>
> **Légende**
> - 🟢 **CORE** : fonctionnalité indispensable, déjà cadrée par le modèle de données.
> - 🟡 **TRANSVERSE** : fonctionnalité transverse (dashboards, exports, audit, etc.) à implémenter.
> - 🔵 **BONUS** : proposition d'amélioration pertinente, non présente dans le CLAUDE.md. À implémenter selon priorité.
>
> **Rappels d'architecture (voir CLAUDE.md)**
> - Tout endpoint hors plateforme est **tenant-scopé** : `TenantContext` (ThreadLocal) + filtre Hibernate sur `tenant_id`.
> - Communication inter-modules **uniquement** via API publique de module ou événements applicatifs Spring.
> - Domaine pur (zéro JPA/Spring) ; adaptateurs REST en `adapters/in/web`, persistance en `adapters/out/persistence`.
> - RBAC : `SUPER_ADMIN` au niveau plateforme ; autres rôles au niveau tenant.

---

## Conventions transverses (à appliquer partout)

### Format des réponses API

- Préfixe global : `/api/v1`.
- Ressources tenant-scopées : pas besoin du `tenant_id` dans l'URL (résolu via `TenantContext`). Les endpoints plateforme (`SUPER_ADMIN`) utilisent `/api/v1/platform/...`.
- Pagination standard sur toutes les collections : `?page=0&size=20&sort=champ,asc`. Réponse enveloppée `{ content, page, size, totalElements, totalPages }`.
- Filtres : query params explicites (`?statut=ACTIF&classeId=12`).
- Erreurs : format RFC 7807 (`application/problem+json`) avec `type`, `title`, `status`, `detail`, `instance`, `errors[]` pour la validation.
- Dates en ISO-8601, montants en `BigDecimal` sérialisés en string, devise implicite tenant.

### Codes HTTP

`200` lecture/maj, `201` création (+ `Location`), `204` suppression, `400` validation, `401` non authentifié, `403` permission/quota/tenant, `404` introuvable, `409` conflit (doublon, état incohérent), `422` règle métier violée, `429` rate limit.

### Sécurité par défaut

- Chaque endpoint déclare la **permission** requise (`@PreAuthorize("hasAuthority('...')")`).
- Toute écriture est journalisée dans l'`AuditLog` (voir §13).
- Validation des entrées via `spring-boot-starter-validation` (Bean Validation).

---

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

# 3. Module `students` — Élèves & Parents 🟢

## 3.1 Gestion des élèves

**Endpoints**
- `POST   /api/v1/eleves` — créer (déclenche contrôle de quota).
- `GET    /api/v1/eleves` — lister (filtres : classe, statut inscription, sexe, recherche nom/matricule).
- `GET    /api/v1/eleves/{id}` — fiche complète (inscriptions, parents, documents).
- `PUT    /api/v1/eleves/{id}` — modifier.
- `PATCH  /api/v1/eleves/{id}/photo` — upload photo.
- `DELETE /api/v1/eleves/{id}` — soft delete (interdit si notes/paiements existants → archiver).

**Règles métier**
- `matricule` unique par tenant, généré automatiquement (format paramétrable, ex. `ANNEE-SEQ`).
- Avant création : `QuotaService.verifierQuotaEleves(tenant)` (voir §1.3). Refus `403` si dépassement.
- Un élève peut avoir un compte `Utilisateur` (rôle `ELEVE`) — création optionnelle.

**Événements**
- `EleveCree` → audit, incrément compteur quota.

## 3.2 Inscriptions / réinscriptions

**Endpoints**
- `POST   /api/v1/inscriptions` — inscrire un élève dans une classe pour une année.
- `GET    /api/v1/inscriptions` — filtres élève, classe, année, statut.
- `PATCH  /api/v1/inscriptions/{id}/valider` / `/annuler`.
- `POST   /api/v1/inscriptions/reinscription` — réinscrire (nouvelle année).

**Règles métier**
- `type` : `INSCRIPTION` (première) ou `REINSCRIPTION` (année suivante).
- `statut` : `EN_ATTENTE → (VALIDEE | ANNULEE)`.
- Une inscription est rattachée à `classe_id` + `annee_scolaire_id` (jamais champ texte).
- Refus si la classe a atteint `capacite_max`.
- Une seule inscription `VALIDEE` par élève et par année.

**Événements**
- `InscriptionValidee` → `billing` crée automatiquement la facture de frais d'inscription ; `academics` met à jour l'`effectif` de la classe.

## 3.3 Documents administratifs de l'élève

**Endpoints**
- `POST   /api/v1/eleves/{id}/documents` — uploader (acte de naissance, photo, certificat médical, historique).
- `GET    /api/v1/eleves/{id}/documents`.
- `DELETE /api/v1/eleves/{id}/documents/{docId}`.

**Règles métier**
- `type_document` enum ; fichiers stockés sur object storage, URL persistée.
- Validation du type MIME et de la taille.

## 3.4 Parents & liens de parenté

**Endpoints**
- `POST   /api/v1/parents` / `GET` / `GET /{id}` / `PUT /{id}`.
- `POST   /api/v1/eleves/{eleveId}/parents` — lier un parent (`ELEVE_PARENT`, lien PERE/MERE/TUTEUR).
- `DELETE /api/v1/eleves/{eleveId}/parents/{parentId}`.
- `GET    /api/v1/parents/{id}/enfants` — élèves rattachés.

**Règles métier**
- Un parent peut être lié à plusieurs élèves ; un élève à plusieurs parents/tuteurs.
- Compte `Utilisateur` (rôle `PARENT`) optionnel pour l'accès au portail parent.
- Les notifications (absences, bulletins, échéances) ciblent les parents liés.


---

# 4. Module `staff` — Enseignants & Personnel 🟢

## 4.1 Gestion des enseignants

**Endpoints**
- `POST   /api/v1/enseignants` / `GET` / `GET /{id}` / `PUT /{id}` / `DELETE /{id}`.
- `GET    /api/v1/enseignants/{id}/classes` — classes dont il est responsable.
- `GET    /api/v1/enseignants/{id}/matieres` — affectations `ClasseMatiere`.
- `GET    /api/v1/enseignants/{id}/emploi-du-temps` — planning consolidé.

**Règles métier**
- `matricule` unique par tenant.
- Compte `Utilisateur` (rôle `ENSEIGNANT`) optionnel.
- Suppression interdite si l'enseignant est lié à des notes/évaluations → désactivation.
- `specialite` informatif ; pas de blocage d'affectation hors spécialité (paramétrable en bonus).

**Événements**
- `EnseignantCree` → audit.

---

# 5. Module `academics` — Calendrier, Classes & Organisation 🟢

## 5.1 Années scolaires

**Endpoints**
- `POST   /api/v1/annees-scolaires` / `GET` / `GET /{id}` / `PUT /{id}`.
- `PATCH  /api/v1/annees-scolaires/{id}/statut` — PLANIFIEE → EN_COURS → CLOTUREE → ARCHIVEE.
- `PATCH  /api/v1/annees-scolaires/{id}/definir-courante`.

**Règles métier**
- **Une seule** `AnneeScolaire` avec `courante = true` par tenant (transaction qui bascule l'ancienne).
- L'archivage rend les données **en lecture seule** (inscriptions, notes, bulletins, paiements conservés).
- Transitions de statut strictes ; clôture interdite s'il reste des périodes non clôturées.

**Événements**
- `AnneeScolaireClôturee` → fige les notes ; `AnneeScolaireArchivee` → bascule lecture seule.

## 5.2 Périodes (trimestres / semestres)

**Endpoints**
- `POST   /api/v1/annees-scolaires/{anneeId}/periodes` / `GET` / `PUT /{id}`.
- `PATCH  /api/v1/periodes/{id}/cloturer`.

**Règles métier**
- `type` TRIMESTRE ou SEMESTRE, `ordre` séquentiel, dates incluses dans l'année parente.
- La clôture d'une période **fige les notes** et déclenche la génération des bulletins (voir §7).
- Saisie de notes interdite sur période clôturée ou année non EN_COURS.

**Événements**
- `PeriodeClôturee` → `grading` génère les bulletins de la période.

## 5.3 Niveaux & salles

**Endpoints**
- `POST   /api/v1/niveaux` / `GET` / `PUT /{id}` / `DELETE /{id}`.
- `POST   /api/v1/salles` / `GET` / `PUT /{id}` / `PATCH /{id}/disponibilite`.

**Règles métier**
- `Niveau.ordre` définit la progression (passage de classe).
- `Salle` : libellé, bâtiment, capacité, type, disponibilité. Référencée par `Classe` et `EmploiDuTemps` pour détecter les conflits d'occupation.

## 5.4 Classes

**Endpoints**
- `POST   /api/v1/classes` / `GET` / `GET /{id}` / `PUT /{id}` / `DELETE /{id}`.
- `GET    /api/v1/classes/{id}/eleves` — via inscriptions validées.
- `GET    /api/v1/classes/{id}/effectif` — effectif courant vs capacité.

**Règles métier**
- Rattachée à `niveau_id`, `annee_scolaire_id`, `enseignant_resp_id` (prof principal), `salle_id`.
- `effectif` recalculé automatiquement à chaque inscription validée/annulée.
- Création interdite si l'année est CLOTUREE/ARCHIVEE.

## 5.5 Matières & affectations

**Endpoints**
- `POST   /api/v1/matieres` / `GET` / `PUT /{id}` / `DELETE /{id}`.
- `POST   /api/v1/classes/{classeId}/matieres` — affecter matière + enseignant (`ClasseMatiere`).
- `GET    /api/v1/classes/{classeId}/matieres`.
- `DELETE /api/v1/classes/{classeId}/matieres/{id}`.

**Règles métier**
- `Matiere.coefficient` utilisé dans le calcul des moyennes (§7).
- `ClasseMatiere` lie une classe, une matière et l'enseignant qui l'enseigne.

## 5.6 Emploi du temps

**Endpoints**
- `POST   /api/v1/emplois-du-temps` — créer un créneau (classe, matière, enseignant, salle, jour, heures).
- `GET    /api/v1/emplois-du-temps` — filtres classe, enseignant, salle, jour.
- `GET    /api/v1/classes/{id}/emploi-du-temps` / `GET /api/v1/enseignants/{id}/emploi-du-temps`.
- `PUT    /api/v1/emplois-du-temps/{id}` / `DELETE /{id}`.

**Règles métier**
- **Détection de conflits** (refus `409`) : même enseignant, même salle ou même classe sur créneaux qui se chevauchent le même jour.
- `heure_debut < heure_fin` ; bornes dans les horaires d'ouverture (paramétrable).

**Événements**
- `EmploiDuTempsModifie` → 🔵 notification optionnelle aux concernés.

---

# 6. Module `attendance` — Présences 🟢

## 6.1 Présence des élèves

**Endpoints**
- `POST   /api/v1/presences-eleves` — pointer (élève, classe, date, statut, motif).
- `POST   /api/v1/presences-eleves/lot` — saisie en masse pour une classe/date (appel de classe).
- `GET    /api/v1/presences-eleves` — filtres élève, classe, date, statut.
- `PUT    /api/v1/presences-eleves/{id}` — corriger.
- `GET    /api/v1/eleves/{id}/presences/statistiques` — taux d'assiduité par période.

**Règles métier**
- `statut` : PRESENT, ABSENT, RETARD ; `motif` libre.
- Une présence par élève, classe et date (contrainte d'unicité).
- `notifie_parent` passe à true après l'envoi de la notification.

**Événements**
- `AbsenceEnregistree` (statut ABSENT/RETARD) → `communication` notifie le(s) parent(s) ; positionne `notifie_parent`.

## 6.2 Présence des enseignants

**Endpoints**
- `POST   /api/v1/presences-enseignants` / `GET` / `PUT /{id}`.
- `GET    /api/v1/enseignants/{id}/presences/statistiques`.

**Règles métier**
- Table **séparée** des présences élèves (jamais fusionner).
- `statut` : PRESENT, ABSENT ; usage RH/suivi.


---

# 7. Module `grading` — Notes, Examens & Bulletins 🟢

## 7.1 Évaluations

**Endpoints**
- `POST   /api/v1/evaluations` — créer (classe, matière, enseignant, période, type, note_sur, coefficient).
- `GET    /api/v1/evaluations` — filtres classe, matière, période, enseignant, type.
- `PUT    /api/v1/evaluations/{id}` / `DELETE /{id}`.

**Règles métier**
- `type` : INTERROGATION, DEVOIR, COMPOSITION, EXAMEN ; `note_sur` (ex. 20), `coefficient` de l'évaluation.
- Création/modification interdite si la période est clôturée.
- L'enseignant ne peut créer que sur les classes/matières qu'il enseigne (`ClasseMatiere`).

## 7.2 Saisie des notes

**Endpoints**
- `POST   /api/v1/evaluations/{id}/notes` — saisir une note (élève, valeur).
- `POST   /api/v1/evaluations/{id}/notes/lot` — saisie en masse pour toute la classe.
- `PUT    /api/v1/notes/{id}` — corriger (si non validée / période ouverte).
- `PATCH  /api/v1/evaluations/{id}/notes/valider` — valider en bloc (verrouille).
- `GET    /api/v1/eleves/{id}/notes` — filtres période, matière.

**Règles métier**
- `0 <= valeur <= note_sur` ; refus sinon (`422`).
- Saisie uniquement sur période non clôturée d'une année EN_COURS.
- `validee = true` verrouille la note ; correction nécessite déverrouillage par un rôle autorisé.

**Événements**
- `NotesValidees` → recalcul possible des moyennes en cache.

## 7.3 Examens

**Endpoints**
- `POST   /api/v1/examens` — planifier (classe, période, type, date, convocation).
- `GET    /api/v1/examens` — filtres classe, période, statut.
- `PATCH  /api/v1/examens/{id}/statut` — PLANIFIE → EN_COURS → TERMINE.
- `GET    /api/v1/examens/{id}/convocation` — PDF de convocation.

**Règles métier**
- `type` : BLANC, SEMESTRIEL, ANNUEL, NATIONAL.
- Génération de la convocation (PDF) asynchrone.

## 7.4 Bulletins & calcul des moyennes

**Endpoints**
- `POST   /api/v1/bulletins/generer` — générer pour une classe + période (lot).
- `GET    /api/v1/bulletins` — filtres élève, classe, période, statut.
- `GET    /api/v1/bulletins/{id}` — en-tête + lignes.
- `PATCH  /api/v1/bulletins/{id}/publier` — BROUILLON/GENERE → PUBLIE.
- `GET    /api/v1/bulletins/{id}/pdf`.
- `GET    /api/v1/eleves/{id}/bulletins`.

**Règles métier (calcul, voir CLAUDE.md §grading)**
- Pour chaque matière : `moyenne_matiere` = moyenne des notes pondérée par le **coefficient d'évaluation**.
- `moyenne_ponderee` (ligne) = `moyenne_matiere × coefficient_matiere`.
- `moyenne_generale` = Σ(moyennes pondérées de matière) / Σ(coefficients de matière).
- `rang` calculé **par classe et par période** ; `rang_matiere` par matière.
- Statistiques de classe : `moyenne_classe`, `moyenne_plus_forte`, `moyenne_plus_faible`, `effectif_classe`.
- `mention` et `decision` dérivées de la moyenne générale (barème paramétrable par tenant).
- Génération uniquement après clôture de période ; PDF asynchrone.
- Un bulletin `PUBLIE` est visible par l'élève/parent.

**Événements**
- `PeriodeClôturee` (reçu de `academics`) → génération automatique des bulletins.
- `BulletinGenere` / `BulletinPublie` → `communication` notifie le parent.

---

# 8. Module `billing` — Frais Scolaires & Facturation 🟢

> Flux financier **école ↔ parents**, distinct de l'abonnement SaaS (§1).

## 8.1 Paramétrage des frais

**Endpoints**
- `POST   /api/v1/frais-scolaires` — définir un frais (classe, type, montant, année).
- `GET    /api/v1/frais-scolaires` — filtres classe, type, année.
- `PUT    /api/v1/frais-scolaires/{id}` / `DELETE /{id}`.

**Règles métier**
- `type` : INSCRIPTION, REINSCRIPTION, MENSUALITE, TRANSPORT, CANTINE, EXAMEN.
- Frais rattaché à une classe (donc à un niveau/année).

## 8.2 Échéances

**Endpoints**
- `POST   /api/v1/frais-scolaires/{fraisId}/echeances` — échelonner (libellé, montant, date limite).
- `GET    /api/v1/frais-scolaires/{fraisId}/echeances` / `PUT /{id}` / `DELETE /{id}`.

**Règles métier**
- La somme des échéances doit correspondre au montant total du frais (contrôle, `422` sinon).
- Une échéance dépassée non payée déclenche une relance.

**Événements**
- `EcheanceDepassee` (job planifié) → `communication` relance le parent.

## 8.3 Factures de frais

**Endpoints**
- `POST   /api/v1/factures` — émettre (élève, frais).
- `POST   /api/v1/factures/generer-lot` — générer pour toute une classe.
- `GET    /api/v1/factures` — filtres élève, classe, statut, période.
- `GET    /api/v1/factures/{id}` / `GET /api/v1/factures/{id}/pdf`.
- `GET    /api/v1/eleves/{id}/factures` — situation financière de l'élève.

**Règles métier**
- `statut` : IMPAYEE → PARTIELLE → PAYEE (dérivé du `montant_paye` vs `montant_total`).
- `solde_restant = montant_total - montant_paye` recalculé à chaque paiement.
- `reference` unique séquentielle par tenant.
- Génération automatique sur `InscriptionValidee` (frais d'inscription).

**Événements**
- `FactureEmise` → 🔵 notification au parent.
- `FacturePayee` → clôture, reçu généré (via `payments`).

---

# 9. Module `payments` — Paiements & Encaissements 🟢

## 9.1 Initiation et suivi des paiements

**Endpoints**
- `POST   /api/v1/paiements` — initier (facture, élève, montant, canal, type partiel/complet).
- `GET    /api/v1/paiements` — filtres élève, facture, canal, statut, date.
- `GET    /api/v1/paiements/{id}` — statut + transaction associée.
- `POST   /api/v1/paiements/{id}/rembourser` — remboursement (rôle COMPTABLE).
- `GET    /api/v1/paiements/{id}/recu/pdf`.

**Règles métier**
- `canal` : ORANGE_MONEY, MTN_MONEY, WAVE, CARTE_BANCAIRE.
- `type_paiement` : PARTIEL ou COMPLET ; un paiement partiel met la facture en `PARTIELLE`.
- Machine à états : `INITIE → EN_ATTENTE → (REUSSI | ECHOUE | REMBOURSE)`.
- Flux complet : `Facture → Paiement → TransactionPaiement (appel fournisseur) → PaymentWebhook (callback) → maj statut → Recu`.

**Événements**
- `PaiementReussi` → `payments` génère `Recu` ; `billing` met à jour la `Facture` ; `communication` notifie (WhatsApp/SMS).
- `PaiementEchoue` → `communication` informe, facture inchangée.
- `PaiementRembourse` → ajustement facture + reçu d'avoir.

## 9.2 Transactions fournisseurs

**Endpoints**
- `GET    /api/v1/transactions-paiement` — filtres fournisseur, statut, date (rôle COMPTABLE/ADMIN).
- `GET    /api/v1/transactions-paiement/{id}` — détail + `payload_reponse`.

**Règles métier**
- `fournisseur` : ORANGE_MONEY, MTN_MONEY, WAVE, STRIPE.
- `TransactionPaiement` trace chaque appel sortant ; `payload_reponse` stocke la réponse brute.
- Adaptateurs hexagonaux distincts (`OrangeMoneyAdapter`, `MtnMoneyAdapter`, `WaveAdapter`, `StripeAdapter`) derrière le port `FournisseurPaiementGateway`.

## 9.3 Reçus

**Endpoints**
- `GET    /api/v1/recus` — filtres élève, date.
- `GET    /api/v1/recus/{id}/pdf`.

**Règles métier**
- Un `Recu` (relation 1-1 avec `Paiement`) généré uniquement au paiement `REUSSI`.
- PDF asynchrone, URL persistée, envoi par email/WhatsApp.


---

# 10. Module `webhooks` — Intégrations entrantes & sortantes 🟢

## 10.1 Webhooks entrants (callbacks de paiement)

**Endpoints**
- `POST   /api/v1/webhooks/paiement/{fournisseur}` — endpoint public de callback (Orange/MTN/Wave/Stripe).

**Règles métier**
- **Vérification de signature OBLIGATOIRE** avant tout traitement (`PaymentWebhook.signature`, `verifie`).
- Idempotence : un même `evenement` fournisseur n'est traité qu'une fois (`traite = true`).
- Stocke le `payload_brut`, met à jour la `TransactionPaiement` puis le `Paiement` correspondant.
- Endpoint **hors authentification JWT** mais protégé par la signature ; résolution du tenant via la transaction.

**Événements**
- Après traitement réussi → `PaiementReussi` / `PaiementEchoue` (voir §9).

## 10.2 Webhooks sortants (notifications vers tiers)

**Endpoints**
- `POST   /api/v1/webhook-endpoints` — configurer (url, secret, événements abonnés).
- `GET    /api/v1/webhook-endpoints` / `PUT /{id}` / `PATCH /{id}/activer` / `DELETE /{id}`.
- `GET    /api/v1/webhook-events` — journal des envois (filtres statut, endpoint).
- `POST   /api/v1/webhook-events/{id}/rejouer` — renvoyer manuellement.

**Règles métier**
- `type_evenement` : PAIEMENT_RECU, BULLETIN_DISPONIBLE, ABSENCE, INSCRIPTION.
- **Retry exponentiel** : `nb_tentatives`, `prochaine_tentative`, statut `EN_ATTENTE → ENVOYE | ECHOUE`, code de réponse stocké. Job planifié qui dépile les events dus.
- Signature HMAC sortante avec le `secret` de l'endpoint.
- Souscription par type : un endpoint ne reçoit que les événements de sa liste `evenements`.

**Événements**
- Écoute les événements applicatifs internes (`PaiementReussi`, `BulletinPublie`, `AbsenceEnregistree`, `InscriptionValidee`) et crée les `WebhookEvent` correspondants pour les endpoints abonnés.

---

# 11. Module `communication` — Notifications SMS / Email / WhatsApp 🟢

## 11.1 Envoi de communications

**Endpoints**
- `POST   /api/v1/communications` — envoyer (canal, type destinataire, sujet, contenu, motif).
- `POST   /api/v1/communications/diffusion` — diffusion de masse (à une classe, un niveau, un groupe).
- `GET    /api/v1/communications` — historique (filtres canal, motif, date, expéditeur).
- `GET    /api/v1/communications/{id}` — détail + statuts de livraison par destinataire.

**Règles métier**
- `canal` : SMS, EMAIL, WHATSAPP ; `motif` : RETARD, ABSENCE, ECHEANCE_PAIEMENT, RESULTAT, REUNION, GENERAL.
- Chaque envoi crée N `MessageDestinataire` avec `statut_livraison` (EN_ATTENTE → LIVRE | ECHOUE), `lu`, `livre_le`.
- Adaptateurs hexagonaux par canal derrière un port `NotificationGateway` (ex. Twilio/SMS, SMTP, WhatsApp Business API).
- Envoi asynchrone + mise à jour du statut via callback du fournisseur si disponible.

**Événements consommés (déclenchent des envois automatiques)**
- `AbsenceEnregistree` → notification parent (motif ABSENCE/RETARD).
- `BulletinPublie` → notification parent (motif RESULTAT).
- `PaiementReussi` → confirmation parent.
- `EcheanceDepassee` → relance paiement (motif ECHEANCE_PAIEMENT).
- `AbonnementBientotExpire` → relance admin tenant.

## 11.2 Modèles de message (templates) 🔵 BONUS

**Endpoints**
- `POST   /api/v1/templates-message` / `GET` / `PUT /{id}` / `DELETE /{id}`.

**Règles métier (bonus)**
- Templates par `motif` avec variables (`{{nom_eleve}}`, `{{montant}}`, `{{moyenne}}`...).
- Permet de standardiser les notifications automatiques. Nécessite une table `TemplateMessage` (tenant-scopée).


---

# 12. Tableaux de bord & statistiques 🟡 TRANSVERSE

> Pas de nouvelles tables nécessaires (agrégations sur l'existant). Endpoints de lecture seule, fortement cachés.

## 12.1 Dashboard plateforme (SUPER_ADMIN)

- `GET /api/v1/platform/dashboard` — nb tenants (actifs/suspendus), MRR/ARR, répartition par plan, taux de churn, abonnements à échéance, paiements d'abonnement du mois.

## 12.2 Dashboard école (ADMIN_ECOLE / DIRECTEUR)

- `GET /api/v1/dashboard` — effectifs (total, par niveau/classe), taux d'assiduité global, taux de recouvrement des frais (payé vs attendu), nb bulletins publiés, alertes (factures impayées, quota proche, échéances).

## 12.3 Dashboard comptable

- `GET /api/v1/dashboard/finance` — encaissements par canal/période, soldes restants, top débiteurs, échéances à venir.

## 12.4 Dashboard enseignant

- `GET /api/v1/dashboard/enseignant` — mes classes, prochains cours (emploi du temps), évaluations à corriger, taux de saisie des notes.

## 12.5 Dashboard parent / élève

- `GET /api/v1/dashboard/parent` — enfants, dernières notes, absences, factures impayées, prochains examens.

**Règles métier**
- Chaque dashboard filtré par rôle + tenant + portée (un parent ne voit que ses enfants).
- Agrégations calculées à la volée ou via vues matérialisées (perf).

---

# 13. Audit, journalisation & sécurité 🟡 TRANSVERSE

> Nécessite une table `AuditLog` (tenant-scopée, sauf actions plateforme) — **à ajouter au modèle**.

**Champs `AuditLog`** : `id, tenant_id, utilisateur_id, action, ressource, ressource_id, details (Text/JSON), adresse_ip, user_agent, created_at`.

**Endpoints**
- `GET /api/v1/audit-logs` — filtres utilisateur, action, ressource, période (rôle ADMIN/DIRECTEUR).
- `GET /api/v1/platform/audit-logs` — niveau plateforme (SUPER_ADMIN).

**Règles métier**
- Journaliser **toute écriture sensible** : auth (login/échec/reset), création/suspension de tenant, changements de rôles/permissions, paiements, remboursements, publication de bulletins, suppression de données.
- Logs immuables (append-only), jamais modifiables ni supprimables via l'API.
- Rétention paramétrable ; export possible (§14).
- 🔵 **BONUS** : intégration d'un mécanisme de détection d'anomalies (connexions inhabituelles).

---

# 14. Imports / Exports & génération documentaire 🟡 TRANSVERSE

## 14.1 Imports en masse

**Endpoints**
- `POST /api/v1/imports/eleves` — import CSV/Excel d'élèves (avec validation et rapport d'erreurs ligne par ligne).
- `POST /api/v1/imports/notes` — import des notes d'une évaluation.
- `POST /api/v1/imports/enseignants`.

**Règles métier**
- Validation préalable (dry-run) renvoyant les lignes invalides avant commit.
- Respect du quota d'élèves à l'import.
- Traitement asynchrone pour les gros fichiers + rapport téléchargeable.

## 14.2 Exports

**Endpoints**
- `GET /api/v1/exports/eleves?format=csv|xlsx` — liste d'élèves filtrée.
- `GET /api/v1/exports/notes`, `/api/v1/exports/paiements`, `/api/v1/exports/presences`.
- `GET /api/v1/exports/bulletins?classeId=&periodeId=` — archive ZIP des PDF.

## 14.3 Génération de documents PDF (module dédié `documents` ou service partagé)

**Documents** : bulletins, reçus (scolaires + abonnement), factures, convocations d'examen, certificats de scolarité 🔵, attestations 🔵.

**Règles métier**
- Génération **asynchrone** (iText/OpenPDF), stockage object storage, URL persistée sur l'entité (`fichier_pdf` / `*_url`).
- Templates avec branding du tenant (logo, couleur, en-tête).
- Idempotence : régénération possible sans dupliquer le fichier.

**Événements**
- Consomme `BulletinGenere`, `PaiementReussi`, `FactureEmise`, etc. pour déclencher la production.

---

# 15. Recherche, paramétrage & confort 🟡 TRANSVERSE

## 15.1 Recherche globale

- `GET /api/v1/recherche?q=...` — recherche transversale (élèves, parents, enseignants, classes, factures) scopée au tenant et filtrée par permissions.

## 15.2 Paramètres du tenant

- `GET /api/v1/parametres` / `PUT /api/v1/parametres` — barème de mentions, format de matricule, devise, fuseau horaire, délai de grâce abonnement, horaires d'ouverture, politique de mot de passe.
- Nécessite une table `ParametreTenant` (clé/valeur ou colonnes typées) — **à ajouter**.

## 15.3 Internationalisation (i18n)

- En-tête `Accept-Language` ; messages d'erreur et templates traduisibles (FR par défaut, EN en option).

## 15.4 Notifications in-app 🔵 BONUS

- Table `Notification` (utilisateur, type, contenu, lu, lien) + endpoints `GET /api/v1/notifications`, `PATCH /{id}/lu`. Cloche de notifications dans l'UI.

---

# 16. Propositions bonus (à prioriser) 🔵

| # | Fonctionnalité | Valeur ajoutée | Impact modèle |
|---|----------------|----------------|---------------|
| B1 | **Portail parent/élève** dédié (lecture notes, absences, factures, paiement en ligne) | Engagement familles, réduction charge secrétariat | Aucun (réutilise l'existant) |
| B2 | **Passage de classe en masse** (fin d'année → réinscription auto au niveau supérieur) | Gain de temps énorme en fin d'année | Aucun |
| B3 | **Paiement en ligne self-service** par le parent (lien de paiement) | Améliore le recouvrement | Aucun |
| B4 | **Rappels automatiques d'échéances** (J-3, J, J+3) configurables | Réduit les impayés | Réutilise `communication` |
| B5 | **Templates de messages** (§11.2) | Cohérence des notifications | Table `TemplateMessage` |
| B6 | **Paramètres tenant** (§15.2) | Personnalisation par école | Table `ParametreTenant` |
| B7 | **Audit log** (§13) | Conformité, sécurité, traçabilité | Table `AuditLog` |
| B8 | **Notifications in-app** (§15.4) | UX moderne | Table `Notification` |
| B9 | **Gestion des bulletins multi-périodes** (bulletin annuel cumulant les périodes) | Vue d'ensemble | Aucun (agrégation) |
| B10 | **Statistiques comparatives inter-années** | Pilotage pédagogique | Aucun (les années sont des entités) |
| B11 | **Certificats de scolarité / attestations** générés à la demande | Service aux familles | Réutilise génération PDF |
| B12 | **Détection de conflits de salle/enseignant** déjà cadrée (§5.6) — étendre à un calendrier visuel | Confort planification | Aucun |
| B13 | **Soft delete + corbeille** restaurable sur entités sensibles | Sécurité des données | Champ `deleted_at` |
| B14 | **Rate limiting & protection anti-abus** sur auth et webhooks | Sécurité | Aucun |
| B15 | **Health checks & métriques** (Actuator + Micrometer/Prometheus) | Exploitation | Aucun |

---

# 17. Checklist d'implémentation par module

> À cocher par Claude Code. Chaque module = domaine pur + ports + adaptateurs (in/web, out/persistence, out/gateway) + `package-info` `@ApplicationModule` + tests (domaine, application, adapters) + migration Flyway.

- [ ] `platform` — Tenants, Plans, Abonnements, FactureAbonnement, PaiementAbonnement, RecuAbonnement, ChangementPlan, QuotaService, dashboard plateforme.
- [ ] `iam` — Auth (JWT/refresh/reset), Utilisateurs, Rôles, Permissions, RBAC, `TenantContext` + filtre Hibernate multi-tenant, audit auth.
- [ ] `students` — Élèves (+quota), Inscriptions/réinscriptions, Documents, Parents, liens de parenté.
- [ ] `staff` — Enseignants + vues classes/matières/emploi du temps.
- [ ] `academics` — Années, Périodes (clôture→bulletins), Niveaux, Salles (conflits), Classes (effectif), Matières, ClasseMatiere, Emploi du temps (conflits).
- [ ] `attendance` — Présences élèves (lot, notif parent) + présences enseignants (séparées).
- [ ] `grading` — Évaluations, Notes (saisie/validation), Examens, Bulletins (calcul moyennes/rang/mention, génération sur clôture période), LigneBulletin.
- [ ] `billing` — FraisScolaire, Échéances, Factures (génération sur inscription, statuts).
- [ ] `payments` — Paiements (machine à états), TransactionPaiement (adaptateurs Orange/MTN/Wave/Stripe), Reçus, remboursements.
- [ ] `webhooks` — Entrants (vérif signature, idempotence) + Sortants (retry exponentiel, souscription par type, rejeu).
- [ ] `communication` — SMS/Email/WhatsApp (adaptateurs), diffusion de masse, statuts de livraison, écoute des événements métier, templates (bonus).
- [ ] **Transverse** — Dashboards (§12), AuditLog (§13), Import/Export + PDF (§14), Recherche/Paramètres/i18n (§15).
- [ ] **Tests Modulith** — `ApplicationModules.of(...).verify()` + `Documenter` ; Testcontainers PostgreSQL ; couverture domaine/application/adapters.

---

# 18. Récapitulatif des événements applicatifs (contrat inter-modules)

| Événement | Émis par | Consommé par | Effet |
|-----------|----------|--------------|-------|
| `TenantCree` | platform | iam, communication | provisionne admin/rôles, email bienvenue |
| `AbonnementActive` / `Suspendu` | platform | iam | débloque / bloque l'accès tenant |
| `AbonnementBientotExpire` | platform (job) | communication | relance admin |
| `PlanChange` | platform | platform (quota) | recalcul quota |
| `UtilisateurConnecte` | iam | audit | trace + `derniere_connexion` |
| `InscriptionValidee` | students | billing, academics | facture inscription + effectif classe |
| `PeriodeClôturee` | academics | grading | génération bulletins |
| `AbsenceEnregistree` | attendance | communication, webhooks | notif parent + webhook sortant |
| `NotesValidees` | grading | grading | recalcul moyennes |
| `BulletinGenere` | grading | documents, communication | PDF + notif parent |
| `BulletinPublie` | grading | communication, webhooks | notif parent + webhook |
| `FactureEmise` | billing | communication | notif parent |
| `EcheanceDepassee` | billing (job) | communication | relance paiement |
| `PaiementReussi` | payments | billing, payments, communication, webhooks | maj facture, reçu, notif, webhook |
| `PaiementEchoue` | payments | communication | information échec |
| `PaiementRembourse` | payments | billing | ajustement facture |
| `PaiementAbonnementReussi` | platform | platform, communication | activation + reçu |

---

*Fin du document — toutes les fonctionnalités des 11 modules métier + transverses + bonus sont couvertes, avec endpoints, règles métier et événements. Les éléments marqués 🔵 BONUS et les tables additionnelles (AuditLog, ParametreTenant, TemplateMessage, Notification) sont à valider avant implémentation.*