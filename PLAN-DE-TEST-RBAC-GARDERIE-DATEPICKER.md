# Plan de test — RBAC permissions personnalisées, cycle Garderie, sélecteur de date, heures enseignant

> Correctifs liés à un rôle personnalisé (Comptable) qui n'obtenait pas l'accès malgré des
> permissions cochées, à la confusion Garderie/Maternelle dans le référentiel des niveaux, et au
> warning React du sélecteur de date.

---

## 1. Sélecteur de date — plus de warning "uncontrolled to controlled"

**Où** : n'importe quel formulaire avec un champ date (ex. `/students` → créer un élève → date de
naissance, ou `/academic-years` → dates de l'année scolaire).

1. Ouvrir la console navigateur (F12).
2. Ouvrir un formulaire avec un champ date vide, puis choisir une date dans le calendrier.
3. Vérifier qu'**aucun** warning `component is changing an uncontrolled input to be controlled`
   n'apparaît plus dans la console.

*Résultat attendu* : le champ affiche la date choisie normalement, sans warning.

---

## 2. Cycle Garderie distinct de la Maternelle

**Où** : `/academic-years` (ou page des niveaux) → onglet **Niveaux** → **Catalogue**.

1. Ouvrir le catalogue des niveaux : la case **Garderie** est toujours proposée à part des sections
   Petite/Moyenne/Grande Section.
2. Créer le niveau **Garderie** (s'il n'existe pas déjà) puis consulter sa fiche : la colonne
   **Cycle** doit afficher **Garderie**, pas **Maternelle**.
3. Pour une école existante où « Garderie » avait déjà été créée avec le cycle Maternelle avant ce
   correctif : après déploiement, vérifier que son cycle a basculé automatiquement sur **Garderie**
   (migration de données), sans intervention manuelle.
4. Créer une classe rattachée au niveau Garderie, y inscrire un élève, puis sur `/attendance/scan`
   scanner son badge de sortie : vérifier que le **badge de retrait obligatoire** (nom + relation de
   la personne qui récupère l'enfant) est toujours demandé, comme pour la Maternelle/le Primaire.

*Résultat attendu* : Garderie a son propre cycle, distinct de la Maternelle, sans perdre la sécurité
du retrait obligatoire à la sortie.

---

## 3. Heures enseignant réalisées — marge de transition, récréation, départ anticipé

**Où** : `/attendance` → onglet **Réglages** (SCHOOL_ADMIN/DIRECTOR uniquement) pour la
configuration ; onglet **Présences enseignants** → section « Récap mensuel » pour le résultat ;
`/payroll` pour la fiche de paie.

1. Sur l'onglet **Réglages**, régler la **marge de transition** (ex. 5 minutes) et ajouter une
   **fenêtre de récréation** (ex. 10h00–10h15).
2. Créer un emploi du temps où un enseignant a un cours 10h-12h dans une salle A puis 12h-14h dans
   une salle B (salles différentes, back-to-back).
3. Sur `/attendance/scan`, scanner l'entrée de l'enseignant à 12h04 pour la séance 12h-14h : avec une
   marge de 5 min, ce n'est **pas** compté en retard (arrivée avant 12h05).
4. Scanner l'entrée à 12h07 pour la même séance : **2 minutes** de retard doivent être comptées (pas 7).
5. Pour un cours dont le début (10h00) tombe dans la fenêtre de récréation 10h00–10h15 : scanner
   l'entrée à 10h12 → **aucun retard** compté (attendu seulement à partir de 10h15).
6. Scanner une sortie **avant** la fin prévue de la séance (ex. sortie à 13h55 pour une séance
   finissant à 14h00) : vérifier que 5 minutes de **départ anticipé** apparaissent dans le récap
   mensuel de l'enseignant (`/attendance` → Présences enseignants → Récap mensuel).
7. Ne **pas** scanner de sortie du tout pour une séance : vérifier qu'aucune pénalité de départ
   anticipé n'est appliquée (bénéfice du doute — comportement volontaire).
8. Générer la fiche de paie du mois pour cet enseignant (`/payroll`) : le retard cumulé et le départ
   anticipé cumulé apparaissent sur le PDF de la fiche de paie (section heures), et les heures
   effectives = réalisées − retard − départ anticipé.

*Résultat attendu* : le retard/départ anticipé tient compte du temps de trajet entre salles et des
récréations définies par l'école, et les deux sont visibles sur le récap mensuel et la fiche de paie.

---

## 4. Scanner QR — résolution et robustesse

**Où** : `/attendance/scan`, depuis un téléphone.

1. Ouvrir le scanner sur un téléphone (caméra arrière). Présenter une carte avec QR à différentes
   distances et légèrement inclinée : vérifier que la détection reste fiable (avant ce correctif, la
   résolution de capture était plafonnée à 480px, ce qui pénalisait les codes petits/inclinés).

*Résultat attendu* : scan plus robuste sur téléphone, sans changement de comportement sur desktop.

---

## 5. Connexion par numéro de téléphone

**Où** : page de connexion (`/login`).

1. Se connecter avec le numéro de téléphone d'un utilisateur (élève, parent, enseignant ou
   personnel) au lieu de son email, avec son mot de passe habituel.

*Résultat attendu* : fonctionnait déjà côté backend et frontend (label « Email, identifiant ou
téléphone ») — aucune modification nécessaire, vérifié pendant cette session.

---

## 6. Frais annexe : accès à la plateforme optionnel par élève

**Contexte** : avant ce correctif, le frais d'usage plateforme (fixé par le SUPER_ADMIN) était
ajouté **automatiquement à toutes les mensualités de tous les élèves**, sans que le parent n'ait
donné son accord. Désormais, il n'est ajouté que si le parent a explicitement accepté l'accès à la
plateforme pour son enfant — à l'inscription ou plus tard.

**⚠️ Impact immédiat au déploiement** : toutes les inscriptions déjà existantes repartent sur
« non activé ». Si vous voulez que des élèves déjà inscrits continuent à payer le frais plateforme,
il faudra réactiver l'accès école par école (ou classe par classe) après le déploiement — c'est un
choix assumé (voir échange avec Claude), pas un oubli.

**Où** : `/enrollments` (page Inscriptions) → bouton **Inscrire** ou **Modifier**.

1. Inscrire un nouvel élève : dans le formulaire, cocher **« Accès de l'élève à la plateforme »**.
2. Générer les mensualités de cet élève (`/invoices` → Frais scolaires → Générer) : vérifier que
   chaque facture mensuelle inclut le frais plateforme configuré (`/platform/frais-plateforme`,
   SUPER_ADMIN).
3. Inscrire un second élève **sans** cocher l'accès plateforme : vérifier qu'aucune facture générée
   pour lui n'inclut ce frais.
4. Sur une inscription existante, cliquer **Modifier** et cocher/décocher l'accès plateforme a
   posteriori : vérifier que les mensualités **déjà émises** ne changent pas rétroactivement (le
   montant est figé à la génération), mais que les **prochaines** mensualités générées reflètent le
   nouveau choix.
5. Dans la liste des inscriptions (`/enrollments`), vérifier la colonne **Plateforme** (badge
   Activé/Non activé) reflète bien le choix de chaque élève.

*Résultat attendu* : le frais plateforme n'apparaît que sur les mensualités des élèves dont la
famille a explicitement accepté l'accès, réparti automatiquement sur tous les mois de l'année tant
que l'opt-in reste actif.

---

## 7. RBAC — permissions d'un rôle personnalisé réellement appliquées

**Contexte** : de nombreux endpoints vérifiaient une liste de rôles codée en dur (ex.
`hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY','TEACHER')`) au lieu des permissions cochées sur
le rôle (`Comptable`, `Directeur`, etc. → onglet **Rôles & permissions**). Résultat : cocher
`eleve:read` sur un rôle personnalisé ne suffisait pas si ce rôle n'était pas dans la liste codée en
dur de l'endpoint. La plupart des endpoints ont été migrés pour vérifier réellement les permissions.

**Où** : `/roles` → onglet **Rôles & permissions**.

1. Se connecter avec un utilisateur ayant le rôle `Comptable` (ACCOUNTANT).
2. Aller sur **Guichet d'accueil** (`/guichet`) ou **Élèves** (`/students`) : la liste des élèves doit
   s'afficher sans erreur *Access denied* (c'était le bug initial rapporté).
3. Depuis `/roles`, retirer une permission au rôle `Comptable` (ex. `facture:read`), se reconnecter
   avec ce compte, et vérifier que la fonctionnalité correspondante (ex. `/invoices`) devient
   effectivement inaccessible — pas seulement cachée dans le menu, mais bloquée côté serveur (403).
4. Réattribuer la permission, se reconnecter, vérifier que l'accès revient.

**Points de vigilance identifiés pendant la migration (à valider avec le fondateur/l'équipe métier)** :
- Le rôle **Directeur** perd désormais la création/modification/suppression directe des élèves,
  enseignants, notes et évaluations (il garde la lecture + validation d'inscription/notes) — c'est ce
  que la table des permissions (`/roles`) prévoyait déjà, mais le code ne l'appliquait pas avant ce
  correctif. Si un Directeur doit pouvoir modifier une fiche élève directement, ajouter les
  permissions `eleve:update`/`eleve:create` etc. à son rôle depuis `/roles` (pas besoin de redéployer).
- Le rôle **Responsable Pédagogique** (RESPONSABLE_PEDAGOGIQUE) n'a **aucune permission** dans le
  catalogue actuel (`/roles` → son profil doit être complété manuellement) : les écrans de
  configuration des notes (types d'évaluation, règles de décision/élimination, formules de calcul,
  workflow de validation) n'ont pas été touchés par cette migration pour ne pas lui couper l'accès —
  ils continuent de fonctionner via l'ancienne vérification de rôle codée en dur, en attendant que ses
  permissions soient définies.
- Plusieurs modules récents (comptabilité générale, paie, biométrie, transport, dépenses, cantine,
  discipline, devoirs, cours, calendrier, annonces, subventions, cartes élèves, blog, DPE) n'ont pas
  encore d'entrée dans le catalogue de permissions : ils continuent de fonctionner avec les rôles codés
  en dur existants, inchangés par cette migration.

*Résultat attendu* : les permissions cochées sur un rôle personnalisé pilotent réellement l'accès aux
fonctionnalités couvertes par le catalogue actuel (voir liste des modules migrés ci-dessus).
