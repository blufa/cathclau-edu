> **Contexte :** fonctionnalités transverses de la plateforme SaaS scolaire. À implémenter APRÈS les 11 modules métier. Lire avec CLAUDE.md et features/00-conventions.md.
> Certaines nécessitent des tables additionnelles (AuditLog, ParametreTenant, etc.) — à valider avant création.

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

