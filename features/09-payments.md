> **Contexte :** ce fichier fait partie des specs de la plateforme SaaS scolaire. À lire avec `CLAUDE.md` (modèle de données + architecture Spring Modulith / hexagonale) et `features/00-conventions.md` (conventions transverses : format API, codes HTTP, sécurité).
> Implémente **TOUT** ce fichier : tous les endpoints, toutes les règles métier, tous les événements. Ne rien omettre.

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

