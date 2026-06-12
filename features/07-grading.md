> **Contexte :** ce fichier fait partie des specs de la plateforme SaaS scolaire. À lire avec `CLAUDE.md` (modèle de données + architecture Spring Modulith / hexagonale) et `features/00-conventions.md` (conventions transverses : format API, codes HTTP, sécurité).
> Implémente **TOUT** ce fichier : tous les endpoints, toutes les règles métier, tous les événements. Ne rien omettre.

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

