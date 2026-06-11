# Prompts de planification — projet Ecole SaaS

Prompts prêts à copier pour Claude Code, **un par module**, dans l'ordre des
dépendances. Chaque prompt suppose que `CLAUDE.md` est à la racine du projet et
déjà chargé dans le contexte.

## Mode d'emploi

1. Active le plan mode persistant : `Shift+Tab` deux fois (le bas affiche
   `plan mode on`).
2. Colle le prompt de l'étape voulue.
3. Lis le plan, corrige-le si besoin (`Ctrl+G` pour l'éditer), puis approuve.
4. Claude exécute. Une fois le module terminé et les tests Modulith verts, passe
   au module suivant.

> Astuce : le mot **« Ultrathink »** demande à Claude une réflexion plus
> approfondie. Garde-le pour les modules complexes (payments, webhooks, grading).

> Règle d'or après chaque module : demande à Claude de lancer le test
> `ApplicationModules.of(Application.class).verify()` pour garantir que les
> frontières entre modules ne sont pas violées.

---

## Étape 0 — Squelette du projet

```
Lis CLAUDE.md. Le projet Spring Boot 4.0.6 (Java 21) avec Spring Modulith est
DÉJÀ initialisé et toutes les dépendances sont déjà présentes dans le fichier
de build. Ne recrée pas le projet et ne touche pas aux dépendances.

D'abord, explore l'existant et fais-moi un état des lieux :
- lis le fichier de build (pom.xml ou build.gradle) et liste les dépendances
  déjà présentes
- compare-les à la section "Stack & dépendances" de CLAUDE.md et signale
  uniquement ce qui manquerait éventuellement (sans rien ajouter sans mon accord)
- repère le package racine réel et la classe d'application existante
- liste la structure de packages déjà en place

Ensuite, mets en place UNIQUEMENT le squelette d'architecture, en respectant
strictement la section "Architecture cible : Spring Modulith + Hexagonale" :
- vérifie / complète la configuration PostgreSQL + Flyway (dossier
  db/migration), sans écraser une config existante
- vérifie / complète la configuration de base Spring Security
- crée le test ModularityTests qui appelle
  ApplicationModules.of(Application.class).verify() et génère la documentation
- crée un package vide par module (platform, iam, students, staff, academics,
  attendance, grading, billing, payments, webhooks, communication) avec son
  package-info.java annoté @ApplicationModule, en t'alignant sur le package
  racine réel du projet (pas forcément com.ecole.saas — utilise celui qui
  existe déjà)

Contraintes :
- réutilise et adapte l'existant, ne duplique pas, n'écrase pas un fichier sans
  me prévenir
- ne crée aucune table métier à ce stade
- si un choix de config existant diffère de CLAUDE.md, signale-le et demande-moi
  avant de modifier

Ultrathink. Propose le plan d'abord, en commençant par l'état des lieux.
```

---

## Étape 1 — Module `platform`

```
Lis CLAUDE.md. Implémente le module "platform" en architecture hexagonale
(domain / application / adapters in-out), pour les tables Tenant, Plan,
Abonnement, FactureAbonnement, PaiementAbonnement, RecuAbonnement,
ChangementPlan.

Respecte :
- domaine pur sans JPA, entités JPA dans adapters/out/persistence avec mapping
- ports in (use cases) et out (repositories) dans application/port
- API publique du module exposée dans le package racine
- migrations Flyway pour ces tables
- le CYCLE DE VIE DE L'ABONNEMENT décrit dans CLAUDE.md (section dédiée) :
  choix du plan -> abonnement EN_ATTENTE -> facture émise -> paiement (Orange
  Money / MTN / Wave / carte) -> activation ACTIF + reçu, ou IMPAYEE -> relance
  -> renouvellement à échéance ou suspension après délai de grâce
- le contrôle de quota : vérifier limite_eleves du plan actif avant d'autoriser
  l'ajout d'élèves (exposer un port/service interrogeable par le module students)
- les changements de plan (upgrade/downgrade) historisés dans ChangementPlan
  avec calcul du prorata
- fonctionnalités super-admin : création / validation / suspension / suppression
  d'une école, gestion des abonnements et des plans
- ports out de paiement réutilisables avec ceux du module payments si pertinent,
  ou interface FournisseurPaiementGateway propre au module
- tests par couche + vérification Modulith

Ultrathink. Propose le plan d'abord.
```

---

## Étape 2 — Module `iam`

```
Lis CLAUDE.md. Implémente le module "iam" (sécurité et multi-tenant) pour les
tables Utilisateur, Role, Permission, UtilisateurRole, RolePermission.

Inclure impérativement :
- le mécanisme multi-tenant transverse : TenantContext (ThreadLocal), filtre
  HTTP qui lit le tenant depuis le JWT, et filtre Hibernate global sur tenant_id
- l'authentification JWT (login) et l'autorisation RBAC via Spring Security
  (rôles SUPER_ADMIN, ADMIN_ECOLE, DIRECTEUR, COMPTABLE, SECRETAIRE,
  ENSEIGNANT, PARENT, ELEVE)
- architecture hexagonale complète, migrations Flyway, tests par couche
- vérification Modulith : iam expose son API publique, les autres modules ne
  dépendent que de cette API

Ultrathink. Propose le plan d'abord.
```

---

## Étape 3 — Modules `students` et `staff`

```
Lis CLAUDE.md. Implémente les modules "students" et "staff" en architecture
hexagonale.

- students : Eleve, Inscription, DocumentEleve, Parent, EleveParent.
  Fonctions : inscription, réinscription, affectation en classe, dossier élève,
  upload de documents (acte de naissance, photo, certificat médical), liaison
  parents-enfants (un parent peut avoir plusieurs enfants).
- staff : Enseignant. Lien optionnel vers un compte Utilisateur du module iam.

Liaison aux comptes Utilisateur via l'API publique du module iam, jamais par
accès direct. Migrations Flyway, tests, vérification Modulith.

Propose le plan d'abord.
```

---

## Étape 4 — Module `academics`

```
Lis CLAUDE.md. Implémente le module "academics" en architecture hexagonale pour
les tables AnneeScolaire, Periode, Salle, Niveau, Classe, Matiere, ClasseMatiere,
EmploiDuTemps.

Respecte la section "Calendrier scolaire" de CLAUDE.md :
- AnneeScolaire (statut PLANIFIEE/EN_COURS/CLOTUREE/ARCHIVEE, une seule courante
  par tenant) et Periode (trimestre/semestre, clôturable)
- Salle comme entité dédiée (capacité, type, disponibilité) référencée par Classe
  et EmploiDuTemps, avec détection des conflits d'occupation de salle
- Classe et affectations rattachées à une AnneeScolaire (FK), pas un champ texte
- création des niveaux et classes, affectation enseignant responsable et salle,
  gestion des matières (coefficient, programme), affectation
  classe-matière-enseignant, emploi du temps (jour, créneaux, salle) avec vues
  par classe / enseignant / salle
- expose un service de clôture de période (déclenche la génération des bulletins
  côté grading via événement PeriodeCloturee)

Référence les élèves, enseignants et classes des autres modules via leurs API
publiques. Migrations Flyway, tests, vérification Modulith. Ultrathink. Propose
le plan.
```

---

## Étape 5 — Modules `attendance` et `grading`

```
Lis CLAUDE.md. Implémente les modules "attendance" et "grading" en architecture
hexagonale.

- attendance : PresenceEleve (présent / absent / retard, notification parent),
  PresenceEnseignant (présent / absent). À l'enregistrement d'une absence élève,
  publie un événement applicatif AbsenceEnregistree.
- grading : Evaluation (interrogation / devoir / composition / examen avec
  coefficient et note_sur, rattachée à une Periode), Note (saisie, import Excel,
  validation), Examen (blanc / semestriel / annuel / national, convocations),
  Bulletin (en-tête : moyenne générale, rang, mention, décision, statistiques de
  classe) + LigneBulletin (détail par matière : moyenne, coefficient, moyenne
  pondérée, rang matière, appréciation). Calcul selon la section "Détail des
  bulletins" de CLAUDE.md. Génération PDF asynchrone. Écoute l'événement
  PeriodeCloturee (émis par academics) pour générer les bulletins de la période.
  À la génération d'un bulletin, publie BulletinGenere.
  Saisie de notes autorisée uniquement sur une Periode non clôturée d'une année
  EN_COURS.

Communication inter-modules uniquement par événements et API publiques.
Ultrathink. Migrations Flyway, tests, vérification Modulith. Propose le plan.
```

---

## Étape 6 — Module `billing`

```
Lis CLAUDE.md. Implémente le module "billing" en architecture hexagonale pour
les tables FraisScolaire, Echeance, Facture.

Fonctions : paramétrage des frais (inscription, réinscription, mensualité,
transport, cantine, examen) par classe, échéancier, génération de factures par
élève avec montant total / payé / solde restant et statut (impayée / partielle /
payée).

Écoute l'événement InscriptionValidee (émis par students) pour créer
automatiquement la facture d'inscription. Migrations Flyway, tests,
vérification Modulith. Propose le plan d'abord.
```

---

## Étape 7 — Module `payments`

```
Lis CLAUDE.md. Implémente le module "payments" en architecture hexagonale pour
les tables Paiement, TransactionPaiement, Recu.

Respecte le flux décrit dans CLAUDE.md :
Facture -> Paiement -> TransactionPaiement (appel fournisseur) -> attente
callback -> mise à jour statut -> génération Recu.

- ports out : PaiementRepository et FournisseurPaiementGateway (interface)
- adapters out/gateway : OrangeMoneyAdapter, MtnMoneyAdapter, WaveAdapter,
  CarteBancaireAdapter, branchés sur le port FournisseurPaiementGateway
- paiement partiel et complet, références uniques
- à la réussite d'un paiement, publie l'événement PaiementReussi qui déclenche :
  génération du Recu (PDF asynchrone), mise à jour de la Facture côté billing,
  notification côté communication
- ne traite PAS encore les webhooks entrants ici (module webhooks)

Ultrathink. Migrations Flyway, tests (mock du gateway), vérification Modulith.
Propose le plan d'abord.
```

---

## Étape 8 — Module `webhooks`

```
Lis CLAUDE.md. Implémente le module "webhooks" en architecture hexagonale.

- Webhooks ENTRANTS (PaymentWebhook) : endpoint REST qui reçoit les callbacks
  des fournisseurs (Orange Money, MTN, Wave, carte). Vérifie la signature avant
  tout traitement, stocke le payload brut, marque verifie puis traite, et
  notifie le module payments (via son API publique ou un événement) pour mettre
  à jour la transaction correspondante.
- Webhooks SORTANTS (WebhookEndpoint, WebhookEvent) : configuration d'endpoints
  par tenant, émission d'événements (paiement reçu, bulletin disponible,
  absence, inscription) avec mécanisme de retry exponentiel (nb_tentatives,
  prochaine_tentative) traité par un job planifié (@Scheduled).

Sécurité : ne jamais marquer traite=true sans signature valide. Ultrathink.
Migrations Flyway, tests, vérification Modulith. Propose le plan d'abord.
```

---

## Étape 9 — Module `communication`

```
Lis CLAUDE.md. Implémente le module "communication" en architecture hexagonale
pour les tables Communication et MessageDestinataire.

Fonctions : envoi SMS / Email / WhatsApp, individuel ou groupé, aux parents /
élèves / enseignants, pour motifs (retard, absence, échéance de paiement,
résultat, réunion). Suivi du statut de livraison par destinataire.

- ports out : SmsGateway, EmailGateway, WhatsAppGateway (interfaces) avec
  adaptateurs correspondants
- écoute les événements applicatifs des autres modules : PaiementReussi,
  BulletinGenere, AbsenceEnregistree -> envoie la notification adaptée
- communication uniquement par événements et API publiques, jamais d'accès
  direct aux entités d'autres modules

Migrations Flyway, tests (mock des gateways), vérification Modulith. Propose le
plan d'abord.
```

---

## Étape 10 — Portails et tableaux de bord

```
Lis CLAUDE.md. Implémente les couches de présentation (adapters in/web)
transverses qui agrègent les données via les API publiques des modules, sans
violer les frontières Modulith :

- Portail Parent : notes, moyennes, classement, absences, paiements,
  téléchargement bulletin, paiement en ligne, emploi du temps des enfants.
- Portail Élève : notes, moyennes, examens, emploi du temps, documents.
- Tableau de bord Direction : KPIs (nombre d'élèves, taux de paiement, revenus,
  retards de paiement, moyennes générales, taux de réussite, présences) et
  graphiques financiers / pédagogiques.
- Rapports : financiers, pédagogiques, présence, paiement, avec exports PDF et
  Excel.

Ces vues lisent les données via les API exposées des modules (lecture seule),
idéalement par un module "reporting" ou des read-models dédiés. Ultrathink.
Tests, vérification Modulith. Propose le plan d'abord.
```

---

## Vérification finale (après tous les modules)

```
Lance le test ApplicationModules.of(Application.class).verify() et génère la
documentation des modules avec Documenter. Liste toute violation de frontière
restante et corrige-la. Puis exécute l'ensemble de la suite de tests et fais un
récapitulatif de la couverture par module.
```
