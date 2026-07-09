# SCHOOL SaaS — Workflow d'abonnement + Prompts `platform` & `billing`

> Plateforme SaaS de **gestion scolaire** multi-tenant (chaque tenant = une école). Même socle technique que BADUS : Spring Boot 4 / Maven / Spring Modulith / hexagonal / Kafka, front Next.js + Tailwind + Mantine, IAM externe.
> **Distinction capitale** : ne JAMAIS mélanger l'**abonnement à la plateforme** (l'école paie l'éditeur du logiciel) avec les **frais scolaires** (les parents paient l'école). Ce sont deux modules de paiement totalement séparés (`platform`/`billing-subscription` vs `billing-school`/`payments`).

---

## 0. Les 3 moments d'« effectivité » de l'inscription d'une école


| Bascule | Déclencheur | État école | Usage |
|--------|-------------|-----------|-------|
| **1. Créée** | Formulaire d'inscription OU création par SUPER_ADMIN (éditeur) | `PENDING_VERIFICATION` | Rien |
| **2. Effective / utilisable** | Vérification email du directeur | `TRIAL` | **Gestion complète de l'école, gratuitement, N jours** |
| **3. Payante** | 1er paiement d'abonnement REUSSI | `ACTIVE` | Usage durable dans les limites du plan |

> L'école devient **utilisable à la bascule 2** (email vérifié → TRIAL). Le paiement de l'abonnement assure la **continuité** après l'essai. Les frais scolaires des élèves sont un flux indépendant, disponible dès que l'école est utilisable.

---

## 1. Machine à états de l'ÉCOLE (tenant) — module `platform`

```
   (inscription / création SUPER_ADMIN)
                 │
                 ▼
      PENDING_VERIFICATION
        │ email vérifié        │ création manuelle déjà réglée
        ▼                      ▼
      TRIAL ───(paiement)───► ACTIVE ◄──(renouvellement payé)
        │ fin essai            │  │
        ▼ sans paiement        │  ├─(impayé après grâce / suspension)─► SUSPENDED ─(régularisé)─► ACTIVE
      EXPIRED ─(souscription)──┘  └─(archivage)──────────────────────► ARCHIVED (terminal)
```
États : `PENDING_VERIFICATION, TRIAL, ACTIVE, SUSPENDED, EXPIRED, ARCHIVED`.
- `SUSPENDED` bloque l'accès de tous les utilisateurs de l'école (directeur, enseignants, comptables) sans supprimer les données.
- `ARCHIVED` = école fermée : lecture seule définitive.

---

## 2. Cycle de vie de l'abonnement plateforme — identique BADUS

`EN_ATTENTE → ACTIF → (SUSPENDU | EXPIRE | ANNULE)`, factures, paiements, reçus, prorata, renouvellement auto, délai de grâce, changement de plan. Plans scolaires basés sur le **nombre d'élèves** :
- `STARTER` ≤ 500 élèves, `STANDARD` ≤ 2000, `PREMIUM` illimité.
- Quota : `QuotaService.verifierQuotaEleves(tenantId)` appelé avant toute création d'élève ; refuse 403 si `count(eleves) >= plan.limite_eleves`.

---

## PROMPT 1 — Module `platform` (inscription école, cycle de vie, onboarding, quota élèves)

```
Implémente le module "platform" (niveau éditeur du SaaS scolaire). Entités : Tenant (= école), 
EmailVerificationToken, et les références de plan/abonnement exposées en lecture (le cycle complet est
dans billing-subscription). Accès /platform/** réservé au SUPER_ADMIN (éditeur). L'ADMIN_ECOLE ne voit
que SON école et son propre abonnement. IAM externe pour auth/rôles/permissions/découpage administratif.

================================================================
MACHINE À ÉTATS ÉCOLE (TenantLifecycleService centralisé)
================================================================
États : PENDING_VERIFICATION, TRIAL, ACTIVE, SUSPENDED, EXPIRED, ARCHIVED.
Transitions autorisées (autre -> 422) :
- PENDING_VERIFICATION -> TRIAL (email directeur vérifié)
- PENDING_VERIFICATION -> ACTIVE (création manuelle déjà réglée)
- TRIAL -> ACTIVE (AbonnementActive) ; TRIAL -> EXPIRED (fin d'essai)
- EXPIRED -> ACTIVE (souscription tardive)
- ACTIVE -> SUSPENDED ; SUSPENDED -> ACTIVE
- ACTIVE|SUSPENDED|EXPIRED -> ARCHIVED (terminal)
Toute transition passe par TenantLifecycleService.transition(...), idempotent, qui audite et publie.

================================================================
1) INSCRIPTION SELF-SERVICE D'UNE ÉCOLE (PUBLIC)
================================================================
- POST /api/v1/public/signup
  Corps : nom_ecole, slug_souhaite, email_directeur, password (vers IAM), ville, pays, type_etablissement
  (PRIMAIRE|COLLEGE|LYCEE|SUPERIEUR|MIXTE), plan_souhaite (optionnel; défaut essai).
  Actions : valider unicité slug+email -> créer Tenant PENDING_VERIFICATION, trial_ends_at (now +
  trial_days, défaut 30 pour le scolaire) -> EmailVerificationToken signé (24-48h) -> publier
  TenantSignupRequested (IAM crée l'ADMIN_ECOLE non vérifié ; communication envoie l'email de vérif) ->
  202 "vérifiez votre email".
- POST /api/v1/public/verify-email (token) — idempotent : PENDING_VERIFICATION -> TRIAL, activated_at,
  token consommé, publier TenantActivated -> 200, l'école est utilisable immédiatement.
- POST /api/v1/public/resend-verification (email).
- GET  /api/v1/public/ecoles/{slug}/branding — branding public (page login de l'école), sans auth.
RÈGLES : slug unique global ; PENDING_VERIFICATION sans accès (filtre global) ; purge des non vérifiés
après X jours ; rate limit sur signup/resend.

================================================================
2) ONBOARDING AUTOMATIQUE (sur TenantActivated)
================================================================
- IAM : marque le directeur vérifié, provisionne les rôles par défaut (ADMIN_ECOLE, ENSEIGNANT,
  COMPTABLE, PARENT, ELEVE) + permissions standard.
- Créer l'AnneeScolaire courante (optionnelle, selon le pays/calendrier) et les paramètres par défaut
  de l'école.
- Publier TenantOnboarded -> billing-subscription crée un Subscription TRIAL ; communication envoie
  l'email de bienvenue + guide directeur.
Idempotent (rejouable).

================================================================
3) CRÉATION MANUELLE (SUPER_ADMIN)
================================================================
- POST   /api/v1/platform/tenants — créer une école (onboarding). initial_state TRIAL ou ACTIVE (si
  contrat déjà payé -> Subscription ACTIF + facture PAYEE type MANUAL via événement).
- GET    /api/v1/platform/tenants — lister/rechercher (filtres : statut, ville, plan, type).
- GET    /api/v1/platform/tenants/{id} — détail + abonnement courant + quota élèves consommé.
- PUT    /api/v1/platform/tenants/{id} — maj (nom, branding, coordonnées, directeur).
- PATCH  /api/v1/platform/tenants/{id}/statut — activer/suspendre/archiver via TenantLifecycleService.
- DELETE /api/v1/platform/tenants/{id} — soft delete (jamais physique).
RÈGLES : SUSPENDU bloque tout l'accès de l'école (filtre de sécurité global) sans suppression. Le
branding est public pour la page de login. Création -> provisionnement admin + rôles + permissions
+ année scolaire courante (via événements).

================================================================
4) CONTRÔLE DE QUOTA ÉLÈVES (frontière billing-subscription)
================================================================
- QuotaService.verifierQuotaEleves(tenantId) appelé AVANT toute création d'élève par le module scolarité.
- Limite autorisée = BillingSubscriptionModuleApi.getActivePlanLimits(tenantId).limite_eleves
  (STARTER 500 / STANDARD 2000 / PREMIUM illimité). En TRIAL : limite d'essai.
- Consommé = ScolariteModuleApi.countEleves(tenantId) (count réel). Refus 403 { resource:"ELEVES",
  limite, consomme } si consomme >= limite. Écoute PlanChanged pour rafraîchir la limite.

================================================================
5) VUE ABONNEMENT (ADMIN_ECOLE, lecture seule)
================================================================
- GET /api/v1/abonnements/courant — agrège via BillingSubscriptionModuleApi (plan, statut, dates,
  auto_renew, élèves consommés vs limite). Actions -> endpoints billing-subscription.

ÉVÉNEMENTS (Outbox -> Kafka, school.platform.*.v1) : TenantSignupRequested, TenantActivated,
TenantOnboarded, TenantSuspended, TenantReactivated, TenantExpired, TenantArchived, TenantBrandingUpdated.
Consumers : AbonnementActive/Suspendu/Expire, PlanChange (-> via TenantLifecycleService + quota).

PLATFORM MODULE API : getTenant, existsAndActive, getStatus, getBranding(slug),
getDirectorContact(tenantId), isWithinStudentQuota(tenantId).

LIVRABLES : Flyway, DTO MapStruct, validation, ProblemDetail (403/409/422), tests Testcontainers
(signup->verify->TRIAL, transitions illégales 422, onboarding idempotent, quota élèves, branding public,
blocage SUSPENDED). ApplicationModules.verify().
```

---

## PROMPT 7 — Module `billing-subscription` (abonnement plateforme de l'école)

```
Implémente le module "billing-subscription" (abonnement de l'ÉCOLE à la plateforme éditeur).
NE PAS confondre avec billing-school (frais scolaires des élèves). Entités : SubscriptionPlan,
Subscription, PlanChange, SubscriptionInvoice, SubscriptionPayment, SubscriptionReceipt.

================================================================
MACHINE À ÉTATS ABONNEMENT (SubscriptionLifecycleService centralisé)
================================================================
EN_ATTENTE, ACTIF, SUSPENDU, EXPIRE, ANNULE. Transitions autorisées (autre -> 422) :
- EN_ATTENTE -> ACTIF (SubscriptionPayment REUSSI)
- ACTIF -> ACTIF (renouvellement payé)
- ACTIF -> SUSPENDU (impayé après grâce / manuel) ; SUSPENDU -> ACTIF (régularisé)
- ACTIF -> EXPIRE (date_fin sans renouvellement après grâce) ; ACTIF -> ANNULE (résiliation)
EXPIRE/ANNULE terminaux. Chaque transition audite, publie, et synchronise l'état ÉCOLE via événement.

================================================================
A) CATALOGUE DES PLANS (SUPER_ADMIN)
================================================================
- GET /api/v1/platform/plans (public, tarifs) ; POST ; PUT /{id} ; PATCH /{id}/archiver.
RÈGLES : STARTER (≤500 élèves), STANDARD (≤2000), PREMIUM (illimité). Champs : limite_eleves, prix,
devise, periodicite (MENSUEL|ANNUEL|TRIMESTRE_SCOLAIRE), type. Plan archivé non souscriptible mais valide
pour les abonnements en cours ; changement de prix sans effet avant renouvellement.

================================================================
B) CYCLE DE VIE DE L'ABONNEMENT
================================================================
- POST  /api/v1/platform/abonnements — souscrire (école + plan + périodicité) : Subscription EN_ATTENTE
  + SubscriptionInvoice (BROUILLON->EMISE).
- GET   /api/v1/abonnements/courant ; GET /api/v1/platform/abonnements (filtres).
- PATCH /api/v1/platform/abonnements/{id}/suspendre ; /annuler.
- PATCH /api/v1/abonnements/{id}/auto-renouvellement.
- POST  /api/v1/platform/abonnements/{id}/changer-plan (crée PlanChange).
RÈGLES : activation conditionnée à SubscriptionPayment REUSSI. Renouvellement à date_fin : auto_renew ->
régénérer facture + tenter paiement ; sinon délai de grâce (défaut 7j) puis EXPIRE + école SUSPENDED.
Délai de grâce sur impayé : relances J+1/3/7, fin de grâce -> SUSPENDU, régularisation -> ACTIF.
Changement de plan : UPGRADE immédiat avec prorata (montant_proratise) ; DOWNGRADE au renouvellement ;
downgrade qui rend l'école au-dessus de la nouvelle limite_eleves -> 409 (ou gel des inscriptions
d'élèves, configurable). PlanChange(type UPGRADE|DOWNGRADE|RENOUVELLEMENT, date_effet, montant_proratise).
Publie PlanChange. Événements : AbonnementActive, AbonnementSuspendu, AbonnementBientotExpire (J-7),
AbonnementExpire, AbonnementAnnule.

================================================================
C) FACTURATION ABONNEMENT
================================================================
- GET /api/v1/platform/factures-abonnement ; GET /api/v1/abonnements/factures ;
  GET /api/v1/factures-abonnement/{id}/pdf ; POST /api/v1/platform/factures-abonnement/{id}/relancer.
MACHINE : BROUILLON -> EMISE -> (PAYEE | PARTIELLE | IMPAYEE | ANNULEE).
RÈGLES : montant_total = montant_ht + montant_tva ; solde_restant = total - montant_paye. PDF asynchrone,
URL dans fichier_pdf. reference unique séquentielle par tenant éditeur. IMPAYEE échue -> relances auto.

================================================================
D) PAIEMENT ABONNEMENT & REÇUS
================================================================
- POST /api/v1/abonnements/paiements (Orange Money/MTN/Wave/Visa/MasterCard via payment) ;
  GET /api/v1/abonnements/paiements/{id} ; GET /api/v1/abonnements/recus/{id}/pdf.
RÈGLES : flux INITIE->EN_ATTENTE->REUSSI|ECHOUE|REMBOURSE, confirmation par callback (module webhooks).
Au REUSSI (consumer idempotent) : facture PAYEE, Subscription EN_ATTENTE->ACTIF, génération
SubscriptionReceipt (PDF + email). Publie PaiementAbonnementReussi + AbonnementActive. ECHOUE -> facture
IMPAYEE + relance (PaiementAbonnementEchoue). Paiement partiel -> PARTIELLE.

================================================================
JOBS PLANIFIÉS
================================================================
- Échéances : J-7/3/1 -> AbonnementBientotExpire ; date_fin -> renouvellement auto ou grâce ; fin grâce
  -> SUSPENDU/EXPIRE. Réconciliation des paiements EN_ATTENTE sans callback.

BILLING SUBSCRIPTION MODULE API : getActivePlanLimits(tenantId), getSubscription(tenantId),
isSubscriptionActive(tenantId).

LIVRABLES : Flyway, DTO, validation, ProblemDetail (409/422), PDF async, tests Testcontainers
(souscription->facture->paiement->ACTIF, transitions illégales 422, renouvellement auto, grâce->
suspension->régularisation, changement de plan + prorata, downgrade hors-limite, paiement partiel,
réconciliation, reçu). ApplicationModules.verify().
```

---

## Note sur les DEUX systèmes de paiement de l'école

Pour éviter toute confusion lors de l'implémentation :

| | Abonnement plateforme | Frais scolaires |
|--|----------------------|-----------------|
| **Module** | `billing-subscription` | `billing-school` + `payments` |
| **Qui paie** | L'école → l'éditeur du SaaS | Les parents → l'école |
| **Objets** | SubscriptionInvoice, SubscriptionPayment, SubscriptionReceipt | FraisScolaire, Facture, Paiement, Recu |
| **Déclencheur** | Souscription / renouvellement | Inscription élève / échéancier |
| **Tables** | Préfixe `subscription_*` | Préfixe `school_fee_*` / `invoice_*` |
| **Accès** | SUPER_ADMIN (+ ADMIN_ECOLE lecture) | ADMIN_ECOLE, COMPTABLE, PARENT |

Les deux partagent le module `payment` (passerelles Orange/MTN/Wave/Visa) mais avec des `object_type` distincts (`SUBSCRIPTION` vs `SCHOOL_FEE`) et des comptes comptables séparés.
```