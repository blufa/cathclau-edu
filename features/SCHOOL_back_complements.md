# SCHOOL SaaS — Compléments back-end (volet 2) : Comptabilité, Cartes scolaires, Plans paramétrables

> Ce document **complète** `SCHOOL_back_complements.md` (volets A–J). Il ajoute le module de **comptabilité**, détaille le module **cartes scolaires**, et précise le **paramétrage complet des plans par le Super Admin** (modules inclus + plan trial). À lire avec `CLAUDE.md` et `00-conventions.md`. Implémente **TOUT**.

---

## K. NOUVEAU module `accounting` (14) — Comptabilité (2 niveaux : simple + partie double SYSCOHADA)

> Deux niveaux activables par école via un paramètre `mode_comptable` (SIMPLE | PARTIE_DOUBLE).
> - **SIMPLE** : caisse/banque, journaux de recettes/dépenses, catégorisation, rapports — accessible à toute école.
> - **PARTIE_DOUBLE** : comptabilité normalisée **SYSCOHADA** (plan comptable, écritures équilibrées débit=crédit, grand livre, balance, bilan, compte de résultat) — pour les écoles qui veulent une compta réglementaire.
    > La compta **n'invente aucune donnée** : elle s'alimente des modules `payments` (recettes), `billing` (créances), `expenses` (dépenses) via événements, et permet la saisie manuelle d'écritures diverses.

```
Implémente le module "accounting" (comptabilité scolaire à deux niveaux).

=== PARAMÉTRAGE ===
- AccountingSettings : tenant_id, mode_comptable (SIMPLE|PARTIE_DOUBLE), referentiel (SYSCOHADA|AUTRE),
  devise, exercice_debut (mois de début d'exercice, souvent septembre pour le scolaire), tva_applicable,
  taux_tva_defaut, numerotation_auto, est_actif.
- ENDPOINTS : GET /api/v1/accounting/settings ; PUT /api/v1/accounting/settings.
- Passer de SIMPLE à PARTIE_DOUBLE est autorisé (les journaux simples restent lisibles) ; l'inverse est
  bloqué s'il existe des écritures en partie double sur l'exercice courant (-> 409).

=== EXERCICE COMPTABLE ===
- FiscalYear : tenant_id, libelle, date_debut, date_fin, statut (OUVERT|CLOTURE), annee_scolaire_id?
  (lien optionnel avec l'année scolaire). Un seul exercice OUVERT courant.
- ENDPOINTS : POST/GET /api/v1/accounting/exercices ; PATCH /{id}/cloturer (génère le report à nouveau
  en partie double : soldes des comptes de bilan reportés sur l'exercice suivant). Clôture interdite si
  des écritures sont en brouillon (-> 422).

=== NIVEAU 1 — COMPTABILITÉ SIMPLE (toujours disponible) ===
- CashBox (caisse) et BankAccount (compte bancaire de l'école) : tenant_id, libelle, type (CAISSE|BANQUE),
  banque?, numero_compte?, solde_initial, solde_courant, devise, est_actif.
- CashFlowEntry (mouvement de trésorerie) : tenant_id, compte (CashBox/BankAccount), sens (ENTREE|SORTIE),
  montant, devise, date, categorie (SCOLARITE|INSCRIPTION|SALAIRE|FOURNISSEUR|CHARGE|AUTRE), libelle,
  reference_type (PAIEMENT|EXPENSE|MANUEL), reference_id, piece_justificative_url, rapproche (bool).
- ENDPOINTS :
  POST/GET/PUT /api/v1/accounting/caisses ; .../banques ; PATCH /{id}/ajuster-solde (régularisation).
  POST/GET /api/v1/accounting/mouvements (filtres compte, sens, catégorie, période) ; PUT /{id} ; DELETE.
  POST /api/v1/accounting/mouvements/{id}/rapprocher — rapprochement bancaire (marque rapproche=true).
  GET /api/v1/accounting/caisses/{id}/journal — journal de caisse (entrées/sorties + solde progressif).
- ALIMENTATION AUTO (consumers) : PaiementReussi -> CashFlowEntry ENTREE ; DepensePayee -> SORTIE.
  Idempotent (reference_type+reference_id unique).

=== NIVEAU 2 — PARTIE DOUBLE (SYSCOHADA) ===
- ChartOfAccount (plan comptable) : tenant_id, numero_compte (ex. 411 Clients, 521 Banque, 571 Caisse,
  701 Ventes/Scolarité, 60x Achats/Charges, 64x Charges de personnel...), libelle, classe (1..8 SYSCOHADA),
  type (ACTIF|PASSIF|CHARGE|PRODUIT), parent_id (réflexive), lettrable, est_actif. Seed d'un plan SYSCOHADA
  scolaire par défaut au provisioning (modifiable par l'école).
- Journal (journaux) : tenant_id, code (VT ventes, AC achats, BQ banque, CA caisse, OD opérations diverses),
  libelle, type, compte_contrepartie_defaut?.
- JournalEntry (pièce/écriture) : tenant_id, fiscal_year_id, journal_id, numero_piece (séquentiel par
  journal+exercice), date, libelle, reference_type?, reference_id?, statut (BROUILLON|VALIDEE|LETTREE),
  total_debit, total_credit. CONTRAINTE : total_debit = total_credit (équilibre) sinon 422.
- JournalEntryLine (ligne d'écriture) : journal_entry_id, chart_of_account_id, libelle, debit, credit,
  lettrage_code?, tiers_type? (CLIENT/PARENT/FOURNISSEUR), tiers_id?. Une ligne a soit débit soit crédit.
- ENDPOINTS :
  CRUD /api/v1/accounting/plan-comptable (arbre) ; import/export du plan.
  CRUD /api/v1/accounting/journaux.
  POST /api/v1/accounting/ecritures — saisir une écriture (lignes équilibrées) ; PUT /{id} (si BROUILLON) ;
  PATCH /{id}/valider (BROUILLON->VALIDEE, verrouille) ; DELETE /{id} (si BROUILLON).
  GET /api/v1/accounting/ecritures — filtres journal, exercice, compte, période, statut, tiers.
  POST /api/v1/accounting/lettrage — lettrer des lignes d'un compte (rapprocher créance/règlement).
- ALIMENTATION AUTO (consumers, génèrent des écritures VALIDEE selon mapping paramétrable) :
  * FactureEmise (billing) -> 411 Clients (débit) / 701 Scolarité (crédit).
  * PaiementReussi (payments) -> 521 Banque ou 571 Caisse (débit) / 411 Clients (crédit) + lettrage.
  * DepensePayee (expenses) -> 60x/61x/64x Charge (débit) / 521/571 (crédit).
  Le MAPPING (type d'opération -> comptes) est paramétrable : AccountingMapping (tenant_id, operation_type,
  compte_debit_id, compte_credit_id, journal_id). Modifiable par le comptable.

=== ÉTATS / RAPPORTS COMPTABLES ===
- GET /api/v1/accounting/grand-livre?compte&exercice&periode — mouvements + solde par compte.
- GET /api/v1/accounting/balance?exercice — balance générale (tous comptes : total débit, crédit, solde).
- GET /api/v1/accounting/journal/{code}?exercice&periode — journal détaillé.
- GET /api/v1/accounting/bilan?exercice — bilan SYSCOHADA (ACTIF / PASSIF) [partie double].
- GET /api/v1/accounting/compte-resultat?exercice — compte de résultat (CHARGES / PRODUITS, résultat net).
- GET /api/v1/accounting/livre-caisse?exercice&compte — pour le niveau simple.
- Tous exportables PDF/Excel (asynchrone, DispatchJob).

RÈGLES MÉTIER
- Équilibre débit=crédit obligatoire sur chaque écriture (partie double).
- Écriture VALIDEE non modifiable (contre-passation possible : écriture d'extourne).
- Numérotation séquentielle continue par journal et par exercice (pas de trou).
- Exercice CLOTURE en lecture seule ; report à nouveau auto des comptes de bilan.
- mode SIMPLE : pas de plan comptable requis, seulement caisses/banques + mouvements + journaux simples.
- Lettrage pour rapprocher créances (411) et règlements. Soft delete, audit complet.

ÉVÉNEMENTS : EcritureValidee, ExerciceCloture, RapprochementEffectue, EtatComptableGenere.
ACCOUNTING MODULE API : getSoldeCaisse(), getResultatExercice(exerciceId), getBalance(exerciceId).
INTÉGRATIONS : lecture seule via PaymentsModuleApi, BillingModuleApi, ExpensesModuleApi — aucune donnée
dupliquée, agrégation uniquement, génération d'écritures via mapping.
TESTS : mode simple (caisse/banque/mouvements/rapprochement), partie double (écriture équilibrée 422 si
déséquilibre, validation verrou, grand livre, balance équilibrée, bilan, compte de résultat), mapping auto
sur événements (facture/paiement/dépense), lettrage, clôture d'exercice + report à nouveau, transitions 422.
```

---

## L. Module `student-cards` (15) — Cartes scolaires avec QR (génération dans l'app)

> Reprend et détaille le volet I. L'école génère ses cartes scolaires directement dans l'application : modèle personnalisable (recto/verso), QR code, génération PDF unitaire ou par classe, réémission.

```
Implémente le module "student-cards" (génération des cartes scolaires des élèves).

ENTITÉS
- CardTemplate (modèle de carte) : tenant_id, nom_modele, recto_config (JSON : champs affichés, positions,
  polices), verso_config (JSON : règlement, QR, contacts), logo_url, couleur_primaire, couleur_secondaire,
  fond_url?, orientation (PAYSAGE|PORTRAIT), est_defaut, est_actif. Plusieurs modèles possibles, un défaut.
- StudentCard (carte émise) : tenant_id, eleve_id, card_template_id, annee_scolaire_id, classe_id,
  matricule, numero_carte (séquentiel unique par tenant), qr_code_payload (signé), qr_code_url,
  photo_url, date_emission, date_expiration, statut (ACTIVE|EXPIREE|PERDUE|REMPLACEE), pdf_url,
  remplace_carte_id? (réflexive, si réémission).
- CardPrintBatch (impression en masse) : tenant_id, classe_id?, annee_scolaire_id, total, statut
  (EN_COURS|TERMINE|ECHEC), pdf_url (planche de cartes), demande_par, date.

ENDPOINTS
- CRUD /api/v1/cartes/modeles (CardTemplate) ; PATCH /{id}/definir-defaut ; aperçu du modèle (preview PNG).
- POST /api/v1/eleves/{id}/carte — générer/émettre la carte d'un élève (avec photo, modèle).
- GET  /api/v1/eleves/{id}/carte — carte courante de l'élève.
- GET  /api/v1/cartes — liste (filtres classe, année, statut, recherche matricule).
- GET  /api/v1/cartes/{id}/pdf — PDF de la carte (asynchrone).

- POST /api/v1/cartes/impression-masse — générer pour toute une classe/année (CardPrintBatch) -> planche PDF.
- GET  /api/v1/cartes/batches/{id} — suivi du batch + PDF planche.
- PATCH /api/v1/cartes/{id}/remplacer — réémettre (perte/détérioration) : ancienne -> REMPLACEE, nouvelle
  ACTIVE avec nouveau numero_carte (lien remplace_carte_id).
- PATCH /api/v1/cartes/{id}/declarer-perdue — statut PERDUE.
- GET  /api/v1/cartes/verifier/{qrPayload} — vérifier la validité d'une carte par scan QR (endpoint léger,
  renvoie profil minimal + validité ; protégé/rate-limité).

QR CODE
- Contenu signé (HMAC) : { tenant, eleve_id, matricule, numero_carte, annee }. Le scan permet :
  consultation rapide du profil élève, vérification de validité, base d'un futur contrôle d'accès.
- Le payload est signé pour empêcher la falsification ; la vérification se fait côté serveur.

RÈGLES MÉTIER
- Un seul exemplaire ACTIVE par élève et par année (réémission -> ancienne REMPLACEE).
- Génération PDF asynchrone (DispatchJob), URL persistée. Impression unitaire ou par classe.
- Historique des impressions/réémissions conservé. date_expiration alignée sur l'année scolaire.
- Modèle personnalisable par l'école (logo, couleurs, champs, recto/verso). Utilise TenantSettings
  (logo/cachet/couleurs) comme valeurs par défaut.
- Photo de l'élève requise (depuis students) ; si absente -> 422 avec message.

ÉVÉNEMENTS : CarteScolaireGeneree, CarteScolaireImprimee, CarteScolaireRemplacee, CarteScolairePerdue.
STUDENT-CARDS MODULE API : getCarteActive(eleveId), verifierQr(payload).
TESTS : génération unitaire, impression masse par classe, réémission (un seul ACTIVE), déclaration perte,
QR signé + vérification, photo manquante 422, modèle par défaut, PDF asynchrone.
```

---

## M. Super Admin — Paramétrage complet des plans (modules inclus + plan trial)

> Complète le volet G. Le Super Admin construit chaque plan de bout en bout : limites, **modules inclus**, prix, périodicité, et le **plan d'essai** (trial = un plan normal avec **prix 0 + trial_days**).

```
Étends "billing-subscription" / "platform" : paramétrage complet des plans par le SUPER_ADMIN.

CATALOGUE DES MODULES (référentiel plateforme, lecture)
- ModuleCatalog : module_code (IAM, STUDENTS, STAFF, ACADEMICS, ATTENDANCE, GRADING, BILLING, PAYMENTS,
  WEBHOOKS, REPORT_CARD, COMMUNICATION, EXPENSES, ACCOUNTING, STUDENT_CARDS), libelle, description,
  est_premium, dependances (modules requis), est_actif.
- GET /api/v1/platform/modules — catalogue complet (pour cocher les modules d'un plan).

PLAN ENRICHI (SubscriptionPlan + champs)
- Champs : code, libelle, type (STANDARD|TRIAL|SUR_MESURE), prix, devise,
  periodicite (MENSUEL|ANNUEL|TRIMESTRE_SCOLAIRE), limite_eleves, limite_utilisateurs?,
  limite_sms_mensuel?, limite_stockage?, is_trial (bool), trial_days (si is_trial ou essai inclus),
  est_public (visible sur la grille tarifaire), est_archive, ordre_affichage, description, mise_en_avant.
- PLAN TRIAL : c'est un plan normal avec prix = 0 et is_trial = true + trial_days (ex. 30). À la création
  d'une école sans plan choisi, on lui affecte le plan trial par défaut (le plan public is_trial avec
  est_default_trial = true). Un seul plan trial par défaut.
- PlanModule (volet G) : association plan <-> module_code (+ est_actif). Définit les modules inclus au plan.

ENDPOINTS SUPER_ADMIN
- POST   /api/v1/platform/plans — créer un plan (tous les champs ci-dessus).
- PUT    /api/v1/platform/plans/{id} — modifier.
- PATCH  /api/v1/platform/plans/{id}/archiver — archiver (non souscriptible, conservé pour les abonnements
  en cours).
- PATCH  /api/v1/platform/plans/{id}/definir-trial-defaut — désigner le plan d'essai par défaut.
- GET    /api/v1/platform/plans/{id}/modules ; POST .../modules (ajouter) ;
  DELETE .../modules/{moduleCode} ; PATCH .../modules/{moduleCode}/activer|desactiver.
- GET    /api/v1/platform/tenants/{id}/modules — modules effectifs de l'école.
- PATCH  /api/v1/platform/tenants/{id}/modules/{moduleCode}/activer|desactiver — override par école
  (ex. accorder temporairement un module premium : date_activation/date_expiration via TenantModule).

RÈGLES MÉTIER
- À la souscription/au provisioning : les modules du plan (PlanModule) -> TenantModule de l'école (avec
  permissions IAM associées créées). Le front filtre les menus selon les modules actifs de l'école.
- Vérification des dépendances : activer GRADING peut requérir ACADEMICS, etc. (ModuleCatalog.dependances)
  -> 409 si dépendance manquante.
- Changement de plan : recalcule l'ensemble des TenantModule (ajout/retrait) ; un downgrade qui retire un
  module utilisé -> warning/confirmation (données conservées en lecture seule, configurable).
- Plan archivé : valide pour les abonnements en cours ; changement de prix sans effet avant renouvellement.
- TRIAL : à l'expiration (trial_days), l'école passe EXPIRED si aucun plan payant souscrit (cf. machine à
  états platform) ; relances via communication (J-7/3/1).
- GestionGuard : un endpoint d'un module désactivé pour l'école -> 403 { module, raison:"MODULE_INACTIF" }.

ÉVÉNEMENTS : PlanCree, PlanModifie, PlanArchive, PlanModulesModifies, TenantModuleActive,
TenantModuleDesactive, PlanTrialDefini.
MODULE API : getModulesActifs(tenantId), isModuleActif(tenantId, moduleCode), getPlanTrialDefaut().
TESTS : créer plan (standard + trial prix 0 + trial_days), associer modules, dépendances 409, provisioning
modules à la souscription, override module par école (temporaire avec expiration), downgrade retire module,
plan trial par défaut affecté à l'inscription, endpoint module inactif -> 403.
```

---

## N. Récap final des modules back-end

| # | Module | Statut |
|---|--------|--------|
| 00 | conventions | existant |
| 01 | platform + billing-subscription | **enrichi (modules par plan, plan trial, tenant settings)** |
| 02 | iam | existant |
| 03 | students | **enrichi (cartes scolaires -> module 15)** |
| 04 | staff | existant |
| 05 | academics | existant |
| 06 | attendance | existant |
| 07 | grading | enrichi (barèmes paramétrables) |
| 08 | billing (frais scolaires) | existant |
| 09 | payments | complété (reçu multicanal) |
| 10 | webhooks | existant |
| 11 | report-card (livret scolaire) | nouveau |
| 12 | communication (SMS/WhatsApp/Email) | nouveau |
| 13 | expenses (dépenses + budget + trésorerie) | nouveau |
| 14 | **accounting (compta simple + partie double SYSCOHADA)** | **NOUVEAU** |
| 15 | **student-cards (cartes scolaires QR)** | **NOUVEAU** |

> Les rapports financiers consolidés (volet J) sont fournis par `accounting` (synthèse, trésorerie, recettes,
> dépenses, impayés, taux de recouvrement) en agrégeant billing/payments/expenses — aucune donnée dupliquée.