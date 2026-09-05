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
- Plusieurs modules récents (comptabilité générale, cantine, calendrier, blog, DPE, paramètres
  d'établissement) n'ont toujours pas d'entrée dans le catalogue de permissions : ils continuent de
  fonctionner avec les rôles codés en dur existants. Paie, biométrie, transport, dépenses/budget/
  subventions, discipline, devoirs, cahier de textes, cartes élèves, annonces et modèles de message
  ont depuis été ajoutés au catalogue (voir §8).

*Résultat attendu* : les permissions cochées sur un rôle personnalisé pilotent réellement l'accès aux
fonctionnalités couvertes par le catalogue actuel (voir liste des modules migrés ci-dessus et §8).

---

## 8. Créer un rôle personnalisé (ex. « Surveillant », « Économe ») et voir les bons menus

**Contexte** : le backend permettait déjà de créer un rôle avec seulement un nom (`POST /roles`, pas
de code fixe imposé) et de lui affecter des permissions, mais **les menus de la barre latérale et
les onglets de page restaient filtrés par une liste de rôles codée en dur** (`SCHOOL_ADMIN`,
`DIRECTOR`, etc.) — un rôle personnalisé n'y correspondait donc jamais, même avec les bonnes
permissions cochées. La navigation vérifie désormais **le rôle OU la permission**. Une vingtaine de
modules qui n'avaient encore aucune entrée dans le catalogue de permissions (cartes scolaires,
transport, devoirs, cahier de textes, discipline, santé/infirmerie, documents & modèles, budget,
dépenses, subventions, paie, biométrie, annonces, modèles de message) ont été ajoutés à cette
occasion, sinon la permission cochée n'aurait servi à rien côté serveur.

**Où** : `/users/roles`.

1. Cliquer **Nouveau rôle**, saisir un nom (ex. « Surveillant ») + description, créer.
2. Ouvrir **Permissions** sur ce nouveau rôle, cocher par exemple `presence:read`, `presence:create`,
   `presence:update`, `discipline:manage`, `carte:manage` — enregistrer.
3. Créer un utilisateur, lui affecter ce rôle (`/users`).
4. Se connecter avec ce compte : la sidebar doit afficher **Présences**, **Vie scolaire → Discipline**
   et **Cartes scolaires (badges)**, sans qu'aucun de ces libellés n'apparaisse dans le code sous ce
   nom de rôle — uniquement via les permissions cochées.
5. Vérifier qu'un menu correspondant à une permission **non cochée** (ex. « Paie ») reste invisible,
   et que naviguer directement vers son URL (`/payroll`) redirige/bloque plutôt que d'afficher une
   page cassée.
6. Retirer une permission (ex. `carte:manage`) depuis `/users/roles`, se reconnecter : le menu
   correspondant doit disparaître immédiatement.

**Limites connues (documentées en commentaire dans `navigation.ts`)** : Tableau de bord, Vision
Fondateur, Rapports, Classes, Personnel, Messages, Paramètres, Annuaire inter-écoles et quelques
écrans de configuration (paramètres de notation, comptabilité générale, cantine, calendrier, DPE,
blog) restent accessibles uniquement aux rôles système — soit parce qu'aucune permission ne
correspond avec assez de précision à l'accès actuel sans élargir ou restreindre involontairement
l'accès d'un rôle existant, soit par sensibilité (recherche cross-établissements).

*Résultat attendu* : un rôle entièrement nouveau, créé depuis l'interface, donne accès aux bons
menus et aux bonnes pages dès que les permissions correspondantes sont cochées — sans toucher au code.

---

## 9. SMS réservés aux élèves ayant accepté l'accès plateforme

**Contexte** : avant ce correctif, les SMS (encaissements, retards, absences, annonces) partaient
vers **tous** les parents, qu'ils aient accepté ou non le frais annexe d'accès à la plateforme
(§6). Désormais, seuls les parents d'un élève ayant accepté ce frais reçoivent ces notifications.

**Où** : `/enrollments` (case « Accès plateforme » à l'inscription), `/attendance`, `/invoices`,
`/communications/announcements`.

1. Inscrire un élève **sans** cocher l'accès plateforme. Le déclarer absent (`/attendance`) : le
   parent ne reçoit **aucun** SMS d'absence.
2. Inscrire un second élève **avec** l'accès plateforme accepté. Le déclarer absent : son parent
   reçoit bien le SMS.
3. Encaisser un paiement de frais pour l'élève sans accès plateforme : aucun SMS de confirmation
   n'est envoyé à son parent. Encaisser pour l'élève avec accès : le parent reçoit la confirmation.
4. Envoyer une diffusion « À tous les parents » (`/communications/announcements` → diffusion) :
   seul le parent de l'élève ayant accepté l'accès plateforme figure parmi les destinataires SMS.
5. Envoyer une diffusion ciblée sur la classe des deux élèves : même filtrage, seul le parent de
   l'élève avec accès reçoit le SMS.

*Résultat attendu* : le frais annexe d'accès plateforme conditionne désormais réellement l'envoi des
SMS (encaissements, retards/absences, annonces), plus seulement la facturation.

---

## 10. Rappel (webhook) de statut de livraison NimbaSMS

**Contexte** : le rappel Nimba n'était pas implémenté. Il est maintenant reçu sur
`POST /api/v1/webhooks/sms/nimba` (public, hors authentification JWT comme tout webhook entrant),
au format confirmé par la documentation Nimba : `{ messageid, status: "received"|"failed", contact,
metadata }`.

**Où** : tableau de bord Nimba (API KEYS → Webhooks) à configurer avec l'URL de production, ex.
`https://api.cathclau-edu.com/api/v1/webhooks/sms/nimba` (à adapter au domaine réellement déployé).

1. Envoyer un SMS depuis l'application (ex. une absence). Noter l'identifiant du message envoyé
   (visible dans les logs serveur `[SMS webhook]` ou via `/communications` → détail de l'envoi).
2. Une fois Nimba configuré avec l'URL ci-dessus, vérifier dans les logs serveur qu'un rappel arrive
   (`[SMS webhook] Nimba rawPayload=...`) et que le statut de livraison du destinataire correspondant
   passe de « En attente » à « Livré » (ou « Échoué ») dans `/communications` → détail de l'envoi.
3. Rejouer volontairement un rappel avec un `messageid` inconnu : aucune erreur ne doit remonter à
   Nimba (toujours `200 OK`), seul un log « Aucun message correspondant trouvé » apparaît côté
   serveur.

*Résultat attendu* : le statut réel de livraison (livré/échoué) remonté par Nimba met à jour le
suivi des envois, au lieu de rester figé sur « accepté par le fournisseur ».

---

## 11. Cours en ligne payants (vidéo/PDF par classe)

**Contexte** : nouveau module permettant à l'école de vendre des cours (vidéo/PDF) par classe et
matière. Le paiement réutilise entièrement le circuit existant des frais scolaires (facture +
paiement Djomy) : aucun circuit de paiement séparé n'a été créé. La protection du contenu est un
lecteur web qui bloque le clic-droit et le téléchargement direct (pas un DRM).

**Où** : menu **Pédagogie → Cours en ligne** (personnel) ou **Mon espace → Cours en ligne**
(parent/élève), route `/courses`.

1. **Création (personnel avec la permission `cours:manage` — SCHOOL_ADMIN/DIRECTOR/
   RESPONSABLE_PEDAGOGIQUE/TEACHER)** : onglet **Gestion** → *Nouveau cours* → choisir une classe,
   saisir matière (« Mathématiques »), titre, description, prix, image de couverture → créer.
2. Onglet **Contenu** du cours créé : ajouter une ressource **Vidéo** (fichier vidéo) puis une
   ressource **PDF** (fichier PDF) via le bouton de téléversement.
3. Tenter de **publier** le cours *avant* d'avoir ajouté une ressource : le bouton doit refuser
   (« Ajoutez au moins une ressource… ») — ajouter une ressource lève le blocage, puis publier.
4. Se connecter en **parent** (ou **élève**) : le cours publié apparaît dans le **Catalogue**, avec
   son prix ; un cours non publié n'apparaît jamais côté parent/élève.
5. Ouvrir le cours depuis le catalogue : la liste des ressources s'affiche avec un badge
   **Verrouillé** (aucune lecture possible), puis cliquer **Acheter**.
6. Suivre le paiement Djomy (mobile money / carte) jusqu'à confirmation : une fois le paiement
   réussi, le cours passe en **Acheté** et les ressources se déverrouillent (lecture vidéo et PDF
   disponibles depuis `/courses` → onglet **Mes achats** ou directement depuis le catalogue).
7. Vérifier qu'un clic droit sur la vidéo ou le PDF affiché ne propose pas d'option de
   téléchargement/enregistrement (protection « lecteur web », pas un DRM).
8. Élève/parent n'ayant **pas** acheté le cours : les ressources restent verrouillées (aperçu titre
   seul, sans lien de lecture), même en accédant directement à la fiche du cours.
9. Tenter de **supprimer** un cours déjà acheté par au moins un élève (`/courses` → Gestion) : le
   système doit refuser (« déjà acheté… ») ; dépublier reste possible.
10. Un parent avec plusieurs enfants : vérifier le sélecteur d'enfant en haut de `/courses` et que
    l'achat/l'accès sont bien rattachés à l'enfant choisi, pas à un autre.

*Résultat attendu* : un cours n'est vendable qu'une fois publié avec au moins une ressource ; son
achat suit exactement le même circuit de paiement (Djomy) que les frais scolaires ; le contenu ne se
déverrouille qu'après règlement complet de la facture générée.
