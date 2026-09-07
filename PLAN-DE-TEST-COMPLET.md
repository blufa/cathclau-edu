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
5. **SMS d'identifiants** : créer un utilisateur (n'importe quel rôle : comptable, secrétaire,
   parent...) en renseignant un numéro de téléphone — un SMS doit arriver automatiquement avec
   l'identifiant et le mot de passe (temporaire ou choisi). Créer un utilisateur **sans** téléphone :
   aucun SMS ne doit être tenté (pas d'erreur non plus). Personnaliser le message depuis
   `/communications/templates` (motif « Compte créé ») et vérifier que le SMS suivant reprend le
   modèle personnalisé.

*Résultat attendu* : aucun rôle n'est câblé en dur dans le code — la sidebar et les actions suivent
uniquement les permissions affectées ; tout nouveau compte avec un téléphone reçoit ses accès sans
action manuelle du personnel.

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
8. `/student-cards` → **Émettre une carte** pour un élève déjà inscrit dans une classe : ouvrir
   l'aperçu du badge — la **classe de l'élève** doit apparaître (sous le nom), pas seulement le
   matricule/n° de carte. Cliquer **Imprimer / Télécharger en PDF** : dans l'aperçu avant impression,
   le QR code doit rester à la même position/taille relative que sur les autres éléments de la carte
   (recto + verso), sans décalage visuel par rapport à l'aperçu écran.
9. `/enrollments` (en ligne) : ouvrir la page une première fois normalement — cela prépare la liste
   des classes pour l'inscription hors-ligne (voir Phase 14).

*Résultat attendu* : le quota d'élèves du plan est réellement bloquant, le frais annexe de plateforme
reste un choix explicite par élève, jamais imposé, et le badge élève imprimé reflète fidèlement sa
classe et son QR tels qu'affichés à l'écran.

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

## Phase 14 — Mode hors-ligne

**Rôle** : `SCHOOL_ADMIN` / `SECRETARY` (saisie terrain sans connexion).

**Prérequis** : se connecter au moins une fois **avec réseau** — aucun compte ne peut se connecter
pour la toute première fois sans connexion (mot de passe jamais stocké en clair sur l'appareil,
choix de sécurité assumé). Une fois connecté, l'app reste utilisable hors-ligne.

1. **Rester connecté hors-ligne** : se connecter avec réseau, couper le réseau (mode avion), puis
   recharger complètement la page/l'app. La session doit rester active (pas de retour à l'écran de
   connexion) — vérifier que la bannière hors-ligne s'affiche, mais que le profil (nom, rôle,
   permissions) reste correct. Reconnecter le réseau : un vrai jeton expiré/révoqué doit, lui,
   toujours déconnecter normalement.
2. **Classes disponibles hors-ligne à l'inscription** : en ligne, ouvrir `/enrollments` au moins une
   fois pour l'année scolaire courante (pré-charge le cache). Couper le réseau, créer un élève
   hors-ligne (`/students` → le formulaire simplifié) : à l'étape « classe », la liste doit
   apparaître normalement (année + classes), pas le message « liste indisponible hors-ligne ».
   Sans être jamais passé par `/enrollments` en ligne au préalable pour cette année : le message de
   repli doit s'afficher clairement, avec possibilité d'« Inscrire plus tard ».
3. **Files d'attente existantes** (à survoler, déjà en place) : créer un élève, un enseignant, lier
   un parent, et encaisser un paiement groupé, tous hors-ligne — chacun doit se mettre en file
   (badge de synchronisation en attente visible) puis se synchroniser automatiquement au retour du
   réseau, sans doublon (clé d'idempotence).

*Résultat attendu* : une fois connecté en ligne au moins une fois, le personnel de terrain peut
continuer à travailler sans réseau (consultation des référentiels déjà visités + saisie en file
d'attente), sans jamais être déconnecté de force par une simple coupure réseau.

---

## Phase 15 — Correctifs septembre 2026 (documents, connexion, référentiels, personnel, site public)

### 15.1 En-tête/pied de page administratifs sur tous les PDF

**Rôle** : `SCHOOL_ADMIN`.

1. `/settings` → renseigner « Rattachement administratif » (IRE, DPE, sous-préfecture).
2. Générer chacun de ces documents et vérifier la présence du bandeau MEPU-A/RÉPUBLIQUE DE GUINÉE
   + devise, et du pied de page (coordonnées école) : bulletin (`/grading/report-cards`), reçu de
   paiement, facture (`/invoices`), fiche de paie (`/payroll`), certificat de scolarité
   (`/students/{id}` → Documents), convocation d'examen, palmarès de classe, centralisateur.

*Résultat attendu* : même en-tête/pied de page administratif sur tous les documents générés.

### 15.2 Connexion par téléphone pour tous les rôles

**Rôle** : tous, y compris `SCHOOL_ADMIN`.

1. Se connecter sur `/login` avec le **numéro de téléphone** (au lieu de l'email) pour un compte
   `SCHOOL_ADMIN` créé via `/platform/tenants` (onboarding SUPER_ADMIN) — doit fonctionner.
2. Si le compte a été créé avant ce correctif et n'a pas de téléphone enregistré : se connecter par
   email, puis `/account` → renseigner le téléphone soi-même, puis retester la connexion par
   téléphone.

*Résultat attendu* : tout utilisateur avec un téléphone en base peut se connecter avec.

### 15.3 Email de réinitialisation de mot de passe

**Rôle** : tous.

1. `/forgot-password` → saisir l'email → un email (motif `PASSWORD_RESET`) doit être reçu avec le
   lien de réinitialisation, que la plateforme soit en cours d'utilisation ou non par un admin.

*Résultat attendu* : l'email de réinitialisation part systématiquement (avant ce correctif, aucun
canal n'était déclenché).

### 15.4 Référentiels : salles, cycle Garderie, niveaux

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR`.

1. `/referentials` → Salles : modifier une salle existante (libellé) → elle doit rester à sa
   position d'origine dans la liste, pas repasser en dernière position.
2. Pour une école déclarant le type d'établissement « Garderie » (`/settings` ou à l'onboarding) :
   créer une classe de cycle Garderie sur `/referentials` → Classes — ne doit plus être bloqué.
3. `/referentials` → Niveaux : vérifier que Petite/Moyenne/Grande Section sont rattachées au cycle
   Maternelle, et CI/CP/CE1/CE2/CM1/CM2 au cycle Primaire (pas des cycles à part entière).

*Résultat attendu* : tri stable des salles, création de classes Garderie possible, modèle
cycle/niveau cohérent.

### 15.5 Création d'élève avec accès portail

**Rôle** : `SCHOOL_ADMIN` / `SECRETARY`.

1. `/students` → créer un élève en cochant la création d'un compte d'accès (identifiant/mot de
   passe) → doit aboutir sans erreur 500 générique ; en cas d'échec, le message doit maintenant
   nommer l'identifiant en cause.

*Résultat attendu* : création fiable, ou message d'erreur exploitable en cas de doublon.

### 15.6 Création d'enseignant simplifiée + personnel pouvant enseigner

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR`.

1. `/teachers` → « Nouvel enseignant » : le formulaire ne comporte plus que 3 étapes (Identité,
   Professionnel, Compte) — plus d'étape « Affectations ». Après création, l'enseignant apparaît
   directement dans la liste ; les affectations (niveaux, classes/matières) se font depuis sa fiche
   (`/teachers/{id}`).
2. `/teachers` → onglet **Personnel** : sur un membre du personnel (administratif/direction/
   soutien), cliquer « Devenir enseignant » → le formulaire de création d'enseignant s'ouvre
   pré-rempli (nom, téléphone, email) → valider crée une fiche enseignant distincte, affectable
   ensuite comme n'importe quel enseignant. Le salaire de base du personnel et la rémunération des
   heures de cours restent deux fiches de paie séparées (`/payroll`).

*Résultat attendu* : création d'enseignant en une passe sans affectation forcée ; un membre du
personnel qui donne aussi des heures de cours peut être affecté à une classe/matière via une fiche
enseignant liée, sans perdre sa fiche personnel/paie de base.

### 15.7 Frais plateforme : cohérence facture/règlement

**Rôle** : `SCHOOL_ADMIN` / `ACCOUNTANT`.

1. Inscrire un élève avec l'option d'accès plateforme activée, générer sa facture de mensualité
   (génération unitaire **ou** en lot `/invoices/generer-lot`) → le reçu de paiement doit afficher
   la ligne « Dont frais d'usage plateforme ».
2. `/reports` → carte de règlement des frais plateforme : après un encaissement réel avec facture,
   le bouton de règlement ne doit plus rester grisé.

*Résultat attendu* : le frais plateforme s'applique quel que soit le chemin de génération de
facture (plus seulement la génération mensuelle automatique).

### 15.8 Clarté du tableau de performance enseignants et fiche de paie

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR`.

1. `/reports` → section Performance enseignants : survoler l'icône d'aide à côté de « Indice global
   (/100) » → l'infobulle explique le calcul (50 % charge, 30 % assiduité, 20 % ponctualité).
2. `/teachers/{id}` → carte « Fiches de paie » : lister les fiches existantes et télécharger le PDF
   directement depuis la fiche enseignant.

### 15.9 Filtre « accès plateforme » sur les inscriptions

**Rôle** : `SCHOOL_ADMIN` / `SECRETARY` / `ACCOUNTANT`.

1. `/enrollments` → utiliser le filtre « Accès plateforme » (Tous / Activé / Non activé) pour ne
   voir que les inscriptions correspondantes.

### 15.10 Grille d'emploi du temps en bandes d'1h

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR`.

1. `/timetable` → sélectionner une classe : la grille affiche désormais des bandes d'1h (au lieu de
   2h), de 08h à 18h (au-delà si des créneaux existants débordent).
2. Affecter une matière sur une bande d'1h (ex. 08h-09h) → laisser l'heure de fin par défaut →
   occupe une seule bande.
3. Affecter une matière sur 2h (ex. 08h-10h, en modifiant l'heure de fin dans la cellule) →
   enregistrer → la cellule doit fusionner visuellement les deux bandes (08h-09h et 09h-10h), sans
   laisser la bande 09h-10h apparaître vide/disponible pour un autre cours.
4. Vérifier qu'un conflit (même enseignant/salle/classe sur un créneau chevauchant) est toujours
   refusé.

*Résultat attendu* : saisie plus fine à l'heure, séances multi-heures fusionnées sur la grille,
plage horaire étendue jusqu'à 18h.

### 15.11 Formulaire élève simplifié + enchaînement inscription → frais

**Rôle** : `SCHOOL_ADMIN` / `SECRETARY`.

1. `/students` → « Nouvel élève » : le champ « Point de repère » n'apparaît plus (seuls Adresse et
   Téléphone restent dans « Coordonnées »).
2. Créer un élève → à la validation, le formulaire d'inscription (`EnrollModal`) s'ouvre
   automatiquement.
3. Inscrire l'élève dans une classe → dès la validation, la fenêtre d'encaissement s'ouvre
   automatiquement (sans clic supplémentaire) et affiche déjà : la facture d'inscription (générée
   automatiquement à la création de l'inscription) **et** les tranches de mensualité (générées
   automatiquement à l'ouverture de cette fenêtre, sans bouton « Générer les tranches » à cliquer).
4. Vérifier qu'ouvrir à nouveau cette fenêtre plus tard (depuis `/enrollments` → « Facture ») ne
   régénère pas de doublons (message « déjà générées » si on force la génération manuelle restée
   disponible en repli).

*Résultat attendu* : parcours continu création élève → inscription → frais prêts à encaisser, sans
étape de génération manuelle pour la scolarité.

### 15.12 Site public simplifié et mobile-stable

**Rôle** : visiteur non connecté.

1. Ouvrir `/` (accueil) sur mobile (largeur ≤ 400px) : en-tête simple (bandeau coordonnées + barre
   bleue avec menu hamburger), pas de carrousel plein écran, pas de saut de mise en page au
   chargement (padding/marge stables).
2. Faire défiler jusqu'au pied de page : colonnes de liens compactes, pas de long bloc Bootstrap.
3. Parcourir `/a-propos`, `/tarifs`, `/faqs`, `/aide`, `/contact`, `/signup`, `/blog`,
   `/fonctionnalites` : même en-tête/pied de page simple partout ; plus de témoignages fictifs sur
   `/` , `/a-propos` et `/faqs`.

*Résultat attendu* : navigation publique simple, cohérente entre pages, stable sur mobile, sans
contenu inventé (témoignages/statistiques fictifs retirés).

---

## Phase 16 — Paie enseignant (retard/départ anticipé × classes normales/examen) et présences hors-ligne

### 16.1 Paie : retard + départ anticipé combinés au tarif normal/examen

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR`.

**Contexte** : un enseignant qui donne cours à la fois en classe normale et en classe d'examen (deux
tarifs horaires différents, `/payroll` → fiche enseignant → « Rémunération ») doit être payé au bon
tarif sur chaque partie, retard et départ anticipé cumulés déduits de la bonne typologie de classe.

1. Configurer un enseignant avec un tarif « classe normale » et un tarif « classe d'examen »
   différents (`/payroll` → sélectionner l'enseignant → Rémunération).
2. Vérifier qu'il a des séances programmées à la fois sur une classe normale et une classe d'examen
   (`/timetable`).
3. Faire pointer (scan ou saisie manuelle) un retard sur une séance de classe d'examen, et un départ
   anticipé sur une séance de classe normale, pour le mois en cours.
4. Marquer les séances correspondantes « réalisées » dans le cahier de textes (`/lessons`).
5. `/payroll` → sélectionner l'enseignant, le mois → carte « Paie du mois » : vérifier « Retard
   cumulé », « Départ anticipé cumulé », et la répartition « Dont classes normales »/« Dont classes
   d'examen » des heures effectives — le retard doit être déduit des heures d'examen, le départ
   anticipé des heures normales, chacune payée à son tarif.
6. Générer la fiche de paie (« Générer la fiche de paie ») et vérifier la note du bulletin détaille
   bien la répartition normal/examen des heures effectives.

*Résultat attendu* : aucun tarif appliqué à tort (plus de repli uniforme sur le tarif normal quand
un enseignant a aussi des heures d'examen), retard et départ anticipé toujours déduits.

### 16.2 Pointage manuel de sortie enseignant (badge oublié)

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR` / `SECRETARY`.

1. `/attendance` → onglet enseignants → un enseignant sans pointage de sortie du jour → bouton
   « Sortie » → saisir une heure avant la fin prévue de sa séance → « OK ».
2. Vérifier la colonne « Départ anticipé » se met à jour, et que la correction est bien prise en
   compte dans `/payroll` (voir 16.1).

*Résultat attendu* : la sortie peut être corrigée manuellement exactement comme l'entrée, sans
attendre un scan de badge.

### 16.3 Présences hors-ligne (scan et saisie manuelle)

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR` / `SECRETARY` (+ élève chef de classe pour le scan).

**Prérequis** : s'être connecté au moins une fois en ligne (§Phase 14).

1. Couper le réseau. `/attendance/scan` : scanner un badge élève ou enseignant (entrée ou sortie) —
   doit s'enregistrer localement (« Scan enregistré hors-ligne — sera synchronisé automatiquement »),
   sans blocage ni erreur.
2. Toujours hors-ligne, `/attendance` → appel manuel (élève ou enseignant), « Présent »/« Sortie » →
   doit s'enregistrer localement avec le même message de confirmation adapté.
3. Vérifier le bandeau hors-ligne global affiche bien les présences dans son compteur « en attente de
   synchronisation ».
4. Reconnecter le réseau : la file doit se rejouer automatiquement (bandeau qui se vide), dans l'ordre
   d'origine (une entrée avant sa sortie sur la même séance).
5. Provoquer un échec métier (ex. scanner un enseignant qui n'a aucune séance programmée ce jour-là)
   pendant que le réseau est coupé, puis reconnecter : l'entrée doit apparaître en échec dans le
   bandeau (pas silencieusement perdue), avec bouton « Réessayer »/« Ignorer ».

*Résultat attendu* : le scan et la saisie manuelle des présences fonctionnent hors-ligne exactement
comme les élèves/enseignants/paiements/parents, avec synchronisation automatique et fiable au retour
du réseau. Note : « Marquer absent » reste une action en ligne uniquement (dépend du jour courant,
non rejouable a posteriori sans risque de décalage de date).

---

## Phase 17 — Présence élève par cours, contacts parent et frais en mensualités égales

### 17.1 Pointage élève par cours (entrée/sortie), y compris manuel

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR` / `SECRETARY` (+ élève chef de classe pour le scan).

**Contexte** : chaque cours de l'emploi du temps d'un élève génère son propre pointage (comme pour
les enseignants) — un élève avec 6 cours dans la journée a jusqu'à 6 pointages d'entrée/sortie.

1. `/attendance/scan` : scanner un élève en entrée puis en sortie sur un cours → vérifier via
   `/attendance` que le retard et le départ anticipé apparaissent bien pour ce cours précis.
2. `/attendance` → onglet élèves (log de présence) : sur un élève sans pointage de sortie du jour →
   bouton « Sortie » → saisir une heure avant la fin prévue du cours → « OK ». Fonctionne même si
   l'élève n'a jamais été pointé en entrée (badge oublié toute la séance).
3. Vérifier que ce pointage manuel de sortie fonctionne aussi hors-ligne (voir §16.3), avec
   synchronisation automatique au retour du réseau.

*Résultat attendu* : entrée ET sortie corrigeables manuellement pour un élève, par cours, en ligne
comme hors-ligne — plus seulement pour les enseignants.

### 17.2 Dashboard parent : heures d'entrée/sortie par cours

**Rôle** : `PARENT`.

1. `/children` → sélectionner un enfant → section « Présences du jour — entrée/sortie par cours » :
   vérifier l'affichage de chaque cours du jour avec heure d'entrée, retard éventuel, heure de sortie
   et départ anticipé éventuel.
2. Changer la date (sélecteur) → vérifier que les pointages d'un autre jour s'affichent correctement.
3. Vérifier qu'un parent ne voit **jamais** les pointages d'un élève qui n'est pas son enfant (accès
   scopé, comme le reste du portail).

*Résultat attendu* : le parent sait précisément à quelle heure son enfant est entré/sorti de chaque
cours, pas seulement un taux d'assiduité global.

### 17.3 Dashboard parent : contacts (enseignants de la classe + direction)

**Rôle** : `PARENT`.

1. `/children` → sélectionner un enfant → carte « Contacts de l'établissement » : vérifier la liste
   des enseignants de sa classe (une ligne par enseignant, matières concaténées si plusieurs), avec
   bouton d'appel direct (`tel:`) quand le téléphone est renseigné.
2. Vérifier la section « Direction de l'école » : directeur et responsable pédagogique du tenant, avec
   téléphone.
3. Avec plusieurs enfants dans des classes différentes : changer d'enfant sélectionné → la liste des
   enseignants doit changer en conséquence (propre à la classe de l'enfant affiché).

*Résultat attendu* : le parent peut joindre directement, par téléphone, chaque enseignant de la
classe de son enfant, ainsi que la direction de l'école.

### 17.4 Frais : génération rapide de mensualités égales

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR` / `ACCOUNTANT`.

1. `/invoices/fees` → « Nouveau frais » → type Mensualité → choisir le mois de départ de l'année
   scolaire, saisir un montant mensuel, cliquer « Proposer 9 mensualités égales » → vérifier que 9
   tranches (une par mois) apparaissent automatiquement, chacune au montant saisi — sans avoir à
   cocher les mois une par une.
2. Ajuster librement une tranche proposée (fusionner deux mois, changer un montant) avant de
   confirmer la création — toujours possible après la génération rapide.
3. Sur un frais déjà existant (« Échéances » → panneau d'échéances) : même bouton « Proposer N
   mensualités égales » disponible pour ajouter des tranches manquantes.

*Résultat attendu* : un cas standard (mensualités égales) se configure en un clic, sans perdre la
possibilité de composer des tranches personnalisées pour les cas particuliers.

---

## Phase 18 — Présence du personnel non-enseignant (nouveau)

**Contexte** : le personnel administratif, direction et soutien (`Employee`, distinct des
enseignants) n'avait jusqu'ici aucun suivi de présence. Nouveau système, miroir simplifié de celui
des enseignants : **un seul pointage par jour** (pas de créneau de cours par individu), retard/départ
anticipé calculés vs un horaire de référence unique configuré par l'école.

### 18.1 Réglage de l'horaire du personnel

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR`.

1. `/attendance` → onglet Réglages → bloc « Horaire de référence du personnel » : définir une heure
   d'entrée et de sortie (ex. 08h00–17h00) → Enregistrer.
2. Rouvrir la page : les valeurs enregistrées doivent persister (pas de retour aux 08h00/17h00 par
   défaut si elles ont été changées).
3. Vérifier que modifier la marge de transition (bloc juste au-dessus, propre aux enseignants) ne
   réinitialise pas l'horaire du personnel, et inversement (les deux réglages sont indépendants).

### 18.2 Scan et saisie manuelle du personnel

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR` / `SECRETARY`.

1. `/attendance/scan` → sélecteur « Personnel » (à côté d'Élève/Enseignant) → choisir un membre du
   personnel dans le pointage manuel → Pointer (Entrée) → vérifier le retard calculé vs l'heure
   d'entrée configurée.
2. Sens Sortie → vérifier le départ anticipé calculé vs l'heure de sortie configurée.
3. Scanner le badge imprimé d'un membre du personnel (badge généré depuis `/teachers` → onglet
   Personnel → « Badge ») : doit être reconnu par matricule, comme pour un élève/enseignant.
4. `/attendance` → onglet « Présences personnel » : appel manuel avec boutons « Présent »/« Sortie »/
   « Absent », mêmes règles que les enseignants (badge oublié → saisie manuelle possible pour
   l'entrée ET la sortie).
5. Récap mensuel (même onglet, bloc du bas) : sélectionner un membre du personnel et un mois →
   vérifier jours présents/absents, retard et départ anticipé cumulés, détail jour par jour.

### 18.3 Hors-ligne

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR` / `SECRETARY`.

1. Couper le réseau → scanner ou saisir manuellement une présence de personnel (entrée ou sortie) →
   doit s'enregistrer localement avec le message « … hors-ligne — sera synchronisé automatiquement »,
   comme pour élèves/enseignants.
2. Reconnecter le réseau → vérifier la synchronisation automatique (bandeau hors-ligne qui se vide).

*Résultat attendu* : le personnel non-enseignant bénéficie du même niveau de suivi de présence
(scan, saisie manuelle entrée/sortie, hors-ligne) que les enseignants, avec un modèle adapté à
l'absence de créneaux de cours individuels.

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
