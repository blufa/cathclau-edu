> **Contexte :** ce fichier fait partie des specs de la plateforme SaaS scolaire. À lire avec `CLAUDE.md` (modèle de données + architecture Spring Modulith / hexagonale) et `features/00-conventions.md` (conventions transverses : format API, codes HTTP, sécurité).
> Implémente **TOUT** ce fichier : tous les endpoints, toutes les règles métier, tous les événements. Ne rien omettre.

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

