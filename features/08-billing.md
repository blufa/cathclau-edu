> **Contexte :** ce fichier fait partie des specs de la plateforme SaaS scolaire. À lire avec `CLAUDE.md` (modèle de données + architecture Spring Modulith / hexagonale) et `features/00-conventions.md` (conventions transverses : format API, codes HTTP, sécurité).
> Implémente **TOUT** ce fichier : tous les endpoints, toutes les règles métier, tous les événements. Ne rien omettre.

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

