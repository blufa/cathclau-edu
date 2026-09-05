# Plan de test complet — de la création d'une école à l'usage quotidien, tous rôles

> Script de recette de bout en bout : à dérouler dans l'ordre pour valider une mise en production.
> Chaque section précise **qui** teste (rôle), **où** (route de l'application) et le résultat
> attendu. Les correctifs déjà couverts en détail (RBAC, Garderie, heures enseignant, SMS, cours en
> ligne...) ont leur propre script pas-à-pas dans `PLAN-DE-TEST-RBAC-GARDERIE-DATEPICKER.md` — ce
> document-ci les référence sans les répéter intégralement.

**Comptes de test nécessaires** : 1 compte `SUPER_ADMIN`, 1 `SCHOOL_ADMIN` (créé à l'onboarding),
puis au moins 1 `DIRECTOR`, 1 `ACCOUNTANT`, 1 `SECRETARY`, 1 `TEACHER`, 1 rôle personnalisé (ex.
« Surveillant »), 2 `PARENT` (dont un avec plusieurs enfants), 1 `STUDENT`, 1 `DPE_ADMIN` (si le
palier préfectoral est utilisé).

---

## Phase 0 — Découverte publique & inscription d'une nouvelle école

**Rôle** : visiteur anonyme.

1. `/` (accueil), `/fonctionnalites`, `/tarifs` : les pages publiques se chargent, le catalogue de
   plans (`STARTER`/`STANDARD`/`PREMIUM`) est visible avec les limites d'élèves.
2. `/signup` : créer une nouvelle école — raison sociale, ville, coordonnées du directeur, plan
   choisi. Vérifier la création du tenant, de l'admin `SCHOOL_ADMIN` et l'email de bienvenue
   (`TenantCree` → `iam` + `communication`).
3. `/verify-email` puis `/resend-verification` si besoin : vérifier l'adresse email de l'admin.
4. `/login` : se connecter avec le nouveau compte `SCHOOL_ADMIN` — connexion possible par **email**
   ou par **numéro de téléphone** (voir §5 du plan RBAC).
5. `/forgot-password` → `/reset-password` : réinitialisation de mot de passe par email, avec le lien
   reçu.

*Résultat attendu* : une école peut s'inscrire de bout en bout sans intervention manuelle du
SUPER_ADMIN, et récupérer l'accès en cas de mot de passe oublié.

---

## Phase 1 — Administration plateforme (SUPER_ADMIN)

**Rôle** : `SUPER_ADMIN`, routes `/platform/*`.

1. `/platform/tenants` : retrouver l'école créée en Phase 0, ouvrir sa fiche `/platform/tenants/[id]`
   — abonnement courant, quota d'élèves consommé, branding.
2. Modifier le tenant (nom, branding) puis tester `PATCH statut` : **suspendre** l'école → se
   reconnecter avec le compte `SCHOOL_ADMIN` de cette école doit être **bloqué** (tous les
   utilisateurs du tenant). Réactiver : l'accès revient.
3. `/platform/plans` : créer/modifier un plan, vérifier que les abonnements déjà actifs sur l'ancien
   tarif ne sont pas affectés rétroactivement. Archiver un plan : il disparaît de `/tarifs` (page
   publique) mais reste valide pour les écoles déjà abonnées.
4. `/platform/subscriptions` : suivre le cycle de l'abonnement de l'école test —
   `EN_ATTENTE → ACTIF` après paiement de la facture d'abonnement (voir Phase 1bis), puis tester
   **suspension manuelle** et **changement de plan** (upgrade) : une ligne `ChangementPlan` avec
   prorata doit apparaître.
5. `/platform/frais` : définir le frais d'usage plateforme (ex. 30 000 GNF/mois) — vérifier qu'il ne
   s'applique **plus automatiquement** à toutes les mensualités (voir §6 du plan RBAC : frais annexe
   opt-in par élève, décidé à l'inscription).
6. `/platform/users` : lister les comptes tous tenants confondus (vue SUPER_ADMIN).
7. `/platform/prefectures` et `/dpe/*` (si le palier préfectoral est activé) : rattacher l'école à une
   préfecture, se connecter en `DPE_ADMIN` et vérifier `/dpe/dashboard` + `/dpe/schools` (lecture
   seule, écoles du périmètre uniquement).
8. `/platform/blog/posts` : publication d'articles (rôle `REDACTEUR` ou `SUPER_ADMIN`).
9. `/platform/dashboard` : nombre de tenants actifs/suspendus, MRR, répartition par plan, taux de
   churn, paiements d'abonnement du mois — les chiffres reflètent les actions des étapes précédentes.

*Résultat attendu* : le cycle de vie complet d'un tenant (créer, suspendre, réactiver, changer de
plan) fonctionne sans jamais mélanger les frais d'abonnement SaaS avec les frais scolaires.

---

## Phase 1bis — Facturation & paiement de l'abonnement SaaS

**Rôle** : `SCHOOL_ADMIN`, route `/subscription/*`.

1. `/subscription/invoices` : voir la facture d'abonnement `EN_ATTENTE`/`EMISE` générée à la
   souscription.
2. Payer via le portail Djomy (`components/platform/payment-flow.tsx`) : redirection, confirmation,
   la facture passe `PAYEE` et l'abonnement `ACTIF` (activation conditionnée au paiement réussi).
3. Télécharger le reçu PDF de l'abonnement et la facture PDF.
4. Activer/désactiver le **renouvellement automatique** ; simuler l'approche de l'échéance (J-7) :
   vérifier la relance (`AbonnementBientotExpire` → email/SMS à l'admin).

*Résultat attendu* : le paiement de l'abonnement suit le même circuit Djomy que les frais scolaires,
mais reste dans un livre comptable strictement séparé (jamais mélangé aux factures élèves).

---

## Phase 2 — Paramétrage initial de l'école

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR`.

1. `/onboarding` : suivi guidé affiché uniquement pour `SCHOOL_ADMIN`/`DIRECTOR` (widget
   « Démarrage » masqué pour les autres rôles — vérifié dans le correctif RBAC).
2. `/academic-years` : créer l'année scolaire, la définir **courante** (une seule à la fois par
   tenant), créer ses périodes (trimestres/semestres). Tenter de créer une 2ᵉ année « courante » :
   la première doit basculer automatiquement à non-courante.
   - Créer une période **par erreur** (mauvais libellé/dates) : boutons **Modifier** et **Suppr.**
     disponibles tant qu'elle n'est pas clôturée — modifier corrige le libellé/dates/ordre,
     supprimer la retire complètement. Clôturer une période : les deux boutons disparaissent (une
     période clôturée est en lecture seule, bulletins déjà générés).
3. `/referentials` : créer les **niveaux** (dont **Garderie**, distinct de Maternelle — voir §2 du
   plan RBAC) et les **salles** (capacité, disponibilité).
4. `/classes` : créer les classes (rattachées à un niveau + l'année courante), affecter un
   enseignant principal et une salle. Vérifier le refus de création si l'année est
   CLOTUREE/ARCHIVEE.
5. `/referentials` (onglet Matières) : créer les matières avec coefficient, puis affecter
   matière + enseignant à chaque classe (`ClasseMatiere`).
6. `/timetable` : construire l'emploi du temps ; tenter de créer deux créneaux qui se chevauchent
   pour le même enseignant, la même salle ou la même classe → doit être **refusé (409)**.
   - **Heure creuse** : dans « Ajouter un créneau », choisir **« — Heure creuse (sans matière) — »**
     à la place d'une matière — un champ **Libellé** libre devient obligatoire (ex. « Étude dirigée »,
     « Permanence », « TD »). Enregistrer **sans** choisir d'enseignant ni de salle : doit être
     accepté (aucun des deux n'est requis). Le créneau apparaît dans la grille/liste avec son
     libellé à la place du nom de matière.
   - **Imprimer toutes les classes** (mode « Par classe ») : un seul document paysage s'ouvre avec
     une page par classe de l'année sélectionnée (saut de page automatique), chacune avec sa propre
     grille hebdomadaire — vérifier que l'impression/export PDF du navigateur donne un document
     multi-pages complet, une classe par page.
7. `/settings` : paramètres du tenant (barème de mentions, format de matricule, devise, fuseau
   horaire, délai de grâce abonnement, horaires d'ouverture, politique de mot de passe).

*Résultat attendu* : l'école dispose d'un référentiel complet (années, niveaux, salles, classes,
matières, emploi du temps sans conflit) avant toute inscription d'élève.

---

## Phase 3 — Utilisateurs, rôles & permissions (RBAC)

**Rôle** : `SCHOOL_ADMIN`.

Se référer au détail complet dans `PLAN-DE-TEST-RBAC-GARDERIE-DATEPICKER.md` §7-8. Checklist rapide :

1. `/users` : créer un utilisateur par rôle système (`DIRECTOR`, `ACCOUNTANT`, `SECRETARY`,
   `TEACHER`) — email unique par tenant, désactivation révoque les sessions.
2. `/users/roles` : créer un rôle **personnalisé** (ex. « Surveillant »), lui affecter des
   permissions précises, créer un utilisateur avec ce rôle, vérifier que **seuls** les menus
   correspondant aux permissions cochées s'affichent, et que les boutons d'action (créer/modifier/
   supprimer) apparaissent bien sur les pages autorisées.
3. Retirer une permission : le menu et les actions correspondantes disparaissent immédiatement à la
   reconnexion.
4. Rôles système (`SUPER_ADMIN`, etc.) : vérifier qu'ils restent non supprimables.

*Résultat attendu* : aucun rôle n'est câblé en dur dans le code — la sidebar et les actions suivent
uniquement les permissions affectées.

---

## Phase 4 — Personnel enseignant

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR`, routes `/teachers/*`.

1. `/teachers` : créer un enseignant (matricule auto-généré unique), avec ou sans compte
   `Utilisateur` associé.
2. `/teachers/staff` : créer un membre du personnel administratif.
3. Sur la fiche enseignant (`/teachers/[id]`) : vérifier les onglets **classes**, **matières**,
   **emploi du temps consolidé** (noms de classes affichés, plus d'identifiants bruts) — puis bouton
   **Imprimer / Télécharger la fiche** : ouvre un document paysage avec les infos de l'enseignant,
   la liste des classes/matières enseignées, et la grille hebdomadaire complète (toutes classes
   confondues, y compris les heures creuses avec leur libellé).
4. Tenter de supprimer un enseignant déjà lié à des notes/évaluations : doit basculer en
   **désactivation** plutôt qu'une suppression physique.
5. `/my-classes` et `/dashboard/enseignant` (connecté en `TEACHER`) : classes assignées, prochains
   cours, évaluations à corriger, taux de saisie des notes.

*Résultat attendu* : le personnel est géré sans jamais bloquer sur des données historiques
(notes/évaluations) déjà rattachées.

---

## Phase 5 — Élèves, parents & inscriptions

**Rôle** : `SCHOOL_ADMIN` / `SECRETARY`.

1. `/students` → créer un élève : matricule auto-généré, upload photo, documents administratifs
   (acte de naissance, certificat médical...). Créer volontairement des élèves jusqu'à dépasser la
   limite du plan (`STARTER` ≤ 500) : la création doit être refusée en **403** au-delà du quota.
2. `/students` → lier un ou plusieurs **parents** (lien PÈRE/MÈRE/TUTEUR), avec ou sans compte
   `Utilisateur` (accès portail parent).
3. `/enrollments` : inscrire l'élève dans une classe pour l'année courante — **cocher ou non** la
   case **accès plateforme** (frais annexe optionnel, §6 du plan RBAC). Vérifier le refus si la
   classe a atteint sa `capacite_max`.
4. `/enrollments/movements` et `/enrollments/overview` : suivi des inscriptions en attente/validées,
   tableau de la rentrée.
5. Valider l'inscription : vérifie que `billing` génère automatiquement la facture de frais
   d'inscription et que l'effectif de la classe (`/classes/[id]` effectif vs capacité) augmente.
6. `/enrollments` → réinscription : réinscrire un élève sur l'année suivante ; une seule inscription
   `VALIDEE` par élève et par année doit être imposée.
7. `/children` (connecté en `PARENT`) : le parent voit exactement les enfants qui lui sont rattachés,
   jamais ceux d'une autre famille.

*Résultat attendu* : le quota d'élèves du plan est réellement bloquant, et le frais annexe de
plateforme reste un choix explicite par élève, jamais imposé.

---

## Phase 6 — Présences (élèves & enseignants)

**Rôle** : `SECRETARY`/`TEACHER` (saisie), `SCHOOL_ADMIN` (réglages).

1. `/attendance` : appel de classe en masse (présent/absent/retard + motif) pour une classe/date.
   Vérifier la contrainte d'unicité (une présence par élève/classe/date).
2. Déclencher une absence : le(s) parent(s) reçoivent une notification (voir Phase 9 pour le
   filtrage par accès plateforme) et `notifie_parent` passe à `true`.
3. `/attendance/scan` : pointage par QR code (élèves et enseignants), y compris le **badge de
   retrait obligatoire** en Maternelle/Primaire/**Garderie** (§2 du plan RBAC).
4. Onglet **Réglages** (`SCHOOL_ADMIN`/`DIRECTOR` uniquement) : marge de transition entre salles,
   fenêtres de récréation — dérouler le script détaillé du §3 du plan RBAC (retard/départ anticipé).
5. `/attendance` → **Présences enseignants** : table séparée des présences élèves (jamais fusionnée),
   récap mensuel avec retard cumulé et départ anticipé cumulé.
6. `/eleves/{id}/presences/statistiques` (widget sur la fiche élève) : taux d'assiduité par période.

*Résultat attendu* : présences élèves et enseignants restent deux circuits distincts, avec un calcul
d'heures enseignant qui tient compte des marges/récréations paramétrées par l'école.

---

## Phase 7 — Notes, examens & bulletins

**Rôle** : `TEACHER` (saisie), `RESPONSABLE_PEDAGOGIQUE`/`DIRECTOR` (validation/publication).

1. `/grading` : créer une évaluation (classe, matière, période, type, note sur X, coefficient) —
   uniquement sur les classes/matières que l'enseignant enseigne réellement.
2. Saisir des notes en masse pour la classe ; tenter une valeur hors barème (ex. 25/20) → refus
   **422**. Valider les notes en bloc : elles se verrouillent (déverrouillage nécessite un rôle
   autorisé).
3. Créer un examen (`BLANC`/`SEMESTRIEL`/`ANNUEL`/`NATIONAL`), générer la convocation PDF.
4. `/academic-years` → clôturer une période : vérifie que la saisie de notes devient impossible sur
   cette période, et déclenche automatiquement la génération des bulletins
   (`PeriodeClôturee → BulletinGenere`).
5. `/grading/report-cards` : générer les bulletins d'une classe/période en lot — moyenne pondérée par
   coefficient de matière puis d'évaluation, rang par classe et par matière, mention/décision selon
   le barème du tenant (`/settings`).
6. `/grading/validation` : conseil de classe, publication des bulletins (`BROUILLON/GENERE → PUBLIE`).
   Un bulletin publié devient visible côté portail élève/parent (`/my-results`, dashboard parent) et
   déclenche la notification (filtrée par accès plateforme, voir Phase 9).

*Résultat attendu* : impossible de saisir une note hors période ouverte ; le calcul de moyenne/rang/
mention est cohérent avec le barème propre au tenant.

---

## Phase 8 — Frais scolaires, factures & paiements

**Rôle** : `ACCOUNTANT`/`SCHOOL_ADMIN` (paramétrage/encaissement), `PARENT`/`STUDENT` (paiement
self-service).

1. `/invoices` (page **Frais scolaires**, `features/billing/components/fees-page.tsx`) : définir un
   frais par classe/type (`INSCRIPTION`, `RÉINSCRIPTION`, `MENSUALITÉ`, `TRANSPORT`, `CANTINE`,
   `EXAMEN`, `OTHER`), avec ses échéances (tranches) — la somme des tranches doit égaler le montant
   total du frais, sinon refus **422**.
2. `/invoices` : générer les factures d'une classe en lot ; vérifier `solde_restant` recalculé après
   chaque paiement, et le statut dérivé (`IMPAYEE → PARTIELLE → PAYEE`).
3. Une échéance dépassée non payée déclenche une relance automatique (`EcheanceDepassee`, job
   planifié) — vérifier le SMS/email de relance (filtré par accès plateforme, Phase 9).
4. `/guichet` : encaissement au guichet (espèces, Orange Money manuel, virement) — reçu généré
   immédiatement.
5. Paiement en ligne self-service (parent) : depuis la fiche facture ou depuis `/courses` (achat de
   cours, Phase 10), payer via Djomy — mobile money (Orange/MTN/Wave) ou carte, suivre la
   redirection/confirmation, télécharger le reçu PDF.
6. `/payments` → **Transactions** : traçabilité de chaque appel fournisseur (`payload_reponse` brut).
7. Rembourser un paiement (`ACCOUNTANT`) : ajustement de la facture + reçu d'avoir généré.
8. `/dashboard/finance` : encaissements par canal/période, soldes restants, top débiteurs, échéances
   à venir.

*Résultat attendu* : le cycle facture → paiement → reçu fonctionne pour tous les canaux (guichet et
self-service), avec des soldes toujours exacts après remboursement.

---

## Phase 9 — Communication & webhooks

**Rôle** : `SCHOOL_ADMIN`/`SECRETARY` (envoi), tous rôles côté réception.

Dérouler intégralement le §9 du plan RBAC (SMS filtrés par accès plateforme) et le §10 (rappel
NimbaSMS). Compléments :

1. `/communications` : historique des envois, statut de livraison par destinataire (`EN_ATTENTE →
   LIVRE | ECHOUE`), mis à jour par le webhook Nimba une fois configuré.
2. `/communications/templates` : modèles de message par motif avec variables (`{{nom_eleve}}`,
   `{{montant}}`...).
3. `POST /api/v1/webhooks/paiement/{fournisseur}` : vérifier que tout callback sans signature valide
   est rejeté, et qu'un même événement fournisseur n'est traité qu'une seule fois (idempotence).
4. Webhooks sortants (`WebhookEndpoint`) : configurer une URL tierce abonnée à `PAIEMENT_RECU`, la
   déclencher (encaisser un paiement), vérifier la réception côté tiers puis rejouer manuellement un
   envoi échoué.

*Résultat attendu* : aucune notification (SMS/webhook sortant) ne part pour un élève n'ayant pas
accepté l'accès plateforme ; les webhooks entrants sont protégés par signature et idempotents.

---

## Phase 10 — Cours en ligne payants

Dérouler intégralement le §11 du plan RBAC (création, publication, achat, déverrouillage,
protection anti-téléchargement, suppression bloquée si déjà acheté).

---

## Phase 11 — Tableaux de bord par rôle

1. `/dashboard` (`SCHOOL_ADMIN`/`DIRECTOR`) : effectifs, taux d'assiduité global, taux de
   recouvrement des frais, nb bulletins publiés, alertes (factures impayées, quota proche,
   échéances).
2. `/dashboard/finance` (`ACCOUNTANT`) : voir Phase 8.
3. `/dashboard/enseignant` (`TEACHER`) : voir Phase 4.
4. Dashboard parent (widget d'accueil connecté en `PARENT`) : enfants, dernières notes, absences,
   factures impayées, prochains examens — strictement scopé aux enfants rattachés.
5. `/founder` (Vision Fondateur, si applicable) et `/reports` (rapports analytiques) : réservés aux
   rôles système, vérifier qu'ils restent invisibles pour un rôle personnalisé sans permission
   correspondante (limite connue documentée au §8 du plan RBAC).
6. `/platform/dashboard` : voir Phase 1.

*Résultat attendu* : chaque dashboard reste filtré par rôle + tenant + portée (un parent ne voit
jamais que ses enfants, un comptable jamais les notes, etc.).

---

## Phase 12 — Audit, imports/exports & recherche

1. `/settings` ou équivalent **Audit** (`ADMIN`/`DIRECTOR`) : toute écriture sensible (connexion,
   suspension de tenant, changement de rôle, paiement, remboursement, publication de bulletin) doit
   apparaître dans le journal, de façon immuable (jamais modifiable/supprimable depuis l'API).
2. `/students` → **Import** : importer un fichier CSV/Excel d'élèves — dry-run avec rapport d'erreurs
   ligne par ligne avant tout commit ; respecter le quota du plan à l'import.
3. Exports CSV/XLSX : élèves, notes, paiements, présences ; export ZIP des bulletins PDF d'une
   classe/période.
4. Barre de recherche globale : rechercher un élève/parent/enseignant/classe/facture par nom — les
   résultats restent scopés au tenant et filtrés par permissions de l'utilisateur connecté.

*Résultat attendu* : aucune donnée sensible n'est exposée hors du tenant ou des permissions de
l'utilisateur, et l'import respecte les mêmes règles de quota que la création manuelle.

---

## Phase 13 — Portail parent / élève (vue d'ensemble transverse)

**Rôle** : `PARENT`, `STUDENT`.

1. Connexion par email **ou téléphone** (§5 du plan RBAC).
2. `/children` (parent) / `/my-results` (élève) : notes, bulletins publiés, absences, factures
   impayées, prochains examens — jamais les données d'un autre élève.
3. `/courses` (Mon espace) : catalogue, achat, contenu déverrouillé après paiement (Phase 10).
4. Notifications reçues (SMS/email) : uniquement pour les élèves ayant accepté l'accès plateforme
   (Phase 9) — un parent avec deux enfants (un avec accès, un sans) ne doit recevoir que les
   notifications du premier.
5. Téléchargement des reçus de paiement et factures PDF depuis l'espace parent.

*Résultat attendu* : le portail parent/élève est strictement en lecture (sauf achat de cours et
paiement self-service), scopé aux seuls enfants rattachés au compte connecté.

---

## Annexe — Matrice rapide rôle × accès attendu

| Rôle | Accès notable | Ne doit jamais voir |
|---|---|---|
| `SUPER_ADMIN` | Tout `/platform/*`, impersonation tenant | — |
| `SCHOOL_ADMIN` | Tout le tenant, `/users/roles`, `/settings` | Autres tenants, `/platform/*` |
| `DIRECTOR` | Comme SCHOOL_ADMIN sauf gestion des rôles/plateforme | `/platform/*`, `/users/roles` (sauf permission) |
| `ACCOUNTANT` | `/invoices`, `/payments`, `/dashboard/finance`, `/guichet` | Notes, bulletins, présences |
| `SECRETARY` | `/students`, `/enrollments`, `/guichet`, `/attendance` (saisie) | Paie, budget, comptabilité générale |
| `TEACHER` | `/my-classes`, `/grading` (ses classes), `/lessons`, `/assignments` | Finances, autres classes |
| `RESPONSABLE_PEDAGOGIQUE` | `/grading/validation`, annonces | Finances, RH |
| Rôle personnalisé | Exactement les permissions cochées (§7-8 plan RBAC) | Tout le reste |
| `PARENT` | `/children`, `/courses`, notifications de ses enfants opt-in | Élèves non rattachés, tout écran de gestion |
| `STUDENT` | `/my-results`, `/courses` | Données d'autres élèves, tout écran de gestion |
| `DPE_ADMIN` | `/dpe/*` (lecture, écoles du périmètre) | Écriture sur les écoles, autres préfectures |
