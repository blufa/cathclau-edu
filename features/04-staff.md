> **Contexte :** ce fichier fait partie des specs de la plateforme SaaS scolaire. À lire avec `CLAUDE.md` (modèle de données + architecture Spring Modulith / hexagonale) et `features/00-conventions.md` (conventions transverses : format API, codes HTTP, sécurité).
> Implémente **TOUT** ce fichier : tous les endpoints, toutes les règles métier, tous les événements. Ne rien omettre.

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

