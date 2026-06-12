> **Contexte :** ce fichier fait partie des specs de la plateforme SaaS scolaire. À lire avec `CLAUDE.md` (modèle de données + architecture Spring Modulith / hexagonale) et `features/00-conventions.md` (conventions transverses : format API, codes HTTP, sécurité).
> Implémente **TOUT** ce fichier : tous les endpoints, toutes les règles métier, tous les événements. Ne rien omettre.

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

