# Specs fonctionnelles — Plateforme SaaS scolaire (découpées par module)

Ce dossier découpe `FEATURES.md` en un fichier par module, pour donner à Claude Code des specs ciblées sans saturer le contexte. Chaque fichier module est autoporteur : endpoints REST, règles métier et événements.

## À lire en premier

- `../CLAUDE.md` — modèle de données complet + architecture Spring Modulith / hexagonale.
- `00-conventions.md` — conventions transverses (format API, pagination, erreurs RFC 7807, codes HTTP, sécurité). **S'appliquent à tous les modules.**

## Ordre d'implémentation recommandé (par dépendances)

| Ordre | Fichier | Module | Pourquoi à ce rang |
|-------|---------|--------|--------------------|
| 1 | `02-iam.md` | iam | Multi-tenant + RBAC : socle de tout le reste |
| 2 | `01-platform.md` | platform | Tenants, plans, abonnements, quota |
| 3 | `05-academics.md` | academics | Années/périodes/classes : référentiel scolaire |
| 4 | `03-students.md` | students | Élèves, inscriptions, parents |
| 5 | `04-staff.md` | staff | Enseignants |
| 6 | `06-attendance.md` | attendance | Présences |
| 7 | `07-grading.md` | grading | Notes, examens, bulletins |
| 8 | `08-billing.md` | billing | Frais, échéances, factures |
| 9 | `09-payments.md` | payments | Paiements, transactions, reçus |
| 10 | `10-webhooks.md` | webhooks | Callbacks entrants + sortants |
| 11 | `11-communication.md` | communication | SMS / Email / WhatsApp |
| 12 | `12-transverse.md` | transverse | Dashboards, audit, import/export, paramètres |
| — | `13-bonus-checklist-events.md` | — | Bonus + checklist globale + contrat d'événements |

## Workflow Claude Code (un module à la fois)

Pour chaque module, coller un prompt du type :

```
Lis CLAUDE.md, features/00-conventions.md et features/02-iam.md.
Implémente la TOTALITÉ de features/02-iam.md : tous les endpoints,
toutes les règles métier, tous les événements (cf. contrat d'événements
dans features/13-bonus-checklist-events.md §18).

Respecte l'architecture du CLAUDE.md : domaine pur + ports (in/out) +
adapters (in/web, out/persistence, out/gateway) + package-info
@ApplicationModule. Multi-tenant via TenantContext + filtre Hibernate.
Migration Flyway. Tests : domaine (purs), application (ports mockés),
adapters (@DataJpaTest, @WebMvcTest, Testcontainers).

À la fin : lance ApplicationModules.of(Application.class).verify(),
compile + tests, commit "feat(iam): implémentation complète", puis
donne-moi le récap et ATTENDS ma validation avant le module suivant.

Si un point est ambigu, pose la question au lieu de deviner.
Ne saute aucune fonctionnalité.
```

Remplacer `02-iam.md` / `iam` par le module courant à chaque étape.

## Tables additionnelles à valider

Signalées dans `12-transverse.md` et `13-bonus-checklist-events.md` : `AuditLog`, `ParametreTenant`, `TemplateMessage`, `Notification`. À confirmer avant que Claude Code ne les crée.