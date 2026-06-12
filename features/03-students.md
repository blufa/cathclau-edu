> **Contexte :** ce fichier fait partie des specs de la plateforme SaaS scolaire. À lire avec `CLAUDE.md` (modèle de données + architecture Spring Modulith / hexagonale) et `features/00-conventions.md` (conventions transverses : format API, codes HTTP, sécurité).
> Implémente **TOUT** ce fichier : tous les endpoints, toutes les règles métier, tous les événements. Ne rien omettre.

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

