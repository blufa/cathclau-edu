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

## Phase 19 — Classes d'examen (assistant + liste) et paie pendant les vacances

### 19.1 Assistant de création de classes — fenêtre qui ne s'élargit plus

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR`.

1. `Référentiels → Classes → Assistant (en lot)` → cocher de nombreux grades/sections → vérifier que
   la fenêtre garde une hauteur fixe avec défilement interne, au lieu de pousser toute la page vers
   le bas à chaque case cochée.

### 19.2 Marquer une classe « examen » — à la création en lot et sur des classes existantes

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR`.

1. Assistant (en lot) : après avoir coché des grades (ex. CM2, 10ème, Terminale), une section
   « Classes d'examen » liste ces grades avec une case à cocher chacun → cocher les grades concernés
   → créer → vérifier que les classes générées (toutes sections/séries) portent bien la typologie
   « Classe d'examen ».
2. Liste des classes (déjà créées) : colonne « Typologie » → cliquer sur le badge « Normale »/« Classe
   d'examen » pour basculer directement, sans ouvrir le formulaire complet.
3. Formulaire classe (Modifier) : la case à cocher « Classe d'examen (CM2, BEPC/10ème, Terminale…) »
   remplace l'ancien menu déroulant.

### 19.3 Répercussion sur la paie

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR`.

1. Affecter un enseignant à une classe normale, générer/consulter sa paie du mois → vérifier les
   heures dans « classes normales ».
2. Basculer cette classe en « Classe d'examen » (§19.2, sans toucher à l'affectation de l'enseignant)
   → recharger la paie de ce même enseignant → les heures doivent maintenant apparaître dans
   « Heures classes d'examen », sans aucune autre action.

### 19.4 Paie pendant les vacances — mois précis au lieu d'un interrupteur global

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR`.

**Contexte** : l'ancien interrupteur « payé pendant les vacances » (oui/non pour toute l'année) est
remplacé par une liste de mois précis (ex. juillet 2026), ajoutés/retirés au cas par cas.

1. `/payroll` → sélectionner un enseignant → bloc « Mois payés pendant les vacances » → ajouter un
   mois (sélecteur année/mois) → vérifier qu'il apparaît dans la liste avec un bouton de retrait.
2. Calculer la paie théorique (emploi du temps) de ce mois précis → vérifier que les jours fériés/
   vacances de ce mois ne sont **pas** déduits des heures dues (comme s'il avait travaillé).
3. Un autre mois de vacances, **non** ajouté à la liste → vérifier que les jours fériés restent bien
   exclus normalement.
4. Retirer le mois de la liste → recalculer → les jours fériés redeviennent exclus.
5. Vérifier que le salaire fixe (mode Fixe) reste toujours versé en entier, que le mois soit dans la
   liste ou non — seul le calcul des heures de cours théoriques est affecté.

*Résultat attendu* : le paiement pendant les vacances se décide mois par mois précis, pas par un
interrupteur valable toute l'année.

### 19.5 Jours fériés chômés payés — toujours comptés comme travaillés en paie

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR`.

**Contexte** : le calendrier distingue déjà « Jour férié » (HOLIDAY, légalement chômé ET payé, ex.
fête nationale) de « Vacances » (VACATION, école fermée mais pas automatiquement payée). Avant ce
correctif, la paie traitait les deux identiquement (déduits des heures dues) — un jour férié légal
réduisait à tort les heures dues d'un enseignant/personnel programmé ce jour-là.

1. `/calendar` → ajouter un événement type « Jour férié » sur un jour de semaine où un enseignant a
   cours (hors période de vacances scolaires).
2. `/payroll` → sélectionner cet enseignant → paie théorique (emploi du temps) du mois concerné →
   vérifier que les heures de ce jour férié sont bien comptées comme dues (pas déduites), même sans
   avoir ajouté ce mois à la liste « Mois payés pendant les vacances » (§19.4 — qui ne concerne que
   les vacances scolaires, pas les jours fériés).
3. Comparer avec un événement type « Vacances » sur une autre période → vérifier que celle-ci reste
   bien déduite des heures dues par défaut, sauf mois explicitement ajouté à la liste (§19.4).

*Résultat attendu* : un jour férié chômé payé ne réduit jamais la paie, indépendamment du statut
« payé pendant les vacances » de l'enseignant — seules les vraies vacances scolaires en dépendent.

## Phase 20 — Impression des badges/cartes (recto/verso) : le QR ne doit plus se décaler

**Rôle** : tout rôle ayant accès à `/students`, `/teachers` ou `/teachers/staff` (fiche détail avec badge).

**Contexte** : le badge imprimé (recto + verso, format carte CR80) forçait une hauteur *fixe* à
l'impression (`53.98mm`) avec les débordements masqués (`overflow-hidden`). Dès que le rendu du texte
à l'impression prenait ne serait-ce que quelques pixels de plus que prévu (variable selon
navigateur/moteur d'impression), la dernière ligne de la carte (QR + note de pied) se retrouvait
compressée/rognée — visuellement le QR « descendait » et quittait sa position d'origine, même si la
carte tenait bien sur une seule page. Corrigé en remplaçant la hauteur fixe par une hauteur *minimale*
(la carte peut légèrement dépasser le format CR80 exact à l'impression, sans conséquence puisqu'elle
est destinée à être découpée à la main).

1. Ouvrir la fiche d'un élève (`/students/{id}`) ou d'un enseignant/personnel (`/teachers/{id}`,
   `/teachers/staff`) ayant une photo, un matricule et un QR généré.
2. Cliquer sur « Imprimer / Télécharger en PDF » sur le badge.
3. Dans l'aperçu d'impression (ou le PDF généré via « Enregistrer en PDF ») : vérifier que le recto et
   le verso sortent bien côte à côte sur une seule page, ET que le QR reste à sa place normale (en bas
   à gauche du recto, aligné avec la note de pied « Présenter ce badge... »), sans être décalé vers le
   bas ni rogné.
4. Vérifier aussi que le recto et le verso font la **même hauteur** à l'impression (bord inférieur
   aligné) — le recto (plus chargé : photo, identité, QR) et le verso (plus sobre) s'étirent
   maintenant l'un sur l'autre plutôt que d'avoir chacun sa propre hauteur indépendante.
5. Répéter avec un texte de nom/champ volontairement long (pour maximiser le risque de débordement de
   la ligne d'en-tête/identité) et confirmer que le QR ne bouge toujours pas et que les deux faces
   restent alignées.

*Résultat attendu* : impression sur une seule page, QR toujours à sa position d'origine sur le recto,
recto et verso de même hauteur, quel que soit le navigateur utilisé pour imprimer.

## Phase 21 — Le logo de l'école ne doit plus disparaître après modification des informations

**Rôle** : `SCHOOL_ADMIN`.

**Contexte** : `/settings` a deux formulaires distincts sauvegardant chacun indépendamment vers le même
champ `logo` du tenant (« Informations de l'établissement » et « Apparence »). Le premier resoumettait
sa propre copie chargée au montage de la page — s'il était enregistré APRÈS un changement de logo via
« Apparence » (ex. après avoir renseigné IRE/DPE/DSSEE, §19), il réécrasait silencieusement le logo
fraîchement téléversé avec l'ancienne valeur. Corrigé : « Informations de l'établissement » resoumet
désormais toujours la valeur de logo la plus fraîche connue (contexte de branding partagé), jamais son
propre instantané périmé.

1. `/settings` → carte « Apparence » → téléverser un nouveau logo → vérifier qu'il apparaît (en-tête de
   l'app, badges).
2. Sans recharger la page, aller dans la carte « Informations de l'établissement » → modifier un champ
   quelconque (ex. l'adresse) → Enregistrer.
3. Vérifier que le logo est toujours présent (en-tête de l'app, `/settings` → « Apparence », badges) —
   il ne doit plus revenir à l'ancien logo ou disparaître.

*Résultat attendu* : le logo survit à l'enregistrement du formulaire « Informations de l'établissement »,
quel que soit l'ordre dans lequel les deux formulaires sont utilisés.

## Phase 22 — Relance SMS automatique des échéances de frais en retard

**Rôle** : `SCHOOL_ADMIN` / `ACCOUNTANT` (configuration NimbaSMS + frais) ; effet observé côté parent.

**Contexte** : la relance automatique (job planifié quotidien, 6h) des échéances dépassées ne
fonctionnait pas : (1) le job tournait hors contexte de tenant (comme tout job planifié, pas de JWT),
et interrogeait donc TOUTES les écoles à la fois au lieu d'une seule ; (2) même l'événement publié
n'avait aucun écouteur côté `communication` — aucun SMS n'était en réalité jamais envoyé, ni
automatiquement ni via le bouton manuel « Faire avancer le recouvrement » sur une facture. D'où
l'impression que les SMS Nimba ne partaient que lorsqu'un utilisateur était connecté (seul cas où un
contexte tenant existe déjà). Corrigé : le job boucle maintenant explicitement par école (comme le
marquage automatique des absences), et un envoi SMS de relance est réellement déclenché pour chaque
élève dont la facture liée à l'échéance n'est pas soldée.

1. Configurer NimbaSMS dans les paramètres du tenant (clé API).
2. `/billing` (ou équivalent) → créer un frais avec une échéance dont la date limite est déjà passée,
   et une facture élève impayée rattachée à cette échéance.
3. Attendre l'exécution du job planifié (6h) — ou, pour un test immédiat, invoquer manuellement le
   déclenchement si un endpoint de test existe, sinon vérifier en environnement de dev via les logs
   backend (`OverdueInstallmentProcessor`, `CommunicationEventListener.onEcheanceDepassee`).
4. Vérifier dans `/communication` (historique) qu'un SMS motif « Échéance de paiement dépassée » a bien
   été créé et envoyé au parent de l'élève concerné.

*Résultat attendu* : un SMS de relance part automatiquement pour chaque élève en retard, sans qu'aucun
utilisateur ne soit connecté au moment de l'exécution du job.

## Phase 23 — Hiérarchie administrative (IRE, DPE/DCE, DSSEE) sur le verso des badges

**Rôle** : tout rôle imprimant un badge (élève, enseignant, personnel).

**Contexte** : ces informations n'étaient renseignables que dans « Paramètres » et utilisées sur les
documents PDF (bulletins, factures, reçus…) — absentes des badges. Le endpoint `/tenant/branding`
(consommé par tous les rôles pouvant imprimer un badge, pas seulement SCHOOL_ADMIN) exposait
seulement logo/couleur/devise/slogan ; il expose désormais aussi IRE, DPE/DCE et DSSEE.

1. `/settings` → renseigner IRE, DPE/DCE et DSSEE (§19 — déjà en place).
2. Ouvrir la fiche d'un élève ou d'un enseignant/personnel → badge → vérifier qu'un bandeau
   « MEPU-A / IRE : ... · DPE/DCE : ... · DSSEE : ... » apparaît en haut du **verso**, au-dessus de
   « République de Guinée ».
3. Vider un des trois champs dans les paramètres → régénérer le badge → vérifier que seuls les champs
   renseignés apparaissent (le bandeau disparaît entièrement si les trois sont vides).
4. Imprimer / « Enregistrer en PDF » → vérifier que le bandeau reste lisible à la taille imprimée.

*Résultat attendu* : la hiérarchie administrative renseignée dans les paramètres apparaît sur le verso
de tous les badges, à l'écran comme à l'impression.

## Phase 24 — Paiement groupé au guichet : un seul reçu, une seule notification

**Rôle** : `ACCOUNTANT` / `SECRETARY` (`/guichet`) ; effet observé côté parent.

**Contexte** : au guichet, régler en une fois plusieurs factures d'un même élève (ex. inscription +
tranche 1 + tranche 2) produisait déjà UN SEUL reçu (correct), mais déclenchait UNE NOTIFICATION PAR
FACTURE couverte (3 SMS pour 3 factures réglées en un seul geste), en plus de la notification globale
du paiement groupé — alors que le code prévoyait explicitement de n'en envoyer qu'une seule pour tout
le règlement. Corrigé : le règlement de chaque facture du groupe ne déclenche plus sa propre
notification ; seule la notification globale du paiement (montant total, référence du règlement)
part au parent.

1. `/guichet` → sélectionner un élève ayant plusieurs factures impayées (ex. inscription + 2 tranches).
2. Effectuer un paiement groupé couvrant les trois.
3. Vérifier qu'**un seul** reçu est généré (déjà le cas avant ce correctif).
4. Vérifier dans `/communication` (historique) qu'**une seule** notification « Paiement reçu » a été
   créée pour ce règlement, pas une par facture.

*Résultat attendu* : un paiement groupé au guichet produit toujours un seul reçu et déclenche
désormais une seule notification, quel que soit le nombre de factures couvertes.

## Phase 25 — Confirmation : SMS et mode hors-ligne du guichet

**Contexte, pour information (pas un correctif — comportement déjà correct et volontaire)** : le
guichet (`/guichet`) a un mode hors-ligne (file locale IndexedDB, rejeu automatique à la reconnexion)
pour les canaux encaissés physiquement (espèces, Orange Money guichet, carte bancaire). Si le
comptable encaisse un paiement alors que l'appareil n'a pas de réseau, le paiement est mis en file
localement — le SMS de confirmation au parent ne part que lorsque l'appareil retrouve une connexion et
que la synchronisation avec le serveur réussit (l'envoi du SMS se fait toujours côté serveur, jamais
depuis l'appareil du comptable). Pas d'action requise ; à vérifier si besoin en coupant le réseau de
l'appareil pendant un encaissement au guichet, puis en le rétablissant.

## Phase 26 — Présence manuelle élève alignée sur enseignant/personnel

**Rôle** : `SECRETARY` / `TEACHER` (`canWrite`) sur `/attendance` ; effet observé côté parent sur `/children`.

**Contexte** : la présence manuelle (entrée + sortie, pas juste un statut) existait déjà côté backend
pour les élèves (mêmes endpoints/mécanisme que pour les enseignants/personnel, y compris hors-ligne),
mais DEUX écarts la rendaient invisible/peu utilisable en pratique par rapport aux enseignants/personnel :
1. L'onglet s'appelait « Suivi & corrections » — aucun nom évoquant « élèves », contrairement à
   « Présences enseignants »/« Présences personnel ». Renommé en **« Présences élèves »**.
2. Plus important : cet onglet n'affichait que les élèves ayant déjà un enregistrement de présence pour
   la date (créé via « Appel de classe ») — tant que l'appel n'avait pas été fait, l'onglet restait
   **vide**. Les onglets enseignants/personnel, eux, partent toujours de la liste complète (tous les
   enseignants/employés apparaissent, avec « Aucun pointage » si rien n'est encore saisi). L'onglet
   élèves fait maintenant pareil : tous les élèves de la classe choisie apparaissent immédiatement,
   avec les boutons directs « Présent », « Retard », « Sortie », « Absent » (fonctionnels même sans
   pointage préalable — même mécanisme d'enregistrement que l'appel de classe, un seul élève à la fois).

1. `/attendance` → choisir une classe et une date **sans avoir fait l'appel** → onglet « Présences élèves ».
2. Vérifier que TOUS les élèves de la classe apparaissent immédiatement (pas seulement ceux ayant déjà
   un pointage), chacun avec un badge « Aucun pointage » et les boutons « Présent », « Retard »,
   « Sortie », « Absent » — comme les onglets « Présences enseignants »/« Présences personnel ».
3. Cliquer « Présent » sur un élève sans pointage préalable → saisir une heure → confirmer → vérifier
   que le statut, l'heure d'entrée et le retard (calculé automatiquement vs l'emploi du temps)
   apparaissent.
4. Cliquer « Retard » ou « Absent » directement (sans passer par « Appel de classe ») → vérifier
   l'enregistrement immédiat.
5. Cliquer « Sortie » → saisir une heure → confirmer → vérifier l'heure de sortie enregistrée.
6. Se connecter en tant que parent de cet élève (`/children`) → section « Présences du jour —
   entrée/sortie par cours » → vérifier que l'entrée/la sortie saisies apparaissent avec le bon cours,
   la même date.

*Résultat attendu* : onglet « Présences élèves » immédiatement exploitable dès qu'une classe et une
date sont choisies, sans devoir d'abord faire l'appel de classe — même logique que pour les
enseignants/personnel ; heures d'entrée/sortie par cours visibles côté parent (déjà en place, aucune
régression).

## Phase 27 — Bouton « Notifier parent » : renvoi réel de la notification (pas juste une case cochée)

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR` / `SECRETARY` / `TEACHER` sur `/attendance` → onglet « Présences élèves ».

**Contexte** : le bouton « Notifier parent » (visible sur une ligne ABSENT/RETARD non encore notifiée)
ne faisait que cocher un indicateur interne — il ne déclenchait AUCUN envoi. La notification
automatique existe bien (SMS via `communication`, email via `notification`, déclenchés dès qu'un élève
est marqué absent/en retard), mais rien ne remettait la case « notifié » à jour après un envoi SMS
réussi (elle n'était mise à jour automatiquement qu'après un envoi EMAIL réussi — rare, la plupart des
parents n'ayant qu'un numéro de téléphone). Corrigé : le bouton déclenche maintenant un **nouvel essai
réel** (mêmes canaux que l'envoi automatique) avant de marquer la ligne comme traitée — utile
notamment si l'envoi automatique avait échoué (ex. NimbaSMS pas encore configuré à ce moment-là).
Au passage, un enseignant (`TEACHER`) voyait ce bouton mais recevait un refus silencieux en cliquant
(permission serveur non alignée avec l'affichage) — corrigé aussi.

1. `/attendance` → onglet « Présences élèves » → marquer un élève « Absent » ou « Retard ».
2. Cliquer « Notifier parent ».
3. Vérifier dans `/communication` (historique) qu'un nouvel envoi (SMS et/ou email selon les contacts
   du parent) apparaît pour cet élève, motif absence/retard.
4. Vérifier que le bouton disparaît (ligne marquée « Notifié ») après le clic.

*Rappel* : l'envoi réel dépend toujours de la configuration NimbaSMS sur le serveur (§22) et de
l'acceptation par le parent de l'accès plateforme (opt-in) — sans l'un ou l'autre, l'essai est
journalisé mais n'atteint pas le parent.

*Résultat attendu* : « Notifier parent » déclenche un envoi réel, pas seulement un indicateur visuel ;
accessible aux mêmes rôles que la saisie de présence.

## Phase 28 — Absences élèves détectées à 14h ; nom de l'école toujours sur les SMS ; annonce paie

**Rôle** : effet observé côté parent (absences) et par tout rôle envoyant des SMS.

**28.1 — Marquage auto d'absence élève avancé à 14h (au lieu de 22h)**

Contexte : le filet de sécurité qui marque absent un élève sans aucun pointage (ni scan, ni saisie
manuelle) tournait à 22h pour tout le monde (élèves et enseignants dans le même job). Séparé en deux
jobs : les élèves sont désormais vérifiés à **14h** (le parent est averti le jour même, pendant qu'il
peut encore agir), les enseignants/personnel restent vérifiés à 22h (usage RH, pas une notification à
un tiers). Pour éviter de signaler à tort un élève dont le seul cours du jour est l'après-midi, le job
de 14h ne considère que les cours **déjà commencés** à cette heure-là.

1. Un élève sans aucun cours avant 14h ce jour-là (ex. classe qui ne commence qu'à 15h) ne doit
   PAS être marqué absent par le job de 14h — vérifier après exécution (ou en environnement de dev en
   invoquant le job manuellement) qu'il reste sans statut tant que son cours de l'après-midi n'a pas
   commencé.
2. Un élève avec un cours le matin et aucun pointage doit être marqué absent à 14h, et son parent
   reçoit le SMS d'absence automatique le jour même (pas seulement au signalement de 22h comme avant).
3. Si l'élève arrive/est pointé plus tard dans la journée (cours de l'après-midi), le statut repasse
   correctement à « Présent ».

**28.2 — Le nom de l'école apparaît désormais sur TOUS les SMS, pas seulement les notifications automatiques**

Contexte : le préfixe `[Nom de l'école]` n'était appliqué que dans le code des notifications
automatiques (absence, paiement, bulletin…) — une diffusion de masse composée manuellement par le
personnel (`/communications` → Diffusion) ou tout futur envoi direct n'en bénéficiait pas forcément.
Déplacé au point d'envoi unique (`CommunicationService`), donc désormais garanti pour tout SMS, quel
que soit l'appelant.

1. `/communications` → Diffusion → composer un message SMS libre à une audience quelconque → envoyer.
2. Vérifier que le SMS reçu commence bien par `[Nom de l'école]`.

**28.3 — Bouton « Notifier salaires disponibles » (tous enseignants + personnel)**

Contexte : nouvelle fonctionnalité demandée — un bouton pour prévenir tout le personnel (enseignant ET
non-enseignant) par SMS que les salaires sont disponibles, en un clic, sans composer une diffusion
manuelle à chaque fois. Nouvelle audience de diffusion `ALL_STAFF` (enseignants + personnel actif),
réutilisable aussi depuis l'écran générique de diffusion (`/communications`).

1. `/payroll` → onglet « Fiches de paie » → bouton « Notifier salaires disponibles » → confirmer.
2. Vérifier dans `/communication` (historique) qu'un SMS est parti à tous les enseignants ET à tout le
   personnel actif (pas le personnel inactif/désactivé), préfixé du nom de l'école.
3. Vérifier depuis `/communications` → Diffusion que l'audience « Tout le personnel (enseignants +
   personnel) » est aussi disponible dans le sélecteur générique.

*Résultat attendu des trois points* : absences élèves signalées dès 14h (sans faux positifs pour les
cours de l'après-midi) ; nom de l'école systématiquement présent sur les SMS, quel que soit le canal
d'envoi ; diffusion en un clic à tout le personnel pour l'annonce de la paie.

## Phase 29 — Nom de l'enseignant affiché au scan (au lieu de « #id ») ; heures au format HH:mm

**29.1 — Le scan d'un enseignant affichait « #3 » au lieu de son nom**

Contexte : au scan d'une carte élève ou personnel, le nom de la personne s'affichait correctement,
mais au scan d'une carte ENSEIGNANT, le backend renvoyait toujours `name: null` (jamais résolu,
contrairement à l'élève et au personnel) — le frontend retombait alors sur l'identifiant brut
(`#3`). Corrigé : le nom de l'enseignant est désormais résolu et renvoyé comme pour les autres profils.

1. `/attendance/scan` → sélectionner « Enseignant » → scanner (ou saisir manuellement) la carte d'un
   enseignant.
2. Vérifier que le journal de scan affiche le **nom** de l'enseignant, pas `#<id>`.

**29.2 — Les heures affichées avaient les secondes/millisecondes (ex. `13:47:09.691`)**

Contexte : plusieurs tableaux de présence (suivi élèves, présences enseignants/personnel, récaps
mensuels, tableau de bord parent, journal de scan) affichaient l'heure brute renvoyée par le serveur
(`HH:mm:ss.SSS`) au lieu de `HH:mm`. Une fonction de formatage (`formatTime`) existait déjà dans le
projet mais n'était pas utilisée à ces endroits. Appliquée partout où une heure de pointage est
affichée.

1. `/attendance` (tous les onglets), `/children` (parent, section présences du jour) et
   `/attendance/scan` (journal de scan) → vérifier que toutes les heures affichées sont au format
   `HH:mm` (ex. `13:47`), jamais avec les secondes.

*Résultat attendu* : nom affiché (pas d'identifiant brut) partout où une personne scannée est
identifiée ; heures toujours au format `HH:mm`.

## Phase 30 — Heure de sortie / départ anticipé visibles sur le tableau « Présences élèves »

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR` / `SECRETARY` / `TEACHER` sur `/attendance` → onglet « Présences élèves ».

**Contexte** : l'onglet « Présences élèves » n'affichait que l'heure d'entrée et le retard —
contrairement aux onglets « Présences enseignants »/« Présences personnel », il n'avait tout
simplement pas de colonnes « Heure de sortie »/« Départ anticipé ». Après un scan ou une saisie de
sortie, rien ne l'indiquait sur ce tableau, même si l'information était bien enregistrée côté serveur
(déjà visible côté parent, voir §portail). Un élève pouvant avoir plusieurs séances (donc plusieurs
sorties) dans la même journée, contrairement à un enseignant/employé (un seul pointage jour), les deux
nouvelles colonnes agrègent : la dernière heure de sortie connue et le total des minutes de départ
anticipé cumulées sur la journée.

1. `/attendance/scan` → scanner l'entrée d'un élève, puis sa sortie (ou utiliser la saisie manuelle
   depuis l'onglet « Présences élèves »).
2. `/attendance` → onglet « Présences élèves » → vérifier que les colonnes « Heure de sortie » et
   « Départ anticipé » affichent bien les valeurs enregistrées pour cet élève.
3. Répéter avec un élève ayant plusieurs séances/sorties le même jour (ex. cours du matin ET de
   l'après-midi) → vérifier que la dernière sortie de la journée est affichée, et que le départ
   anticipé cumule les minutes des séances concernées.
4. Se connecter en tant que parent (`/children`) → confirmer que les mêmes informations (entrée/sortie
   par cours) étaient déjà visibles là aussi.

*Résultat attendu* : le personnel voit désormais l'heure de sortie et le départ anticipé des élèves
sur le même tableau que l'entrée/le retard, sans avoir à consulter une autre page.

## Phase 31 — Marge de tolérance générale (retard) + détail des séances par cours

**31.1 — La marge de tolérance ne s'appliquait pas à une entrée « normale »**

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR` (réglage) ; effet observé côté `SECRETARY`/`TEACHER` sur `/attendance`.

**Contexte** : la marge de tolérance (§réglages, ex. 5 min) n'était appliquée qu'à un cas précis
(deux séances consécutives dans des salles différentes, temps de trajet) — une entrée « normale »
(1ᵉʳ cours de la journée, ou salle inchangée) ne bénéficiait d'AUCUNE tolérance : pointer à 14h05 pour
un cours à 14h00 était compté en retard même avec 5 min de marge configurée. Corrigé : la marge
s'applique désormais à TOUTE entrée (élève, enseignant, personnel), quelle qu'en soit la raison — un
délai de tolérance générale, comme l'attendait la configuration.

1. `/attendance` → onglet « Réglages » → vérifier/définir la marge de tolérance (ex. 5 min).
2. Scanner ou saisir manuellement l'entrée d'un enseignant à « heure de début du cours + 5 min ou
   moins » (ex. cours à 14h00, entrée à 14h05) → vérifier qu'AUCUN retard n'est compté, quelle que soit
   la séance (première du jour, salle inchangée ou changée).
3. Entrée à « heure de début + 6 min » → vérifier que le retard compté est de 1 min (6 − 5), pas 6.
4. Répéter pour un élève (onglet « Présences élèves ») et pour le personnel non-enseignant (onglet
   « Présences personnel ») → même comportement.

**31.2 — Détail des séances : pour quel cours il est arrivé à l'heure, en retard, reparti en avance**

**Rôle** : idem, sur `/attendance` → onglet « Présences enseignants » ou « Présences élèves ».

**Contexte** : le tableau n'affichait que des totaux journaliers (1ʳᵉ heure d'entrée, dernière heure
de sortie, retard/départ anticipé cumulés) — impossible de savoir pour QUEL cours précisément la
personne était en retard ou repartie en avance dès qu'il y avait plusieurs séances le même jour.
Ajouté : une flèche à côté de chaque ligne ayant au moins un pointage déplie un détail séance par
séance (nom du cours, heure d'entrée + statut à l'heure/retard, heure de sortie + statut à
l'heure/départ anticipé).

1. Un enseignant ou un élève ayant plusieurs séances pointées le même jour (au moins une en retard ou
   avec départ anticipé) → cliquer la flèche sur sa ligne.
2. Vérifier que le détail liste chaque séance avec le nom du cours (ou l'intitulé libre si pas de
   matière formelle), l'heure d'entrée avec un badge « À l'heure »/« Retard X min », et l'heure de
   sortie avec un badge « À l'heure »/« Anticipé X min » (ou « — » si pas encore sorti).
3. Une ligne sans aucun pointage ce jour-là ne doit pas afficher de flèche (rien à déplier).
4. Onglet « Présences enseignants » spécifiquement : vérifier qu'une colonne « Classe » apparaît dans
   le détail (un enseignant peut donner la même matière dans plusieurs classes — le nom du cours seul
   ne suffit pas à savoir laquelle). Onglet « Présences élèves » : pas de colonne « Classe » dans le
   détail (déjà fixée pour tout le tableau, redondant de la répéter).

*Résultat attendu* : marge de tolérance appliquée uniformément ; détail par cours consultable en un
clic, sans quitter le tableau récapitulatif.

## Phase 32 — Scan de sortie : heure et départ anticipé enfin affichés (élève/enseignant/personnel)

**Rôle** : tout rôle scannant sur `/attendance/scan`.

**Contexte** : au scan d'une sortie, la réponse immédiate affichait soit une comparaison peu claire
« X réalisées / Y prévues (+/-Z) » (sans heure ni mention explicite « départ anticipé »), soit rien du
tout si les minutes prévues n'étaient pas calculables — jamais l'heure de sortie elle-même. Cause :
la minute exacte de départ anticipé (déjà calculée et enregistrée côté serveur, visible sur les
tableaux de suivi) n'était simplement jamais renvoyée dans la réponse du scan. Corrigé pour les trois
profils (élève, enseignant, personnel) : le scan affiche désormais toujours l'heure de sortie, avec un
badge « À l'heure » ou « Anticipé X min ».

1. `/attendance/scan` → scanner l'entrée puis la sortie d'un élève (ou enseignant, ou personnel).
2. Vérifier que le journal de scan affiche bien « Sortie HH:mm », avec un badge vert « (à l'heure) »
   si sorti à l'heure prévue ou après, ou un badge ambre « — Anticipé X min » si sorti avant.
3. Répéter pour les trois profils (élève/enseignant/personnel) → même comportement partout.

*Résultat attendu* : heure de sortie et départ anticipé toujours visibles immédiatement au scan, pour
tous les profils — pas seulement sur le tableau de suivi consulté après coup.

## Phase 33 — Basculer Entrée/Sortie après le démarrage de la caméra n'était pas pris en compte

**Rôle** : tout rôle scannant via la caméra sur `/attendance/scan`.

**Contexte** : bug réel confirmé — une fois la caméra démarrée, la boucle de lecture des QR codes
gardait en mémoire le sens (Entrée/Sortie) ET le profil (élève/enseignant/personnel) sélectionnés AU
MOMENT du démarrage de la caméra, sans jamais les rafraîchir (closure figée, bug React classique sur
une boucle `requestAnimationFrame`). Résultat : basculer sur « Sortie » après avoir démarré la caméra
n'avait aucun effet sur les scans suivants — ils continuaient à être traités comme une entrée, sur la
même séance déjà pointée, d'où le même retard déjà affiché à l'entrée qui réapparaissait. Seule la
saisie manuelle (recherche par nom + bouton « Pointer ») n'était pas concernée, elle lisait toujours
la sélection à jour.

1. `/attendance/scan` → démarrer la caméra → scanner l'entrée d'un élève (ou enseignant/personnel).
2. Sans arrêter la caméra, basculer sur « Sortie » → rescanner la même carte.
3. Vérifier que le journal affiche bien « Sortie HH:mm » (pas un nouveau « Retard » comme à l'entrée).
4. Basculer le profil (élève → enseignant) sans arrêter la caméra → scanner une carte enseignant →
   vérifier qu'elle est bien traitée comme un enseignant, pas comme un élève.

*Résultat attendu* : le sens (entrée/sortie) et le profil sélectionnés à l'écran sont toujours ceux
réellement appliqués au scan suivant, caméra déjà démarrée ou non.

## Phase 34 — Frais plateforme cohérent, marge de tolérance à 0 par défaut, réalisation de séance à choix explicite, fiche de paie combinée

**34.1 — Cohérence de l'opt-in frais plateforme (frais annexe)**

**Rôle** : `SCHOOL_ADMIN`.

**Contexte** : le frais plateforme n'apparaît sur une facture que si l'école a explicitement activé
l'accès plateforme pour l'élève (déjà opt-in, déjà `false` par défaut — pas de changement nécessaire
sur ce point). Le vrai bug : deux vérifications différentes de cet opt-in pouvaient diverger sur une
inscription encore `PENDING` (facturée mais pas notifiée, ou l'inverse). Corrigé : les deux exigent
maintenant une inscription `VALIDÉE`.

1. Créer une inscription `EN_ATTENTE` (pas encore validée) avec l'opt-in plateforme coché.
2. Vérifier qu'aucune facture ne comporte le frais plateforme tant que l'inscription n'est pas validée.
3. Valider l'inscription → régénérer/émettre une facture → vérifier que la ligne « dont frais de
   service plateforme » apparaît bien sur le PDF.

**34.2 — Marge de tolérance à 0 par défaut**

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR`.

1. `/attendance` → onglet « Réglages » → sur une école n'ayant jamais configuré la marge, vérifier
   qu'elle affiche « 0 » (aucune tolérance), pas une valeur non nulle implicite.
2. Sans configurer de marge, pointer une entrée avec quelques minutes de retard → vérifier que le
   retard est bien compté (0 min de tolérance).

**34.3 — Choix explicite théorique/réel en marquant une séance réalisée**

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR` sur `/lessons`.

1. Marquer une séance comme réalisée pour un enseignant pointé (entrée + sortie) ce jour-là.
2. Vérifier que la modale propose deux options cliquables : « Durée théorique du créneau » et « Durée
   réelle pointée » (avec le détail entrée/sortie et le retard éventuel) — pas une simple suggestion
   facultative.
3. Sélectionner chaque option → vérifier que le champ durée se met à jour en conséquence ; modifier le
   champ manuellement → l'option affichée passe à « personnalisée ».
4. Confirmer → vérifier sur la paie du mois de l'enseignant que la durée retenue est bien celle
   choisie.

**34.4 — Fiche de paie combinée (direction/administratif + enseignement)**

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR` / `ACCOUNTANT` (paie).

1. `/teachers/staff` (ou équivalent) → éditer un membre du personnel de direction/administratif →
   dans « Enseigne aussi », sélectionner sa fiche enseignant → enregistrer.
2. `/payroll` → onglet « Fiches de paie » → bouton « Personnel (direction/admin.) » → choisir le mois
   → vérifier que cette personne propose bien un bouton « Fiche combinée » (les autres n'ont que
   « Fiche simple »).
3. Générer la fiche combinée → ouvrir son détail → vérifier une ligne de salaire de base (fixe) et une
   ligne « Heures d'enseignement » calculée automatiquement, avec un net = somme des deux.
4. Personnel sans enseignant lié → seule « Fiche simple » proposée, générée avec le salaire de base
   fixe uniquement.

*Résultat attendu des quatre points* : facturation du frais plateforme cohérente entre modules ;
aucune tolérance de retard non configurée explicitement ; choix clair théorique/réel à la validation
d'une séance ; une fiche de paie unique pour le personnel cumulant direction et enseignement.

## Phase 35 — Reçu affiché aussitôt l'encaissement + refonte visuelle des badges

**35.1 — Reçu affiché immédiatement après un encaissement guichet**

**Rôle** : `SECRETARY` / `ACCOUNTANT` sur `/students` (encaissement d'un élève ou d'une fratrie).

1. Créer un élève → passer directement à l'inscription → encaisser les frais d'inscription au
   guichet (espèces, Orange Money ou virement — pas Djomy en ligne).
2. Vérifier que la modale reste ouverte et que le reçu PDF s'affiche automatiquement (visualiser,
   télécharger, imprimer) sans avoir à fermer la fenêtre ni à aller sur la fiche élève → onglet
   factures.
3. Fermer manuellement l'aperçu du reçu → vérifier que la modale d'encaissement est toujours ouverte
   (elle ne se ferme plus automatiquement) et permet de régler d'autres factures si besoin.
4. Même vérification depuis « Encaissement groupé » (fratrie, `MultiChildCollectModal`) et depuis la
   fiche élève existante (`CollectFeeModal` sur `/students/{id}`).

**35.2 — Nouveau visuel des badges (recto/verso conforme au modèle fourni)**

**Rôle** : `SCHOOL_ADMIN` / `SECRETARY` sur `/student-cards`, `/teachers`, `/teachers/staff`,
`/attendance/scan` (aperçu badge après scan).

1. Ouvrir un badge élève → vérifier le recto : bandeau coloré (logo + nom école + slogan), bandeau
   noir avec le type de badge, photo ronde en chevauchement sur une vague, nom + informations
   alignées « libellé : valeur » — sans QR sur le recto.
2. Vérifier le verso : bandeau noir « République de Guinée », armoiries en filigrane, devise
   nationale, informations école/IRE/DPE-DCE/DSSEE, puis le QR de vérification en bas avec la note de
   présentation — le QR n'apparaît plus qu'au verso.
3. Imprimer / télécharger en PDF → vérifier que le recto et le verso sortent côte à côte, à la taille
   réelle d'une carte CR80, sans contenu rogné.
4. Même vérification sur un badge enseignant et un badge personnel (recto/verso générique réutilisé).

*Résultat attendu* : un reçu consultable en un clic dès l'encaissement, sans détour par la fiche
élève ; des badges visuellement alignés sur le modèle fourni par l'établissement, QR au verso,
contenu officiel (armoiries, hiérarchie) conservé.

## Phase 36 — Badge : ajustements visuels (vague, bandeau noir, positions) + bug d'impression

**36.1 — Design recto/verso affiné**

**Rôle** : `SCHOOL_ADMIN` / `SECRETARY` sur `/student-cards` (badge élève, enseignant ou personnel).

1. Recto : vérifier que le titre du type de badge (ex. « Carte élève ») est centré dans le bandeau
   coloré, sans fond noir.
2. Verso : vérifier que « République de Guinée » et la devise nationale « Travail — Justice —
   Solidarité » sont l'un directement sous l'autre (pas séparés par la vague ni par un grand espace),
   toujours sans bandeau noir.
3. Verso : vérifier que le nom de l'école, sa propre devise (entre guillemets) et son slogan sont
   affichés de façon visible et distincte (pas noyés dans la liste IRE/DPE-DCE/DSSEE).
4. Verso : vérifier que le QR code est positionné en bas à gauche, en chevauchement sur la vague du
   bas (comme sur le modèle fourni), avec la note de présentation à droite.

**36.2 — Bug corrigé : l'impression/téléchargement PDF ne doit plus changer le visuel**

**Contexte** : le mécanisme d'impression (`printNode`) recopiait le texte des feuilles de style en le
reconstruisant via `cssRules`/`cssText` — cette lecture échouait silencieusement pour une partie du
CSS Tailwind compilé, produisant un PDF quasi sans mise en forme (pas de couleurs, pas de bordures de
carte, texte aligné à gauche). Corrigé en reliant directement les vraies feuilles de style (`<link>`)
dans l'iframe d'impression au lieu de les resérialiser — vérifié en reproduisant fidèlement le pipeline
d'impression hors application (comparaison avant/après : avant = PDF totalement sans style, après =
identique à l'aperçu écran).

**Rôle** : `SCHOOL_ADMIN` / `SECRETARY` sur `/student-cards`.

1. Ouvrir un badge (élève, enseignant ou personnel), cliquer « Imprimer / Télécharger en PDF ».
2. Dans l'aperçu d'impression, vérifier que le rendu est identique à l'écran : couleurs de l'en-tête,
   vagues, photo ronde, bandes tricolores, QR, tout doit apparaître — pas de texte brut sans mise en
   forme.
3. Vérifier que « Imprimer en PDF » (Enregistrer en PDF) donne le même résultat que l'impression papier.

*Résultat attendu* : le PDF/l'impression reproduit fidèlement ce qui est affiché à l'écran, sans perte
de style.

**36.3 — QR mal positionné (chevauchait le texte) + recto/verso de tailles différentes**

**Rôle** : `SCHOOL_ADMIN` / `SECRETARY` sur `/student-cards`.

1. Ouvrir le verso d'un badge dont l'IRE/DPE-DCE/DSSEE sont renseignés → vérifier que le QR (en bas à
   gauche) ne recouvre plus la ligne « DSSEE » ni aucune autre ligne d'information — il ne chevauche
   que la vague décorative, pas de texte.
2. Comparer visuellement le recto et le verso d'un même badge → vérifier qu'ils font la même hauteur
   (la bande tricolore du bas est au même niveau sur les deux faces), même quand l'une des deux faces
   a moins de contenu que l'autre (ex. IRE/DPE non renseignés).

*Résultat attendu* : QR toujours lisible sans chevaucher de texte ; recto et verso visuellement de
même taille, à l'écran comme à l'impression.

## Phase 37 — Vision Fondateur : cohérence des données + refonte visuelle

**37.1 — Recettes de l'école cohérentes avec Rapports & analytique**

**Rôle** : `SCHOOL_ADMIN` (fondateur) sur `/dashboard` (Vision Fondateur) et `/reports`.

1. Relever le montant de la carte « Recettes de l'école » sur Vision Fondateur.
2. Aller sur `/reports` → relever le montant « Encaissé ».
3. Vérifier que les deux montants sont désormais identiques (les deux viennent du même endpoint
   `GET /dashboard/finance`).

**37.2 — Taux d'assiduité des enseignants visible**

1. Sur Vision Fondateur, section « Masse salariale du mois » → vérifier la présence d'un diagramme
   circulaire « Assiduité des enseignants » (présences vs absences, taux affiché dans le titre).
2. Sur chaque carte enseignant (section « Salaire par enseignant »), vérifier la présence d'un badge
   d'assiduité (vert/ambre/rouge selon le taux) quand la donnée existe pour ce mois.

**37.3 — « Salaire par enseignant » et « Estimation de la masse salariale » visuellement distincts**

1. Vérifier que « Salaire par enseignant » est maintenant une grille de cartes verticales (photo ou
   initiales, nom, salaire du mois, estimation annuelle, badge assiduité) — plus un tableau.
2. Vérifier qu'une nouvelle grille « Personnel administratif » (cartes avec initiales, salaire fixe)
   apparaît juste en dessous.
3. Vérifier que « Estimation de la masse salariale sur l'année scolaire » affiche désormais un
   graphique en barres (Enseignants/Personnel par mois) au-dessus du tableau détaillé — visuellement
   non confondable avec les cartes enseignant.

**37.4 — Diagrammes circulaires ajoutés**

1. Section Recouvrement : vérifier la présence d'un donut « Encaissé vs impayés ».
2. Section Masse salariale : vérifier la présence d'un donut « Répartition de la masse salariale »
   (Enseignants vs Personnel) et du donut assiduité (37.2).

*Résultat attendu* : chiffres cohérents entre Vision Fondateur et Rapports ; taux d'assiduité des
enseignants visible ; page globalement plus lisible (cartes + graphiques plutôt que des tableaux
denses empilés).

---

## Phase 38 — Correction-hier.md : 3 bugs critiques corrigés

**38.1 — Session « zombie » après un 401 sur une requête déjà rejouée**

**Contexte** : après plusieurs heures d'inactivité, les actions semblaient échouer silencieusement
(« comme non connecté »), les listes restaient périmées tant qu'on ne rafraîchissait pas (F5), et le
Scan perdait parfois ses données. Cause : `src/lib/api/http.ts`, l'intercepteur ne nettoyait la
session (tokens + redirection `/login`) que si le **premier** rafraîchissement échouait — si la
requête rejouée après un rafraîchissement réussi essuyait elle-même un nouveau 401, plus rien ne
nettoyait la session : token mort en `localStorage`, toute requête suivante échouait sans jamais
rediriger vers la connexion.

**Rôle** : n'importe quel rôle connecté.

1. Se connecter, laisser le token expirer (ou simuler un 401 persistant côté backend).
2. Faire une action (sauvegarde, scan…) → vérifier une redirection propre vers `/login` plutôt qu'un
   échec silencieux avec données périmées.

**38.2 — Factures sans année scolaire (`academic_year_id` NULL), invisibles sur Vision Fondateur**

**Contexte** : « Impayés » affichait toujours 0 GNF et le taux de recouvrement toujours 100 % sur
Vision Fondateur, alors que de vraies factures impayées existaient. Cause : les mensualités générées
depuis la fiche élève (`CollectFeeModal` → `generateMonthlyInvoices`) et les factures de transport
auto-générées (`TransportInvoiceListener`) ne renseignaient jamais l'année scolaire, laissant
`academic_year_id = NULL` — invisibles pour Vision Fondateur, qui filtre strictement par année.
Corrigé à la source : `InvoiceService.resolveAcademicYearId()` (backend) résout automatiquement
l'année scolaire depuis l'inscription active de l'élève dès qu'elle n'est pas fournie explicitement
par l'appelant — couvre tous les appelants actuels et futurs de `create()`/`generateMonthly()`.

**Rôle** : `SECRETARY` / `ACCOUNTANT` puis `SCHOOL_ADMIN` (fondateur).

1. Générer des mensualités pour un élève depuis sa fiche (`CollectFeeModal`) sans les régler.
2. Vérifier sur `/dashboard` (Vision Fondateur) → section Recouvrement, que « Impayés » et le taux de
   recouvrement reflètent bien cette facture impayée (plus de 0 GNF / 100 % artificiels).
3. Même vérification après un abonnement transport fraîchement créé (facture auto-générée).

**38.3 — « Net à payer » affichait en réalité le montant brut, avant fiche de paie**

**Contexte** : sur `/payroll` → onglet Rémunération et paie → « Paie du mois » et « Estimation
prévisionnelle », le libellé « Net à payer » affichait en fait `grossPay` (le montant brut, avant
déduction CNSS/frais scolarité personnel) — le vrai net n'existe qu'une fois la fiche de paie
générée. Corrigé : libellé renommé en « Montant brut estimé » + note explicative ; le vrai « Net à
payer » reste correctement calculé et affiché dans les fiches de paie individuelles (`recompute()`,
non affecté par ce bug d'affichage).

**Rôle** : `SCHOOL_ADMIN` / `ACCOUNTANT` sur `/payroll`.

1. Sélectionner un enseignant → onglet Rémunération et paie → vérifier que le libellé indique
   « Montant brut estimé » (pas « Net à payer »), avec la note explicative en dessous.
2. Générer la fiche de paie → ouvrir son détail → vérifier que le vrai net (après déductions) y est
   bien affiché, distinct du montant brut vu à l'étape précédente.

*Résultat attendu* : plus de session zombie après expiration de token ; les impayés/le taux de
recouvrement sur Vision Fondateur reflètent la réalité ; les libellés brut/net ne prêtent plus à
confusion.

*Reste de `Correction-hier.md`* : 14 autres points recensés (voir l'analyse donnée à l'utilisateur en
conversation) — confusion/clarté sur Vision Fondateur, filtres par défaut sur classe (partiellement
traité en Phase 34), statut « Abandon » élève, recherche sur les listes de présence, responsive
mobile complet et PWA — traités en suivi séparé, hors de cette passe.

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

---

## Phase 39 — Statut « Abandon » élève + correction du remboursement non répercuté

**39.1 — Marquer un élève comme abandon (en cours d'année)**

**Contexte** : nouveau statut `ABANDONED`, distinct de `CANCELLED` — un élève qui a suivi les cours un
temps puis arrêté reste visible pour sa classe en facturation/recouvrement (ses factures déjà émises
restent dues), mais disparaît des listes actives du quotidien (appel, saisie de notes) et de
l'effectif de classe.

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR`.

1. Fiche élève (`/students/{id}`) ou `/enrollments` → sur une inscription **validée**, cliquer
   « Marquer comme abandon » → confirmer.
2. Vérifier le badge « Abandon » (indigo), et que le bouton n'apparaît plus (transition définitive,
   seulement disponible depuis VALIDÉE).
3. `/attendance` (appel du jour) et saisie de notes (`/grading`) pour cette classe → vérifier que
   l'élève n'apparaît plus.
4. Fiche élève → bouton « Facture » → vérifier qu'il reste accessible (rattraper un impayé).
5. Fiche classe / effectif → vérifier qu'il a baissé d'une unité.
6. Vision Fondateur → Recouvrement par classe → vérifier que les factures impayées de cet élève
   comptent toujours dans le taux de recouvrement de sa classe.
7. Essayer de réinscrire l'élève dans une autre classe la même année scolaire → vérifier que c'est
   toujours bloqué (comme pour une inscription validée classique).

**39.2 — Remboursement répercuté sur la facture**

**Contexte** : `PaymentService.markRefunded()` ne faisait que changer le statut du paiement —
`PaymentRefundedEvent` n'était écouté par aucun listener côté facturation. Corrigé : un nouveau
listener répercute désormais le remboursement sur `paidAmount`/solde/statut de la facture.

**Rôle** : `ACCOUNTANT` / `SCHOOL_ADMIN` sur `/payments`.

1. Encaisser une facture (partiellement ou en totalité).
2. Cliquer « Rembourser » sur ce paiement.
3. Ouvrir la fiche élève → onglet factures → vérifier que le solde restant a bien augmenté (la
   facture repasse à IMPAYÉE ou PARTIELLE selon le montant remboursé), visible immédiatement sans
   rechargement manuel.

*Résultat attendu* : un abandon reste financièrement traçable pour sa classe sans polluer les listes
actives du quotidien ; un remboursement se voit immédiatement sur la facture concernée.

## Phase 40 — Onglet Scolarité (factures scopées) + marge de tolérance élèves séparée

**40.1 — « Factures de l'élève » scopées à cet élève**

**Contexte** : le lien « Factures de l'élève » (onglet Scolarité de la fiche élève) pointait vers
`/invoices?student={id}`, mais la page `/invoices` ignorait complètement ce paramètre d'URL et
affichait toujours la liste de tous les élèves.

**Rôle** : n'importe quel rôle avec accès facturation.

1. Fiche élève → onglet Scolarité → « Factures de l'élève ».
2. Vérifier que la liste de factures est déjà filtrée sur cet élève (pas la liste complète), le
   sélecteur d'élève du filtre affichant déjà son nom.

**40.2 — Marge de tolérance élèves distincte de celle des enseignants/personnel**

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR` sur `/attendance` → onglet Réglages.

1. Vérifier deux champs séparés : « Marge de tolérance enseignants / personnel » et « Marge de
   tolérance élèves », chacun avec son propre bouton Enregistrer.
2. Configurer une marge différente pour chacun (ex. 10 min enseignants, 5 min élèves) → pointer une
   entrée élève en retard de 7 min → vérifier qu'elle compte bien comme en retard (au-delà des 5 min
   élève), alors qu'un enseignant dans la même situation (marge 10 min) ne le serait pas.

*Résultat attendu* : facturation d'un élève toujours scopée depuis sa fiche ; marge de tolérance
réglable indépendamment pour les élèves et pour les enseignants/personnel.

## Phase 41 — Recherche sur les listes de présence + actions immédiates au scan d'un retard

**41.1 — Barre de recherche sur les listes de présence**

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR` / `SECRETARY` sur `/attendance`.

1. Onglets « Présences élèves », « Présences enseignants », « Présences personnel » → vérifier la
   présence d'un champ de recherche par nom en haut de chaque liste, qui filtre instantanément la
   liste (élève/enseignant/personnel) sans recharger la page.

**41.2 — Notifier/justifier directement depuis le fil de scan**

**Contexte** : un retard élève déclenche déjà un SMS automatique au parent (aucune action requise),
mais rien ne permettait au surveillant d'agir depuis l'écran de scan lui-même (renvoyer la
notification en cas d'échec, ou justifier le retard) — il fallait naviguer vers `/attendance`.

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR` / `SECRETARY` sur `/attendance/scan`.

1. Scanner l'entrée d'un élève en retard (après l'heure de début + marge de tolérance).
2. Vérifier que la ligne du fil de scan affiche, en plus du badge « Retard », deux boutons
   « Notifier parent » et « Justifier »/« Non justifié ».
3. Cliquer « Notifier parent » → vérifier la confirmation « Parent notifié » (renvoi réel de la
   notification, pas un simple cochage).
4. Cliquer « Justifier » ou « Non justifié » → vérifier que le fil affiche la décision, et que la
   fiche de présence de l'élève (`/attendance`) reflète bien ce choix.

*Résultat attendu* : listes de présence filtrables par nom ; retard élève actionnable (notifier,
justifier) sans quitter l'écran de scan.

## Phase 42 — Détail du calcul sur les bulletins + enseignants avec leurs classes en paie

**42.1 — Détail du calcul visible sur la fiche de paie**

**Contexte** : le calcul détaillé (heures réalisées, retard cumulé, départ anticipé, heures
effectives) était déjà présent sur le PDF du bulletin (bloc « Heures dues/réalisées ») mais invisible
dans le panneau web « Détails », qui ne montrait que base/brut/net.

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR` / `ACCOUNTANT` sur `/payroll`.

1. Onglet « Fiches de paie » → générer une fiche depuis les heures réalisées → cliquer « Détails ».
2. Vérifier un encart « Détail du calcul » avec la phrase récapitulative (heures réalisées − retard −
   départ anticipé = heures effectives).
3. Cliquer « PDF » → vérifier le même détail, plus complet (heures dues/réalisées/non réalisées),
   dans le bulletin téléchargé.

**42.2 — Sélecteur enseignant avec ses classes (onglet Rémunération & heures)**

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR` / `ACCOUNTANT` sur `/payroll` → onglet « Rémunération & heures ».

1. Ouvrir le sélecteur « Enseignant / personnel » → vérifier que chaque enseignant affiche ses
   classes entre parenthèses (ex. « Jean Camara — 6ème A, 5ème B »), pas seulement son nom.

*Résultat attendu* : détail du calcul visible sans télécharger de PDF ; sélection d'un enseignant
sans ambiguïté sur ses classes.

## Phase 43 — Vision Fondateur : allègement + données concrètement demandées

**Contexte** : le fondateur ne comprenait pas certaines données (déjà réglé en Phase 39-40 :
impayés/recouvrement cohérents) et demandait explicitement de retirer le graphique « Recettes vs
dépenses — 6 derniers mois » (dont les chiffres divergeaient de la source désormais utilisée pour la
carte « Recettes de l'école », Phase 39.1) au profit de données concrètes : nombre d'élèves, factures
encaissées avec heure précise, enseignants payés/non payés, SMS de relance envoyés, bénéfice réel du
mois après déduction de la masse salariale.

**Rôle** : `SCHOOL_ADMIN` (fondateur) sur `/dashboard`.

1. Vérifier que le graphique « Recettes vs dépenses — 6 derniers mois » a disparu.
2. Vérifier la nouvelle carte « Élèves dans l'école » (comptage total) en tête de page.
3. Vérifier la nouvelle carte « Bénéfice du mois » (recettes − dépenses − masse salariale, distincte
   de « Balance globale » qui ne déduit pas la masse salariale — le libellé le précise).
4. Vérifier le nouveau tableau « Derniers encaissements » (date + heure précise, référence, montant).
5. Section Recouvrement → vérifier la carte « SMS de relance (ce mois) » (comptage réel des SMS
   envoyés pour échéances impayées).
6. Section Masse salariale → vérifier la carte « Enseignants payés ce mois » (X / Y, avec le nombre
   de non-payés en évidence si besoin de relance).

*Résultat attendu* : page recentrée sur les données que le fondateur a explicitement demandées, sans
le graphique jugé source de confusion.

## Phase 44 — Recouvrement par classe/débiteurs corrigés (rattrapage) + masse salariale réelle

**Contexte** : le fondateur a signalé que malgré la Phase 39-40, « Recouvrement par classe » affichait
encore certaines classes à 100 % et « Plus gros débiteurs » restait vide — root cause identifiée par
investigation : le correctif `resolveAcademicYearId` (session précédente) ne s'appliquait qu'aux
NOUVELLES factures ; les factures déjà en base avec `academic_year_id` NULL (mensualités générées
depuis la fiche élève, factures de transport) restaient invisibles du filtre par année scolaire
strict utilisé par le recouvrement, faussant les taux (seules les quelques factures déjà correctement
taguées et soldées restaient comptées). Le fondateur a aussi rappelé une demande jamais traitée : voir
la somme des retards/départs anticipés de tous les enseignants, le coût que ça représente, et la masse
salariale RÉELLE (sur heures réalisées, cahier de textes) en plus de la masse théorique déjà affichée.

**Rôle** : `SCHOOL_ADMIN` (fondateur) sur `/dashboard`.

1. Migration `V166__backfill_invoice_academic_year.sql` appliquée au démarrage (rattrape les factures
   existantes à `academic_year_id` NULL depuis l'inscription non-annulée la plus récente de l'élève).
2. Section Recouvrement → « Recouvrement par classe » : vérifier que les taux reflètent désormais
   toutes les factures de la classe (plus de classe bloquée artificiellement à 100 %).
3. « Plus gros débiteurs » : vérifier que la liste n'est plus vide dès lors qu'il existe des factures
   impayées, même anciennes.
4. Section Masse salariale → nouveau bloc « Masse salariale réelle (sur heures réalisées) —
   enseignants » :
   - carte « Masse salariale réelle » (basée sur `computeMonthlyPay`, heures cahier de textes,
     retards/départs déjà déduits) ;
   - carte « Écart théorique / réel » (masse théorique − masse réelle ; positif = économisé sur des
     heures non réalisées, négatif = dépassement) ;
   - carte « Retards cumulés » et « Départs anticipés cumulés » (somme sur tous les enseignants, au
     format `Xh MM`).

*Résultat attendu* : les deux écrans de recouvrement montrent des chiffres cohérents avec la
facturation réelle de l'école, et le fondateur peut comparer d'un coup d'œil la masse salariale
théorique (projetée depuis l'emploi du temps) à la masse réellement due sur la base des heures
effectivement réalisées, avec le cumul des retards/départs anticipés de tous les enseignants.

## Phase 45 — Cahier de textes : plus de classe présélectionnée par défaut + retour sur le scan

**Contexte** : audit systématique du reste de `Correction-hier.md` — plusieurs points étaient déjà
couverts par des sessions précédentes (frais plateforme sur facture Phase 34.1, checkbox d'accès
plateforme à l'inscription, salaire fixe pré-rempli en paie, détail heures réalisées/retard/départ
anticipé sur la paie du mois, filtre de classe non pré-appliqué sur les présences enseignants/
personnel — vérifiés à nouveau, aucun changement nécessaire). Deux points restaient réellement non
traités :

**45.1 — Cahier de textes affiche toutes les classes par défaut**

**Rôle** : tout rôle ayant accès à `/lessons`.

1. Arriver sur `/lessons` sans avoir touché le filtre « Classe » → vérifier qu'il affiche « Toutes les
   classes » (pas la première classe de la liste) et que le tableau liste les séances de TOUTES les
   classes, avec une colonne « Classe » ajoutée le temps qu'aucun filtre classe n'est choisi.
2. Sélectionner une classe précise → la colonne « Classe » disparaît (redondante), le tableau se
   filtre normalement.
3. Cliquer « Nouvelle séance » sans avoir sélectionné de classe → vérifier que le formulaire s'ouvre
   normalement (chaque ligne du formulaire a son propre sélecteur de classe).

*Résultat attendu* : plus aucune classe sans séance planifiée ne donne l'impression trompeuse que
« rien n'existe dans le cahier de textes ».

**45.2 — Bouton retour sur la page de scan**

**Rôle** : tout rôle ayant accès à `/attendance/scan`.

1. Depuis `/attendance`, ouvrir le pointage par scan de carte.
2. Vérifier la présence d'un lien « ← Retour aux présences » en haut de page, ramenant à `/attendance`.

*Résultat attendu* : plus besoin d'utiliser le bouton retour du navigateur pour revenir à la liste des
présences après un scan.

## Phase 46 — Transfert de classe d'un élève inscrit (frais scolaires différents)

**Contexte** : dernier point de `Correction-hier.md` — un élève déjà inscrit (et ayant déjà payé une
ou plusieurs mensualités) doit parfois changer de classe en cours d'année (erreur d'orientation
corrigée après-coup), or les frais de scolarité diffèrent souvent d'une classe à l'autre. Décision
validée avec le fondateur : recalcul automatique — les mensualités impayées de l'ancienne classe sont
annulées (conservées à l'état « Annulée » pour l'historique, jamais supprimées) et tout ce qui avait
déjà été payé dessus est reporté en crédit sur les nouvelles mensualités générées pour la nouvelle
classe, sans notification de paiement trompeuse pour le parent. Seule la scolarité (mensualités) est
concernée — les frais d'inscription déjà payés ne sont jamais re-facturés lors d'un transfert.

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR`.

1. Fiche élève → onglet Inscriptions → sur une inscription **validée**, cliquer « Transférer » →
   choisir une autre classe de la même année scolaire → confirmer.
2. Vérifier que la classe de l'inscription a changé, et que l'effectif baisse dans l'ancienne classe
   et augmente dans la nouvelle (`/classes`).
3. Onglet Scolarité de l'élève → vérifier que les mensualités impayées de l'ancienne classe sont
   passées au statut « Annulée » (plus comptées dans le recouvrement), et que de nouvelles mensualités
   sont apparues pour la nouvelle classe.
4. Si l'élève avait déjà payé une ou deux mensualités avant le transfert → vérifier que les nouvelles
   mensualités de la nouvelle classe apparaissent déjà partiellement ou totalement soldées (crédit
   reporté), sans qu'aucun SMS « paiement reçu » ne soit envoyé au parent pour ce report.
5. Vision Fondateur → Recouvrement par classe → vérifier que les montants annulés ne comptent plus ni
   dans le facturé ni dans l'impayé d'aucune des deux classes.
6. Cas limite : transférer vers une classe sans frais MENSUALITE configuré → vérifier dans les logs
   serveur qu'un avertissement signale le crédit non reporté (à régulariser manuellement par le
   comptable) plutôt qu'une erreur silencieuse.

*Résultat attendu* : le transfert de classe ne fait perdre aucune trace des paiements déjà effectués,
et la facturation de l'élève reflète immédiatement le nouveau barème de sa nouvelle classe.

## Phase 47 — Vision Fondateur : cartes personnes cherchables + masse salariale réelle condensée

**Contexte** : avec une centaine d'enseignants/personnel, les grilles de cartes « Salaire par
enseignant » et « Personnel administratif » rendaient la page interminable à faire défiler. Par
ailleurs les 4 cartes de masse salariale réelle (masse réelle, écart théorique/réel, retards, départs
anticipés) étaient trop éclatées — le fondateur voulait les heures réalisées directement dans la carte
« Masse salariale réelle », et retards + départs anticipés réunis dans une seule carte avec le coût
correspondant.

**Rôle** : `SCHOOL_ADMIN` (fondateur) sur `/dashboard`.

1. Section « Salaire par enseignant » / « Personnel administratif » : au-delà de 12 personnes, vérifier
   l'apparition d'une barre de recherche par nom et d'un bouton « Afficher les N autres » (plus de mur
   de cartes interminable par défaut).
2. Rechercher un nom → vérifier que seules les cartes correspondantes s'affichent.
3. Carte « Masse salariale réelle » → vérifier la mention des heures réalisées dans le sous-texte
   (ex. « Sur 42 h réalisées »).
4. Vérifier qu'il n'y a plus que 2 cartes (au lieu de 4) dans le bloc masse salariale réelle : « Masse
   salariale réelle » et « Retards & départs anticipés cumulés » — cette dernière affichant le détail
   retards/départs et le coût estimé (déjà déduit du salaire) en GNF.

*Résultat attendu* : page utilisable quel que soit l'effectif de l'école, et lecture plus directe du
coût réel des retards/départs anticipés des enseignants.

## Phase 48 — Notification parent : email cassé (SpEL) + SMS Nimba (canal manquant)

**Contexte** : en testant « Notifier le parent » sur une absence, deux échecs distincts sont apparus
dans les logs.

**48.1 — Email d'absence cassé (`SpelParseException`)**

`templates/email/absence-notification.html` utilisait `th:text="${studentName + ' est absent(e)
aujourd\'hui'}"` — en SpEL (moteur d'expression de Thymeleaf), un guillemet simple à l'intérieur d'un
littéral se double (`''`), il ne s'échappe jamais avec un antislash comme en Java. Le `\'` cassait le
parsing de l'expression, et l'email échouait silencieusement à chaque absence (exception attrapée et
journalisée dans `EmailService`, sans bloquer le reste). Corrigé en `aujourd''hui`.

**48.2 — SMS Nimba : `sender_name`/`to` « obligatoire » malgré leur présence**

Root cause : le corps JSON envoyé ne précisait pas le champ `channel` (attendu par
developers.nimbasms.com, défaut `"sms"` mais l'école a préféré le rendre explicite). `NimbaSmsGateway`
envoie maintenant `{sender_name, to:[...], message, channel:"sms"}`. Un intercepteur de diagnostic
temporaire (`NIMBA-WIRE`, sur le modèle de `DJOMY-WIRE`) journalise la requête/réponse brute échangée
avec Nimba — à retirer une fois le SMS confirmé livré en conditions réelles.

**48.3 — Backend qui ne répond plus du tout à l'envoi (aucun log, aucune trace)**

Root cause : aucun timeout connexion/lecture n'était configuré sur AUCUN des 4 clients HTTP sortants
(Nimba, Orange SMS, Resend email, Djomy paiements) — un appel réseau bloqué (DNS, tunnel ngrok mal
configuré, pare-feu) gelait indéfiniment le thread de la requête HTTP entrante, donnant l'impression
que « la méthode d'envoi ne se déclenche même pas » alors qu'elle était juste bloquée en attente sans
jamais expirer. Corrigé par un bean centralisé `RestClientTimeoutConfig`
(`gn.lacaeducation.common`) : timeout connexion 10s / lecture 20s appliqué automatiquement à tout
`RestClient` construit depuis le bean Spring `RestClient.Builder` (donc les 4 gateways), sans toucher
aux tests qui construisent leur propre `RestClient.builder()` hors contexte Spring.

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR` sur `/attendance` (scan ou liste, notifier un retard/absence).

1. Notifier le parent d'un élève absent → vérifier dans les logs `NIMBA-WIRE` que la requête part bien
   avec `sender_name`, `to`, `message`, `channel` renseignés, et que la réponse contient un
   `messageid` (succès), pas une erreur 400.
2. Vérifier que le SMS est effectivement reçu sur le téléphone du parent.
3. Si l'email est configuré pour ce parent, vérifier qu'il le reçoit aussi, avec le texte « [Élève] est
   absent(e) aujourd'hui » correctement affiché (apostrophe visible, pas d'erreur serveur dans les
   logs).
4. Simuler un réseau indisponible (ex. couper le tunnel ngrok pendant l'appel) → vérifier que la
   requête HTTP côté frontend échoue avec une erreur en ~20s maximum, au lieu de rester bloquée
   indéfiniment.

*Résultat attendu* : les deux canaux (SMS + email) fonctionnent pour une notification d'absence, et le
backend ne reste plus jamais bloqué silencieusement en cas de souci réseau côté fournisseur.

## Phase 49 — Nouveau message : liste cherchable par type + bug d'envoi jamais transmis corrigé

**Contexte** : sur `/communications` → « Nouveau message », choisir le type « Parent » n'avait aucun
effet — la liste de destinataires restait la liste générique de TOUS les comptes utilisateur (jusqu'à
300, mélangeant admin/comptable/enseignant/parent), sans filtre ni recherche, rendant impossible de
cibler un parent ou un enseignant précis.

En creusant, bug plus grave découvert en chemin : `CommunicationService.send()` ne transmet réellement
un SMS/e-mail au fournisseur QUE si une destination (téléphone/e-mail) est fournie — un envoi manuel
qui ne référence qu'un `recipientUserId` (comme le faisait ce formulaire, et aussi le bouton
« Relancer » sur la fiche élève / dossier documents) ne fait que créer un enregistrement "en attente"
indéfiniment, sans jamais rien envoyer, et aucune tâche ne résout ce `userId` en contact plus tard.
Corrigé partout où c'était utilisé : la destination (téléphone pour SMS/WhatsApp, e-mail pour EMAIL)
est désormais toujours résolue et envoyée directement.

**Rôle** : `SCHOOL_ADMIN` / `DIRECTOR` / `SECRETARY` / `TEACHER`.

1. `/communications` → « Nouveau message » → Type = « Parent » → vérifier que la liste passe aux
   parents de l'école (pas les comptes utilisateur génériques), avec une barre de recherche par nom ou
   numéro au-dessus.
2. Type = « Enseignant » → vérifier que la liste passe aux enseignants.
3. Sélectionner un canal SMS puis un parent sans numéro connu → vérifier qu'il apparaît grisé,
   non-cochable, avec la mention « pas de numéro ».
4. Envoyer un SMS à un parent précis → vérifier qu'il le reçoit réellement (avant ce correctif, rien
   ne partait jamais pour un envoi manuel ciblé).
5. Fiche élève → bouton « Relancer » (facture impayée ou dossier documents incomplet) → envoyer une
   relance SMS à un parent qui n'a jamais eu de compte plateforme → vérifier qu'il la reçoit quand même
   (avant : bloqué avec « Aucun parent avec un compte »).

*Résultat attendu* : le ciblage d'un destinataire précis fonctionne enfin, avec recherche, et l'envoi
part réellement au lieu de rester silencieusement bloqué.
