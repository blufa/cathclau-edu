> **Contexte :** ce fichier fait partie des specs de la plateforme SaaS scolaire. À lire avec `CLAUDE.md` (modèle de données + architecture Spring Modulith / hexagonale) et `features/00-conventions.md` (conventions transverses : format API, codes HTTP, sécurité).
> Implémente **TOUT** ce fichier : tous les endpoints, toutes les règles métier, tous les événements. Ne rien omettre.

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

