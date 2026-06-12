> **Contexte :** ce fichier fait partie des specs de la plateforme SaaS scolaire. À lire avec `CLAUDE.md` (modèle de données + architecture Spring Modulith / hexagonale) et `features/00-conventions.md` (conventions transverses : format API, codes HTTP, sécurité).
> Implémente **TOUT** ce fichier : tous les endpoints, toutes les règles métier, tous les événements. Ne rien omettre.

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

