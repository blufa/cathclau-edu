> **Contexte :** propositions bonus + outils de pilotage (checklist, contrat d'événements) pour la plateforme SaaS scolaire.

# 16. Propositions bonus (à prioriser) 🔵

| # | Fonctionnalité | Valeur ajoutée | Impact modèle |
|---|----------------|----------------|---------------|
| B1 | **Portail parent/élève** dédié (lecture notes, absences, factures, paiement en ligne) | Engagement familles, réduction charge secrétariat | Aucun (réutilise l'existant) |
| B2 | **Passage de classe en masse** (fin d'année → réinscription auto au niveau supérieur) | Gain de temps énorme en fin d'année | Aucun |
| B3 | **Paiement en ligne self-service** par le parent (lien de paiement) | Améliore le recouvrement | Aucun |
| B4 | **Rappels automatiques d'échéances** (J-3, J, J+3) configurables | Réduit les impayés | Réutilise `communication` |
| B5 | **Templates de messages** (§11.2) | Cohérence des notifications | Table `TemplateMessage` |
| B6 | **Paramètres tenant** (§15.2) | Personnalisation par école | Table `ParametreTenant` |
| B7 | **Audit log** (§13) | Conformité, sécurité, traçabilité | Table `AuditLog` |
| B8 | **Notifications in-app** (§15.4) | UX moderne | Table `Notification` |
| B9 | **Gestion des bulletins multi-périodes** (bulletin annuel cumulant les périodes) | Vue d'ensemble | Aucun (agrégation) |
| B10 | **Statistiques comparatives inter-années** | Pilotage pédagogique | Aucun (les années sont des entités) |
| B11 | **Certificats de scolarité / attestations** générés à la demande | Service aux familles | Réutilise génération PDF |
| B12 | **Détection de conflits de salle/enseignant** déjà cadrée (§5.6) — étendre à un calendrier visuel | Confort planification | Aucun |
| B13 | **Soft delete + corbeille** restaurable sur entités sensibles | Sécurité des données | Champ `deleted_at` |
| B14 | **Rate limiting & protection anti-abus** sur auth et webhooks | Sécurité | Aucun |
| B15 | **Health checks & métriques** (Actuator + Micrometer/Prometheus) | Exploitation | Aucun |

---

# 17. Checklist d'implémentation par module

> À cocher par Claude Code. Chaque module = domaine pur + ports + adaptateurs (in/web, out/persistence, out/gateway) + `package-info` `@ApplicationModule` + tests (domaine, application, adapters) + migration Flyway.

- [ ] `platform` — Tenants, Plans, Abonnements, FactureAbonnement, PaiementAbonnement, RecuAbonnement, ChangementPlan, QuotaService, dashboard plateforme.
- [ ] `iam` — Auth (JWT/refresh/reset), Utilisateurs, Rôles, Permissions, RBAC, `TenantContext` + filtre Hibernate multi-tenant, audit auth.
- [ ] `students` — Élèves (+quota), Inscriptions/réinscriptions, Documents, Parents, liens de parenté.
- [ ] `staff` — Enseignants + vues classes/matières/emploi du temps.
- [ ] `academics` — Années, Périodes (clôture→bulletins), Niveaux, Salles (conflits), Classes (effectif), Matières, ClasseMatiere, Emploi du temps (conflits).
- [ ] `attendance` — Présences élèves (lot, notif parent) + présences enseignants (séparées).
- [ ] `grading` — Évaluations, Notes (saisie/validation), Examens, Bulletins (calcul moyennes/rang/mention, génération sur clôture période), LigneBulletin.
- [ ] `billing` — FraisScolaire, Échéances, Factures (génération sur inscription, statuts).
- [ ] `payments` — Paiements (machine à états), TransactionPaiement (adaptateurs Orange/MTN/Wave/Stripe), Reçus, remboursements.
- [ ] `webhooks` — Entrants (vérif signature, idempotence) + Sortants (retry exponentiel, souscription par type, rejeu).
- [ ] `communication` — SMS/Email/WhatsApp (adaptateurs), diffusion de masse, statuts de livraison, écoute des événements métier, templates (bonus).
- [ ] **Transverse** — Dashboards (§12), AuditLog (§13), Import/Export + PDF (§14), Recherche/Paramètres/i18n (§15).
- [ ] **Tests Modulith** — `ApplicationModules.of(...).verify()` + `Documenter` ; Testcontainers PostgreSQL ; couverture domaine/application/adapters.

---

# 18. Récapitulatif des événements applicatifs (contrat inter-modules)

| Événement | Émis par | Consommé par | Effet |
|-----------|----------|--------------|-------|
| `TenantCree` | platform | iam, communication | provisionne admin/rôles, email bienvenue |
| `AbonnementActive` / `Suspendu` | platform | iam | débloque / bloque l'accès tenant |
| `AbonnementBientotExpire` | platform (job) | communication | relance admin |
| `PlanChange` | platform | platform (quota) | recalcul quota |
| `UtilisateurConnecte` | iam | audit | trace + `derniere_connexion` |
| `InscriptionValidee` | students | billing, academics | facture inscription + effectif classe |
| `PeriodeClôturee` | academics | grading | génération bulletins |
| `AbsenceEnregistree` | attendance | communication, webhooks | notif parent + webhook sortant |
| `NotesValidees` | grading | grading | recalcul moyennes |
| `BulletinGenere` | grading | documents, communication | PDF + notif parent |
| `BulletinPublie` | grading | communication, webhooks | notif parent + webhook |
| `FactureEmise` | billing | communication | notif parent |
| `EcheanceDepassee` | billing (job) | communication | relance paiement |
| `PaiementReussi` | payments | billing, payments, communication, webhooks | maj facture, reçu, notif, webhook |
| `PaiementEchoue` | payments | communication | information échec |
| `PaiementRembourse` | payments | billing | ajustement facture |
| `PaiementAbonnementReussi` | platform | platform, communication | activation + reçu |

---

*Fin du document — toutes les fonctionnalités des 11 modules métier + transverses + bonus sont couvertes, avec endpoints, règles métier et événements. Les éléments marqués 🔵 BONUS et les tables additionnelles (AuditLog, ParametreTenant, TemplateMessage, Notification) sont à valider avant implémentation.*
