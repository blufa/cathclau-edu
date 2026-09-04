# Plan de test — Batch paie / permissions / inscription / chef de classe / horaires / moniteur / scan

> Ce document liste, fonctionnalité par fonctionnalité, où cliquer pour retrouver et vérifier chaque
> modification livrée. Se connecter avec un compte `SCHOOL_ADMIN` ou `DIRECTOR` sauf indication contraire.
> Mis à jour avec les correctifs du scan de badge, la réinscription d'un ancien élève absent de la
> plateforme, l'avantage « payé pendant les vacances » et la projection de paie à durée réelle (§3, 3bis, 9, 10).

---

## 1. Paie — jours fériés exclus du calcul des heures dues

**Où** : menu **Paie** (`/payroll`) → onglet **Rémunération**.

1. Choisir un enseignant qui a un cours régulier un jour donné (ex. tous les lundis) dans son emploi du temps.
2. Dans le menu **Année scolaire & calendrier** (`/academic-years`, PAS « Frais scolaires » — le
   calendrier n'y est pas rattaché), sur la fiche de l'année scolaire, section **« Calendrier »**, vérifier
   qu'un événement **Férié** ou **Vacances** existe sur un mois donné et recoupe au moins un des jours où
   l'enseignant a cours (ex. un lundi férié dans le mois choisi). Sinon, en créer un via **« Ajouter un
   événement »** sur cette même page.
3. Sur la fiche enseignant (onglet Rémunération), sélectionner ce mois dans le champ **Mois**.
4. Regarder le panneau **« Estimation prévisionnelle (emploi du temps) »** : le nombre d'heures affiché
   doit être **inférieur** à ce qu'il aurait été si tous les lundis du mois étaient comptés (avant le
   correctif, le jour férié était compté comme un jour de classe normal).

*Résultat attendu* : le jour férié n'est plus compté dans les heures dues du mois.

---

## 2. Paie — distinction bulletin réel vs estimation

**Où** : `/payroll` → onglet **Rémunération**, avec un enseignant sélectionné et un mois choisi.

1. Constater qu'il y a maintenant **deux panneaux bien distincts** :
   - **« Paie du mois »** (fond blanc) : basé sur les heures **réalisées** (cahier de textes, séances
     marquées « Réalisée »). Bouton **« Générer la fiche de paie »** → c'est le bulletin réel.
   - **« Estimation prévisionnelle (emploi du temps) »** (fond orange clair) : basé sur les heures
     **théoriques** de l'emploi du temps, qu'une séance ait eu lieu ou non. Un texte d'avertissement
     explique de ne pas l'utiliser comme bulletin définitif. Bouton renommé **« Générer une estimation »**.
   - Si l'enseignant est payé **à l'heure**, un texte orange rappelle : *« Enseignant payé à l'heure :
     préférez « Paie du mois » pour le bulletin réel »*.
2. Cliquer sur **« Générer une estimation »** → le message de succès précise *« à ne pas confondre avec
   le bulletin réel »*.

*Résultat attendu* : impossible de confondre visuellement les deux, contrairement à avant où les deux
boutons se ressemblaient.

---

## 3. Paie — projection jusqu'à la fin de l'année scolaire (durée réelle, plus de 9 mois figés)

**Où** : menu **Vision Fondateur** (`/founder`).

1. Descendre jusqu'à la section **« Estimation de la masse salariale sur l'année scolaire »** (juste après
   la section « Masse salariale du mois »).
2. Vérifier le tableau : une ligne par mois, **du mois actuellement affiché jusqu'à la date de fin réelle
   de l'année scolaire sélectionnée** (colonne `endDate` de l'année scolaire, module Académique) — plus un
   nombre fixe de 9. Sur une année scolaire de 10 mois, le tableau doit avoir jusqu'à 10 lignes (moins
   celles déjà passées si le mois affiché est en cours d'année) ; changer l'année scolaire courante dans le
   sélecteur du haut de page pour vérifier que la longueur du tableau s'ajuste.
3. Vérifier en bas les 3 totaux : **Total enseignants**, **Total personnel**, **Total général**.

*Résultat attendu* : la longueur de la projection suit la vraie durée de l'année scolaire configurée, pas
un nombre de mois codé en dur.

## 3ter. Vision Fondateur — salaire de chaque enseignant (pas seulement le total)

**Où** : menu **Vision Fondateur** (`/founder`), nouvelle section **« Salaire par enseignant »** (juste
après « Masse salariale du mois »).

1. Vérifier le tableau : une ligne par enseignant, colonnes **Enseignant**, **Ce mois**, **Estimation
   annuelle**.
2. Vérifier que la somme de la colonne « Ce mois » correspond au total « Enseignants » affiché dans la
   section « Masse salariale du mois » juste au-dessus.
3. Vérifier qu'un enseignant à salaire fixe affiche le même montant chaque mois dans sa colonne « Ce
   mois », et que « Estimation annuelle » correspond à ce montant × le nombre de mois restants jusqu'à la
   fin de l'année scolaire (voir §3).

*Résultat attendu* : on voit désormais le détail par enseignant, pas seulement l'agrégat de toute l'école.

## 3bis. Paie — avantage « payé pendant les vacances »

**Où** : `/payroll` → onglet **Rémunération**, formulaire **« Mode de rémunération »** d'un enseignant.

1. Cocher **« Payé même pendant les vacances (avantage accordé par la direction) »**, puis **Enregistrer**.
2. Choisir un mois contenant un jour férié/vacances où cet enseignant a normalement cours → dans le
   panneau **« Estimation prévisionnelle »**, ce jour doit maintenant être **compté** dans les heures dues
   (contrairement au point 1, qui reste le comportement par défaut quand la case n'est PAS cochée).
3. Décocher la case, enregistrer à nouveau → le jour férié redevient exclu pour ce même mois.

*Résultat attendu* : le comportement par défaut (jours fériés exclus, point 1) reste inchangé pour tous les
enseignants ; seuls ceux explicitement marqués bénéficient de l'avantage.

---

## 4. Permissions par utilisateur (en plus du rôle)

**Où** : menu **Utilisateurs** (`/users`).

1. Choisir un utilisateur qui a le rôle **Comptable** (ou tout rôle autre qu'admin).
2. Cliquer sur **« Rôles »** (bouton dans la ligne de l'utilisateur) → une modale **« Rôles & permissions
   de l'utilisateur »** s'ouvre.
3. En haut : la liste des rôles (inchangée). En dessous, une nouvelle section **« Permissions
   supplémentaires »** avec un texte explicatif et une grille de permissions groupées par ressource
   (identique à l'écran Rôles & permissions).
4. Cocher par exemple `eleve:create` (dans le groupe « eleve »), cliquer **Enregistrer**.
5. Message de succès : *« Permissions supplémentaires mises à jour. Elles seront rafraîchies à la
   prochaine connexion. »*
6. **Se déconnecter et se reconnecter avec le compte comptable concerné**, puis vérifier qu'il peut
   maintenant créer un élève (bouton « Nouvel élève » sur la page Élèves) alors que son rôle Comptable ne
   le permettait pas auparavant.

*Résultat attendu* : le compte a la permission en plus de son rôle, sans qu'un rôle personnalisé ait dû
être créé.

---

## 5. Abonnement transport/cantine à l'inscription

**Où** : fiche élève → bouton **« Inscrire »** (ou depuis la liste des élèves).

1. Ouvrir la modale d'inscription (**création**, pas modification) pour un élève non encore inscrit
   dans l'année.
2. Choisir une classe. En dessous du champ **Type**, une nouvelle section **« Abonnements (optionnel) »**
   apparaît :
   - **« Abonner au transport »** : si des lignes de transport existent (module Transport), cocher fait
     apparaître 3 menus : **Ligne**, **Arrêt** (optionnel), **Sens**.
   - **« Abonner à la cantine »** : actif seulement si un frais **CANTINE** est configuré pour la classe
     choisie (Frais scolaires) ; sinon le texte *« Configurez le frais cantine dans Frais scolaires pour
     cette classe »* s'affiche et la case reste grisée.
3. Cocher transport (choisir une ligne) et/ou cantine, puis cliquer **Inscrire**.
4. Vérifier :
   - Module **Transport → Abonnements** : un nouvel abonnement actif pour l'élève sur la ligne choisie.
   - Fiche élève → factures : une facture cantine créée immédiatement (si cochée) ; pour le transport,
     les factures de la ligne s'appliquent automatiquement selon les tranches déjà configurées sur le
     frais TRANSPORT de cette ligne (Frais scolaires).

*Résultat attendu* : plus besoin de jongler entre plusieurs écrans après l'inscription pour abonner
l'élève.

---

## 6. Chef de classe + scan des badges

**Où** : `/classes/{id}` (fiche d'une classe).

1. Sur la fiche d'une classe ayant des élèves inscrits, repérer la nouvelle carte **« Chef de classe »**
   (sous le tableau matières/enseignants).
2. Choisir un élève inscrit dans le menu déroulant → message *« Chef de classe mis à jour »*.
3. **Connecté avec le compte élève désigné** (portail élève), tenter de scanner le badge d'un camarade
   de SA classe → le pointage doit fonctionner (voir §9 ci-dessous pour le scan lui-même, corrigé).
4. **Connecté avec un autre compte élève** (non désigné chef de classe), tenter le même scan → doit être
   **refusé** (message : *« Seul le chef de classe désigné peut scanner les badges de cette classe »*).

---

## 9. Scan de badge — correctif caméra qui ne détectait rien

**Contexte du bug** : la caméra s'allumait mais aucun QR n'était jamais détecté sur les navigateurs sans
l'API native `BarcodeDetector` (Firefox, la plupart des versions de Safari iOS) — silencieux, sans message
clair. Le scan utilise maintenant un décodeur JavaScript (bibliothèque `jsQR`) qui fonctionne sur tous les
navigateurs, plus seulement ceux qui supportent l'API native.

**Où** : menu **Pointage par scan de carte** (`/attendance/scan`).

1. Imprimer ou afficher à l'écran (téléphone) le badge/carte scolaire d'un élève (page **Cartes
   scolaires**, `/student-cards`).
2. Sur `/attendance/scan`, choisir le sens du scan (Entrée/Sortie — voir §13), cliquer **« Démarrer le
   scan »**, autoriser l'accès caméra.
3. Présenter le QR du badge devant la caméra, à une distance raisonnable et bien éclairé.
4. Vérifier qu'un pointage apparaît dans **« Pointages récents »** en quelques secondes, sans avoir besoin
   d'un navigateur particulier (tester notamment sur le navigateur/téléphone qui posait problème
   auparavant).
5. Vérifier aussi le pointage manuel (recherche par nom, à droite) comme solution de secours si jamais un
   QR reste illisible (angle, reflet, impression de mauvaise qualité).

*Résultat attendu* : le scan fonctionne quel que soit le navigateur/appareil utilisé pour le test.

---

## 7. Horaire fixe (multi-jours) + garderie par blocs

**Où** : menu **Emploi du temps** (`/timetable`), en mode **« Par classe »**.

1. Choisir une classe, cliquer sur le nouveau bouton **« Horaire fixe »** (entre « Générer automatiquement »
   et « Ajouter un créneau »).
2. Choisir une matière, un enseignant, cocher plusieurs jours (ex. Lundi à Vendredi), renseigner une heure
   de début/fin (ex. 08:00–13:00 pour une maternelle) → cliquer **« Créer l'horaire »**.
3. Vérifier que l'emploi du temps de la classe affiche bien un créneau identique chaque jour coché.
4. Recommencer en cochant **« Répéter par blocs »** avec par exemple 30 min de bloc / 10 min de pause,
   entre 08:00 et 10:00 → vérifier l'aperçu du nombre de blocs généré, valider, et vérifier que plusieurs
   créneaux courts sont créés dans la journée (ex. garderie).

*Résultat attendu* : plus besoin de créer les créneaux un par un pour un horaire répétitif.

---

## 8. Raccourci « moniteur » — un enseignant sur toutes les matières d'une classe

**Où** : fiche enseignant → modale de création/modification → onglet/section **« Classes & matières
enseignées »**.

1. Ouvrir la fiche d'un enseignant (ex. un moniteur de maternelle).
2. Choisir une classe dans la ligne d'affectation → si des matières sont déjà affectées à cette classe
   (curriculum défini par ailleurs), un bouton **« Affecter à toutes les matières »** apparaît à côté du
   menu Matière.
3. Cliquer dessus → message *« Affecté à X matière(s) de cette classe »*.
4. Vérifier dans l'emploi du temps / les affectations classe↔matière que l'enseignant est bien rattaché à
   toutes les matières de cette classe.

*Résultat attendu* : plus besoin de répéter l'opération matière par matière pour un enseignant qui couvre
tout le programme d'une classe (cas typique maternelle/primaire).

---

## 10. Réinscription d'un ancien élève absent de la plateforme

**Contexte** : le backend supportait déjà ce cas (aucune inscription préalable n'est requise pour choisir
« Réinscription » — seul le type d'inscription détermine le tarif facturé), mais rien à l'écran ne le
suggérait. Deux améliorations :

**Où** : liste des élèves (`/students`) → **« Nouvel élève »**.

1. Créer un élève comme d'habitude (un ancien élève de l'établissement qui n'était jamais entré dans le
   logiciel, par exemple).
2. Juste après la création, la modale **« Inscrire l'élève »** s'ouvre automatiquement (nouveauté).
3. Dans le champ **Type**, vérifier les nouveaux libellés :
   - *« Inscription (nouvel élève, jamais scolarisé ici) »*
   - *« Réinscription (déjà scolarisé ici auparavant) »* — un texte d'aide sous le champ précise qu'un
     ancien élève absent du logiciel peut choisir cette option, et que le tarif de réinscription
     s'appliquera même sans historique dans le système.
4. Choisir **Réinscription**, une classe, valider.
5. Vérifier la facture générée sur la fiche élève : elle doit correspondre au frais **RE_ENROLLMENT**
   (réinscription) configuré pour la classe, pas au frais **ENROLLMENT** (inscription).

*Résultat attendu* : un ancien élève inconnu du logiciel peut être créé puis inscrit au tarif réinscription
en une seule séquence, sans manipulation supplémentaire.

---

## 11. Abonnement transport à l'encaissement (pas seulement à l'inscription)

**Où** : fiche élève → **« Facture & encaissement »** (bouton ouvrant `CollectFeeModal`, ex. depuis
Guichet d'accueil ou la fiche élève).

1. Pour un élève **non encore abonné au transport**, vérifier la présence du bouton **« Abonner au
   transport »** (juste au-dessus de « Ajouter un frais annexe »).
2. Cliquer dessus, choisir une ligne (+ arrêt optionnel + sens), cliquer **« Abonner »**.
3. Vérifier dans le module **Transport → Abonnements** que l'élève apparaît bien abonné.
4. Rouvrir la modale d'encaissement pour ce même élève : le bouton **« Abonner au transport »** ne
   doit plus apparaître (déjà abonné).
5. Pour la cantine : vérifier qu'un frais **CANTINE** configuré pour la classe apparaît bien dans le menu
   **« Ajouter un frais annexe »** (déjà existant, pas une nouveauté de ce point).

*Résultat attendu* : un parent peut désormais s'abonner au transport aussi bien à l'inscription qu'au
moment de payer les frais scolaires, sans devoir passer par le module Transport séparément.

---

## 12. Remise paiement anticipé — uniquement sur le tout premier paiement de l'année

**Règle** : la remise « paiement annuel complet » ne récompense que le choix de payer TOUTE l'année dès
le **premier** règlement de scolarité de l'élève sur ce frais. Dès qu'une tranche a été payée (même
partiellement) sans elle, la remise ne s'applique plus jamais sur ce frais pour cet élève — même si le
parent décide ensuite de régler le reste d'un coup.

**Où** : fiche élève → **« Facture & encaissement »**, avec un frais MENSUALITE ayant plusieurs tranches,
et une règle de remise paiement anticipé active (**Frais scolaires → Remises paiement anticipé**).

**Cas 1 — premier paiement, toute l'année d'un coup (remise applicable)**
1. Sur un élève dont **aucune tranche n'a encore été payée**, cocher directement **« Toute la
   scolarité »** dès le premier encaissement.
2. Vérifier le message **« Remise scolarité complète appliquée : −X »**, avec X = pourcentage × total
   annuel du frais (toutes les tranches, puisqu'aucune n'est encore payée).

**Cas 2 — une tranche déjà payée avant de vouloir régler le reste (remise NON applicable)**
1. Sur un élève, payer normalement une seule tranche (sans cocher « Toute la scolarité »).
2. Revenir plus tard sur la même fiche, cocher **« Toute la scolarité »** pour régler les tranches
   restantes.
3. Vérifier qu**aucun message de remise ne s'affiche** et qu'aucune tranche ne montre de « (remise
   −X) » à côté de son libellé — le montant à payer reste le plein tarif des tranches restantes.

*Résultat attendu* : la remise n'est jamais accordée après coup ; elle ne récompense que la décision prise
dès le premier paiement de l'année.

---

## 13. Scan entrée/sortie automatique (remplace le choix manuel Présent/Absent/Retard)

**Contexte** : deux bugs corrigés avant ce chantier, sans lien avec la logique métier :
- `tenant_id` jamais enregistré sur les tables de pointage (`student_attendances`/`teacher_attendances`) —
  les entités JPA n'avaient tout simplement jamais ce champ, malgré la colonne `NOT NULL` en base depuis le
  début. Chaque scan échouait donc systématiquement, indépendamment du compte/session utilisé. Corrigé.
- Le débounce anti-double-scan de la page caméra était indexé uniquement sur le contenu du QR, pas sur le
  statut choisi : scanner « Présent » puis rescanner la même carte avec « Absent »/« Retard » dans les 3
  secondes ne faisait rien (silencieusement). Corrigé (débounce désormais par carte + sens du scan).

**Changement de fond** : les boutons manuels Présent/Absent/Retard ont disparu de la page de scan. Le
système calcule automatiquement le statut à partir de l'emploi du temps :
- **Entrée** : compare l'heure du scan à l'heure de début du cours en cours (classe de l'élève / emploi du
  temps de l'enseignant) → **Présent** si à l'heure, **Retard (+N min)** sinon. Fonctionne par **séance**
  (pas une fois par jour) : un cours 8h-10h puis 10h-12h donnent chacun leur propre pointage d'entrée.
- **Sortie** : capture l'heure de sortie réelle et affiche la durée réalisée comparée à la durée prévue du
  créneau (ex. « 95 min réalisées / 120 min prévues (−25 min) »).
- **Absent** ne peut plus venir d'un scan (une personne absente ne scanne pas) : bouton manuel **« Marquer
  absent »** sur la page de scan, plus une tâche automatique à 22h qui marque absent quiconque avait cours
  ce jour-là sans aucun pointage.
- Un élève **chef de classe** peut maintenant réellement ouvrir `/attendance/scan` (le rôle était déjà
  autorisé côté serveur mais bloqué par erreur côté écran).

**Où voir les résultats (tableaux)** :
- `/attendance/scan` → **« Pointages récents »** : le flux immédiat de chaque scan (badge Présent/Retard +
  minutes à l'entrée ; durée réalisée/prévue à la sortie ; badge Absent pour un marquage manuel).
- `/attendance` → onglet **« Suivi & corrections »** (par classe + date) : table élève avec colonnes
  **Statut**, **Heure d'entrée**, **Retard** — c'est ici qu'on voit qui est venu, à quelle heure, et avec
  combien de retard, pour toute une classe un jour donné.
- `/attendance` → onglet **« Présences enseignants »** → **« Appel »** : table enseignant avec les mêmes
  colonnes **Statut / Heure d'entrée / Retard**, pointage réel du jour (scan ou saisie manuelle) — plus
  seulement les clics faits dans la session du navigateur.
- `/attendance` → onglet **« Présences enseignants »** → **« Récap mensuel »** : détail jour par jour sur
  tout un mois pour un enseignant choisi (déjà existant).

**Test** :
1. Sur `/attendance/scan`, sélectionner **Entrée**, scanner un élève avant l'heure de son cours → feed
   affiche **Présent**. Rescanner un autre élève après l'heure de début → feed affiche **Retard (+N min)**.
2. Aller sur `/attendance` → classe et date du jour → **Suivi & corrections** : vérifier que les deux
   élèves scannés apparaissent avec leur **heure d'entrée** exacte et le bon **retard**.
3. Sélectionner **Sortie**, rescanner le même élève → feed affiche l'heure de sortie + la comparaison
   durée réalisée/prévue.
4. Scanner une **sortie** sans avoir scanné d'entrée pour la séance en cours → message d'erreur clair
   (pas d'erreur technique brute).
5. Utiliser le bouton **« Marquer absent »** (panneau de pointage manuel) sur un élève qui n'a pas scanné →
   vérifier qu'il apparaît **Absent** dans « Suivi & corrections ».
6. Se connecter avec le compte élève désigné **chef de classe** (§6) → vérifier qu'il peut ouvrir
   `/attendance/scan` et scanner pour sa classe (rejeté pour une autre classe).
7. Répéter côté enseignant : scanner son entrée sur `/attendance/scan`, puis vérifier sur `/attendance` →
   **Présences enseignants** → **Appel**, à la date du jour, que la ligne de cet enseignant affiche le bon
   statut, l'heure d'entrée et le retard éventuel.

*Résultat attendu* : le scan aboutit toujours (plus d'erreur `tenant_id`), le sens Entrée/Sortie répond
systématiquement même après avoir changé de sens sur la même carte, et les tableaux de suivi (élèves et
enseignants) reflètent l'heure d'entrée et le retard réels, pas seulement un badge de statut brut.

---

## 14. Correction manuelle « Présent » avec heure d'entrée + affichage du retard en heures

**Contexte** : deux défauts remontés sur les tableaux de suivi mis en place au §13.
- Corriger manuellement un élève/enseignant en **« Présent »** (sans passer par le scan) ne demandait pas
  l'heure d'entrée — impossible donc de renseigner un retard pour une correction a posteriori (appel
  papier, panne de caméra, oubli de scan). Corrigé : le système demande maintenant l'heure d'entrée et
  calcule automatiquement le retard vs l'emploi du temps, exactement comme le fait le scan.
- Le retard s'affichait toujours en minutes brutes, y compris au-delà de 60 (ex. « 399 min »). Corrigé :
  conversion automatique en heures + minutes au-delà de 60 (ex. « 6 h 39 »), partout où un retard ou une
  durée est affiché (suivi élève, appel enseignant, flux de scan, cahier de textes).

**Où** :
- `/attendance` → classe + date → **Suivi & corrections** : colonne **Actions**, menu de statut.
- `/attendance` → **Présences enseignants** → **Appel** : boutons Présent/Absent par enseignant.

**Test — élève** :
1. Sur **Suivi & corrections**, choisir **« Présent »** dans le menu de statut d'un élève → un champ
   **heure** apparaît à la place du menu (au lieu d'appliquer directement).
2. Saisir une heure **après** le début du cours de cette classe ce jour-là, cliquer **OK**.
3. Vérifier que la ligne affiche la bonne **heure d'entrée** et un **retard** correctement calculé et
   affiché en heures/minutes si ≥ 60 min (ex. « 1 h 15 » et non « 75 min »).
4. Recommencer en saisissant une heure **avant** le début du cours → retard affiché « — » (aucun retard).
5. Choisir **« Absent »** dans le même menu → toujours appliqué immédiatement, sans demande d'heure
   (comportement inchangé).

**Test — enseignant** :
1. Sur **Présences enseignants → Appel**, cliquer **« Présent »** pour un enseignant → un champ heure +
   bouton **OK** apparaissent à la place des deux boutons Présent/Absent.
2. Saisir une heure, valider → la ligne affiche statut, heure d'entrée et retard (formaté en heures si
   pertinent).
3. Cliquer **« Absent »** → toujours immédiat, sans demande d'heure.

*Résultat attendu* : impossible de marquer quelqu'un présent sans préciser son heure d'arrivée ; le retard
qui en découle est calculé par le système (jamais saisi à la main) et toujours lisible (jamais un nombre
de minutes à trois chiffres).

---

## 15. Badge de retrait (maternelle/primaire) + impression des badges corrigée + enseignant retiré du scan

**Contexte** : trois changements liés à l'impression des badges et à la sécurité du scan.
- Les badges (élève, et maintenant badge de retrait) s'imprimaient recto sur une page et verso sur une
  deuxième — anormal pour un usage réel en papier bristol + plastification manuelle. Corrigé : recto et
  verso sortent maintenant **côte à côte sur une seule page**, chacun à la taille réelle d'une carte
  d'identité standard (CR80, 85,6 × 54 mm), avec une ligne de découpe pointillée entre les deux.
- Nouveau **badge de retrait** : à la sortie d'un élève de maternelle/primaire (scan ou pointage manuel
  sur la page de scan), le système demande obligatoirement **qui vient récupérer l'enfant** — un parent/
  tuteur déjà lié à l'élève (liste déroulante) ou une personne saisie librement (« Autre personne » + nom
  + lien). Une fois confirmé, un badge imprimable apparaît (même format carte que ci-dessus).
- **L'enseignant ne peut plus scanner** (ni lui-même ni un élève) — retiré des rôles autorisés sur
  `/attendance/scan`, par mesure d'intégrité (un enseignant ne doit pas pouvoir décider lui-même de son
  heure d'arrivée). Le personnel (admin/direction/secrétariat) et l'élève chef de classe restent
  autorisés ; un rôle **Surveillant** dédié est prévu pour plus tard.

**Où** :
- `/academics` (référentiel académique, onglet **Niveaux**) : chaque niveau a maintenant un champ
  **Cycle** (Maternelle/Primaire/Collège/Lycée/Supérieur/non renseigné).
- `/attendance/scan` : sortie d'un élève de maternelle/primaire.
- `/student-cards` : impression du badge élève.

**Test — cycle du niveau (prérequis)** :
1. Sur `/academics` → Niveaux, éditer (ou créer) un niveau de maternelle ou de primaire (ex. « Petite
   Section », « CP1 ») et lui assigner le cycle **Maternelle** ou **Primaire**. Enregistrer.
2. Vérifier qu'une classe rattachée à ce niveau est bien concernée par le test suivant.

**Test — badge de retrait** :
1. Scanner (ou pointer manuellement) la **sortie** d'un élève dont la classe est de niveau
   maternelle/primaire → une fenêtre **« Badge de retrait »** s'ouvre automatiquement, impossible à
   ignorer sans répondre.
2. Choisir un parent/tuteur dans la liste déroulante (déjà lié à cet élève), renseigner le lien
   (optionnel), confirmer → le badge de retrait s'affiche avec le nom, le lien, la date et l'heure ;
   imprimer pour vérifier une seule page, recto/verso côte à côte.
3. Recommencer avec **« Autre personne »** → saisir un nom (obligatoire) + lien → même résultat.
4. Scanner la sortie d'un élève de classe **secondaire** (collège/lycée) ou sans cycle renseigné →
   **aucune** fenêtre de retrait ne s'ouvre, comportement inchangé (§13).

**Test — impression sur une seule page** :
1. Depuis `/student-cards`, ouvrir l'aperçu d'un badge élève existant et cliquer **« Imprimer / Télécharger
   en PDF »** → vérifier dans l'aperçu d'impression du navigateur qu'**une seule page** est générée, avec
   recto et verso **côte à côte** (pas l'un après l'autre).
2. Comparer visuellement chaque face à une carte bancaire réelle (proportions ~1,6:1) — imprimer sur le
   papier bristol réellement utilisé par l'école pour confirmer que la découpe puis la plastification
   donnent un résultat correct aux bonnes dimensions.
3. Refaire le même test pour un badge de retrait fraîchement généré (étape précédente).

**Test — enseignant retiré du scan** :
1. Se connecter avec un compte **Enseignant** et essayer d'ouvrir `/attendance/scan` → accès refusé
   (« Accès non autorisé »), alors qu'avant ce changement l'enseignant pouvait scanner.
2. Vérifier que le pointage des enseignants continue de fonctionner normalement quand c'est le personnel
   (admin/direction/secrétariat) qui scanne leur badge à leur place.

*Résultat attendu* : impression des badges toujours sur une seule page au format carte réel ; le badge de
retrait est systématiquement demandé (et imprimable) à la sortie d'un enfant de maternelle/primaire, jamais
pour les autres niveaux ; un compte enseignant n'a plus du tout accès à la page de scan.

---

## 16. Blog d'entreprise (public) + back-office + rôle Rédacteur restreint

**Ce qui change** :
- Nouveau **blog public** CathClau Edu, dans le style « Blog Details Right Sidebar » : liste
  d'articles avec recherche, catégories et tags dans une colonne latérale, page détail avec image de
  couverture, contenu riche, tags, navigation article précédent/suivant, et un vrai formulaire de
  commentaire (nom, e-mail, site web optionnel, message).
- Les commentaires visiteurs sont **réellement enregistrés et modérés** : un commentaire soumis reste
  invisible publiquement tant qu'un rédacteur/admin ne l'a pas approuvé depuis le back-office.
- Nouveau rôle plateforme **Rédacteur**, strictement limité au blog (créer/modifier/publier des
  articles, gérer les catégories, modérer les commentaires) — **aucun accès** aux écoles, abonnements
  ou données clients, contrairement à SUPER_ADMIN.
- Éditeur de contenu **riche** (gras/italique/titres/liens/images/listes) pour rédiger les articles,
  pas un simple champ texte.

**Où** :
- Public : `/blog` (liste), `/blog/{slug}` (détail).
- Back-office : `/platform/blog/posts` (articles), `/platform/blog/categories`,
  `/platform/blog/comments` (modération) — visible dans le menu **Plateforme** pour SUPER_ADMIN et
  Rédacteur.
- Création du compte Rédacteur : `/platform/users` → bouton **« Nouveau rédacteur »**.

**Test — création d'un compte Rédacteur et vérification de ses limites d'accès** :
1. En SUPER_ADMIN, sur `/platform/users`, cliquer **« Nouveau rédacteur »**, renseigner prénom/nom/
   e-mail/mot de passe, créer.
2. Se déconnecter, se reconnecter avec ce nouveau compte → vérifier qu'il atterrit directement sur
   `/platform/blog/posts` (pas sur un tableau de bord d'école).
3. Vérifier que le menu latéral ne montre **que** l'entrée Blog dans la section Plateforme — aucun
   lien Écoles/Abonnements/Plans/Utilisateurs/Tableau de bord plateforme.
4. Essayer de naviguer directement vers une URL non autorisée (ex. `/platform/tenants` ou `/dashboard`)
   → redirection automatique vers `/platform/blog/posts`, pas d'accès aux données de l'école.

**Test — rédaction et publication d'un article** :
1. Depuis `/platform/blog/posts` (SUPER_ADMIN ou Rédacteur), cliquer **« Nouvel article »**.
2. Saisir un titre → vérifier que le slug (`/blog/...`) se génère automatiquement ; le modifier
   manuellement si besoin.
3. Ajouter un extrait, une image de couverture, choisir une catégorie (en créer une si la liste est
   vide via **« Catégories »**), ajouter des tags séparés par des virgules.
4. Rédiger le contenu avec l'éditeur riche (gras, titre, lien, image insérée dans le texte).
5. Cliquer **« Enregistrer le brouillon »** → l'article apparaît dans la liste avec le statut
   **Brouillon**, mais n'est **pas visible** sur `/blog` public.
6. Rouvrir l'article, cliquer **« Publier »** → le statut passe à **Publié** ; vérifier qu'il apparaît
   maintenant sur `/blog` et à son URL `/blog/{slug}`.

**Test — commentaire visiteur et modération** :
1. Depuis la page publique de l'article publié, remplir le formulaire de commentaire (nom, e-mail,
   message) et envoyer → message de confirmation « sera visible après validation », mais le
   commentaire n'apparaît **pas** dans la liste des commentaires affichés sous l'article.
2. Depuis `/platform/blog/comments` (filtre par défaut « En attente »), retrouver ce commentaire →
   cliquer **Approuver**.
3. Recharger la page publique de l'article → le commentaire est maintenant visible.
4. Recommencer avec un second commentaire et cliquer **Rejeter** à la place → il ne doit jamais
   apparaître publiquement, même après actualisation.

**Test — accès direct à un brouillon (sécurité)** :
1. Créer un article et le laisser en **Brouillon** (ne pas publier), noter son slug.
2. Essayer d'accéder à `/blog/{slug}` sans être connecté → page « Article introuvable », le brouillon
   n'est jamais exposé publiquement.

*Résultat attendu* : le blog public ne montre jamais un brouillon ni un commentaire non approuvé ; un
compte Rédacteur peut entièrement gérer le blog mais n'a la vue sur strictement aucune autre donnée
plateforme ou école.

---

## Vérifications techniques (déjà faites côté code, rappel)

- Backend : `./mvnw -o test` (inclut `ModularityTests`) — passe sans erreur après chaque phase.
- Frontend : `npx tsc --noEmit` et `npx eslint` sur tous les fichiers modifiés — aucune erreur.
- Aucune de ces vérifications automatiques ne remplace les tests manuels ci-dessus : elles garantissent
  que le code compile et respecte les règles de style, pas que le comportement métier est correct à l'écran.

---

## 17. Correctif — erreur "duplicate key" en enregistrant les permissions d'un rôle

**Ce qui changeait** : en cochant des permissions supplémentaires sur un rôle qui en avait déjà (sans
décocher les anciennes) puis en enregistrant, le serveur renvoyait une erreur 500 (`role_permissions_role_id_permission_id_key`
déjà existante) et rien n'était sauvegardé. Même bug sur l'attribution de permissions directes à un
utilisateur.

**Où** : `/users/roles` (gestion des rôles et permissions).

**Test** :
1. Ouvrir un rôle existant ayant déjà au moins une permission cochée.
2. Cocher une ou plusieurs permissions supplémentaires **sans décocher** celles déjà cochées.
3. Enregistrer → doit réussir sans erreur, et toutes les permissions (anciennes + nouvelles) doivent
   rester cochées après rechargement de la page.
4. Décocher une permission qui était cochée puis enregistrer → elle doit disparaître, les autres restent.

*Résultat attendu* : plus aucune erreur 500 lors de la sauvegarde des permissions, quel que soit le
mélange d'ajouts/retraits de permissions déjà accordées.

---

## 18. Site public — reproduction fidèle du style Vankine home-5 (couleurs, police, images, footer)

**Ce qui change** : le style du site public (`/`) reproduit maintenant fidèlement la maquette de
référence (template Vankine, style "home-5") plutôt qu'une version seulement inspirée :
- **Police** : DM Sans sur tout le site public (`/`, `/blog`, `/a-propos`, `/tarifs`…) — l'espace
  applicatif authentifié garde sa police actuelle (Outfit), inchangée.
- **En-tête** : bandeau clair (coordonnées) au-dessus d'une barre de navigation bleue pleine largeur,
  liens en pilules, bouton "Essai gratuit" blanc en pilule — au lieu de l'ancien en-tête blanc uni.
- **Pied de page** : fond bleu marine (au lieu de noir), avec une barre inférieure encore plus sombre —
  reproduit la structure logo/newsletter/réseaux sociaux + colonnes de liens du template.
- **Photo du hero** : voile de lisibilité allégé pour que la photo (déjà les vraies photos de
  l'établissement) reste nettement visible, au lieu d'un lavis bleu opaque.

**Où** : page d'accueil `/`, et toutes les pages publiques qui héritent de l'en-tête/pied de page
(`/blog`, `/a-propos`, `/tarifs`…).

**Test** :
1. Ouvrir `/` en navigation privée (pour éviter le cache) → vérifier la barre de navigation bleue
   pleine largeur avec les liens en pilules et le bandeau clair au-dessus.
2. Vérifier que le texte du site (titres et paragraphes) utilise une police arrondie sans-serif
   distincte de celle de l'espace applicatif connecté (Outfit) — comparer avec `/dashboard` après
   connexion.
3. Faire défiler jusqu'au pied de page → fond bleu marine avec une bande inférieure plus sombre pour
   le copyright.
4. Vérifier que la photo de fond du hero reste bien visible (pas assombrie à l'excès).

*Résultat attendu* : le site public partage désormais la même identité visuelle (couleurs, police,
structure d'en-tête/pied de page) que la maquette de référence, tout en gardant la palette de marque
CathClau Edu (bleu proche de celui du template).

---

## 19. Correctif — QR code illisible une fois le badge imprimé

**Ce qui changeait** : le QR code affiché sur les badges/cartes (élèves, enseignants, personnel, badge
de retrait) était généré **sans zone blanche de sécurité** (« quiet zone ») autour du motif — une
exigence de la norme QR. Ça pouvait passer inaperçu à l'écran mais rendait le code difficile ou
impossible à scanner une fois imprimé en petit format sur la carte.

**Où** : tous les badges (`/student-cards`, badges enseignant/personnel, badge de retrait généré à la
sortie d'un enfant de maternelle/primaire) — un seul composant partagé (`IdBadge`), donc un seul
correctif couvre tous les cas.

**Test** :
1. Ouvrir l'aperçu d'un badge élève (ou tout autre badge) sur `/student-cards`, imprimer/télécharger
   en PDF.
2. Scanner le QR code imprimé avec un téléphone ou la page `/attendance/scan` → doit être reconnu du
   premier coup, sans avoir à zoomer ou recadrer excessivement.

*Résultat attendu* : le QR imprimé se scanne de manière fiable, y compris à la taille réduite d'une
carte réelle.

---

## 20. Type d'établissement "Maternelle" (PS/MS/GS) + restriction des classes au type de l'école

**Ce qui change** :
- Le type d'établissement d'une école propose maintenant **Maternelle** en plus de
  Primaire/Collège/Lycée/Supérieur/Mixte.
- Le catalogue de niveaux (bouton **« Catalogue »** du référentiel Niveaux) crée directement les 3
  vraies sections de la maternelle — **Petite Section, Moyenne Section, Grande Section** — au lieu
  d'un niveau générique unique « Maternelle », chacune avec le cycle Maternelle déjà renseigné.
- **Nouvelle restriction** : une fois le(s) type(s) d'établissement renseigné(s) pour une école (sauf
  « Mixte », qui n'impose aucune limite), il devient impossible de créer une classe sur un niveau dont
  le cycle ne correspond à aucun des types déclarés (ex. une école déclarée uniquement « Primaire » ne
  peut pas créer de classe de niveau Collège/Lycée) — message d'erreur clair à la création.

**Où** :
- Création d'école (`/signup` ou `/platform/tenants` → nouvelle école) : sélection du/des type(s)
  d'établissement.
- `/academics` (référentiel académique, onglet Niveaux) → bouton **Catalogue**.
- Création de classe (`/classes` ou classe en lot).

**Test** :
1. Créer (ou modifier) une école en cochant uniquement **Maternelle** comme type d'établissement.
2. Sur le référentiel Niveaux de cette école, ouvrir **Catalogue** → cocher Petite/Moyenne/Grande
   Section → vérifier que les 3 niveaux sont créés avec le cycle Maternelle.
3. Essayer de créer une classe sur un niveau Collège/Lycée (via le catalogue, cocher aussi
   "Secondaire" pour en avoir un disponible) pour cette école → doit être **refusé** avec un message
   expliquant que l'école est déclarée pour un autre type.
4. Créer une classe sur un niveau Petite/Moyenne/Grande Section → doit fonctionner normalement.
5. Refaire le test avec une école déclarée **Mixte** → aucune restriction, tous les niveaux sont
   utilisables pour créer une classe.

*Résultat attendu* : une école configurée sur un type précis ne peut créer des classes que sur les
niveaux compatibles avec ce type ; une école "Mixte" ou sans type renseigné n'est jamais bloquée.

---

## 21. Encaissement — le reçu reflète le(s) mois précisément réglé(s) sur une tranche partielle

**Ce qui changeait** : à l'encaissement, quand une tranche couvre plusieurs mois (ex. « mai, juin »)
et que le parent décoche certains mois pour ne payer qu'une partie (ex. seulement « mai »), le montant
était bien recalculé, mais le **libellé du reçu** affichait toujours la tranche complète
(« Tranche 2 (mai, juin) ») au lieu d'indiquer que seul « mai » avait été réglé.

**Où** : `/guichet` (encaissement par élève, et encaissement groupé fratrie) — case à cocher par mois
sous une tranche multi-mois.

**Test** :
1. Ouvrir l'encaissement d'un élève dont la scolarité a une tranche couvrant plusieurs mois.
2. Cocher la tranche, puis **décocher un des mois** dans le détail affiché sous la tranche → le
   montant se réduit proportionnellement.
3. Encaisser → ouvrir le reçu PDF généré → le libellé doit préciser le(s) mois réellement payé(s)
   (ex. « Tranche 2 (mai, juin) — mai réglé(s) »), pas juste le nom générique de la tranche.
4. Refaire le test depuis l'encaissement groupé fratrie (plusieurs enfants à la fois) → même
   comportement pour chaque enfant.

*Résultat attendu* : le reçu reflète toujours précisément ce qui a été payé, y compris pour un
paiement partiel mois par mois sur une tranche.

---

## 22. Assistant de création de classes en lot — ne plus proposer une salle déjà occupée

**Ce qui changeait** : le formulaire de création de classe **au cas par cas** exclut déjà correctement
les salles occupées par une autre classe. Mais l'**assistant en lot** (« Assistant (en lot) », qui crée
plusieurs classes d'un coup) ne le faisait pas : le bouton « Attribuer automatiquement » et les menus
déroulants « Salle » de chaque ligne proposaient toutes les salles, y compris celles déjà affectées à
une classe existante.

**Où** : `/classes` → **Assistant (en lot)**, étape « Salles ».

**Test** :
1. S'assurer qu'au moins une salle est déjà affectée à une classe existante de l'année en cours.
2. Ouvrir l'Assistant (en lot), choisir un cycle/des grades/sections pour générer plusieurs nouvelles
   classes.
3. Cliquer **« Attribuer automatiquement »** → la salle déjà occupée ne doit jamais être proposée.
4. Ouvrir manuellement le menu déroulant « Salle » d'une des nouvelles classes → la salle déjà occupée
   ne doit pas apparaître dans la liste.
5. Vérifier que modifier ensuite une classe **existante** (formulaire classe par classe, pas
   l'assistant) montre toujours **sa propre** salle actuelle dans son menu déroulant, même si elle est
   « prise » — l'exception ne s'applique qu'à l'édition de cette classe précise.

*Résultat attendu* : une salle déjà affectée à une classe n'est jamais proposée pour une autre classe,
ni par l'attribution automatique ni manuellement dans l'assistant en lot ; seule l'édition de la classe
qui détient déjà cette salle continue de l'afficher.

---

## 23. Emploi du temps — « Créer un horaire fixe » : salle préremplie + matières enchaînées

**Ce qui change** :
- La **salle se préremplit automatiquement** avec la salle attitrée de la classe dès l'ouverture du
  formulaire (toujours modifiable) — avant, il fallait la ressaisir alors qu'elle était déjà connue.
- **Nouveau mode « Plusieurs matières enchaînées »**, pensé pour une monitrice de garderie/primaire qui
  enseigne plusieurs matières à la suite dans la même classe : on choisit l'enseignant une seule fois,
  puis on ajoute une ligne par matière avec sa propre durée et sa propre pause ; l'horaire s'enchaîne
  automatiquement à partir de l'heure de début (aucune heure imposée à l'avance, ex. pas de 8h-10h figé).
- Les deux modes existants (créneau unique, blocs répétés même matière) restent inchangés et toujours
  aussi flexibles sur les horaires.
- La **grille hebdomadaire** (vue tableau, une ligne par créneau de 2h) n'est plus limitée à la plage
  08h-16h : si un créneau réel dépasse cette plage par défaut (ex. garderie ouvrant dès 7h, ou activités
  après 16h), la grille étend automatiquement ses lignes pour ne jamais masquer une séance existante.
  L'heure réelle de chaque séance reste, comme avant, entièrement libre dans sa cellule.

**Où** : `/timetable` → sélectionner une classe → **« Créer un horaire fixe »**, et la grille
hebdomadaire elle-même.

**Test — salle préremplie** :
1. S'assurer que la classe sélectionnée a une salle attitrée (référentiel Classes).
2. Ouvrir « Créer un horaire fixe » → vérifier que le champ Salle affiche déjà cette salle, avec la
   mention « préremplie automatiquement ».
3. La changer manuellement → elle ne doit plus se réinitialiser toute seule.

**Test — matières enchaînées (monitrice)** :
1. Ouvrir « Créer un horaire fixe », choisir **« Plusieurs matières enchaînées »**.
2. Choisir l'enseignant (monitrice), les jours, l'heure de début.
3. Ajouter 2-3 lignes de matières, chacune avec sa propre durée et sa pause après (ex. Éveil 30 min +
   5 min de pause, Motricité 45 min + 10 min de pause, Comptines 20 min).
4. Vérifier l'aperçu de la séquence généré (matière → heure de début-fin, dans l'ordre) avant de
   valider.
5. Créer → vérifier sur la grille de l'emploi du temps que les créneaux apparaissent bien à la suite,
   aux bonnes heures, avec le même enseignant partout et sans heure fixe imposée par le système.

*Résultat attendu* : plus besoin de ressaisir la salle ; une monitrice enseignant plusieurs matières à
la suite peut créer tout son horaire de la journée en une seule fois, avec des durées entièrement
libres pour chaque matière.

**Test — grille étendue au-delà de 08h-16h** :
1. Créer un créneau qui commence avant 08h00 (ex. 07h00) ou se termine après 16h00 (ex. 17h00) pour une
   classe, via « Créer un horaire fixe » ou directement dans une cellule de la grille.
2. Recharger la grille hebdomadaire de cette classe → une ligne supplémentaire doit apparaître pour
   couvrir cette plage (au lieu de faire disparaître silencieusement le créneau).

*Résultat attendu* : aucun créneau réel n'est jamais invisible dans la grille, quelle que soit l'heure
de la journée.

---

## 24. Frais de cantine — support des tranches (comme la scolarité et le transport)

**Ce qui change** : le frais de type **Cantine** peut désormais être configuré en tranches, exactement
comme la Mensualité et le Transport — avant, seul un montant unique (sans échéancier) était possible.
Une cantine déjà configurée en montant unique continue de fonctionner sans changement (ajout à la
demande à l'encaissement), aucune donnée existante n'est affectée.

**Où** : `/invoices/fees` (Frais scolaires), création/modification d'un frais Cantine ; `/guichet`
(encaissement) pour la génération et le paiement des tranches de cantine.

**Test** :
1. Créer un nouveau frais de type **Cantine** → vérifier que le générateur de tranches apparaît (mois
   de départ, ajout de tranches avec mois couverts et montant), comme pour la Mensualité.
2. Enregistrer, puis ouvrir l'encaissement d'un élève de la classe concernée → un bandeau propose de
   **« Générer les tranches (cantine) »**, indépendamment de la scolarité (qui a son propre bandeau si
   elle aussi n'est pas encore générée).
3. Générer → les tranches de cantine apparaissent dans la liste à régler, avec le détail des mois
   couverts (case à cocher par mois comme pour la scolarité).
4. Encaisser une tranche de cantine → reçu généré normalement.
5. Vérifier qu'une cantine **existante** configurée en montant unique (sans tranches) continue de
   s'ajouter normalement via « Ajouter un frais annexe » à l'encaissement, sans bandeau de génération.

*Résultat attendu* : la cantine se comporte comme la scolarité pour les écoles qui veulent l'échelonner
en tranches, sans rien casser pour celles qui la facturent en un seul montant.

---

## 25. Correctif — la remise « paiement annuel complet » restait acquise après décochage

**Ce qui changeait** : à l'encaissement, cocher « Toute la scolarité » appliquait une remise (ex. 5%)
en la **sauvegardant directement sur les factures**. Mais décocher ensuite un mois ne l'annulait
jamais : la remise restait acquise sur les factures malgré la sélection incomplète, sans jamais
revenir au montant initial. Le calcul de la remise lui-même (basée sur la somme des 9 mensualités
restantes, quand aucune n'est encore payée) était déjà correct — c'est bien l'absence de retrait qui
posait problème.

**Où** : `/guichet` (encaissement d'un élève), case « Toute la scolarité ».

**Test** :
1. Ouvrir l'encaissement d'un élève dont aucune mensualité n'a encore été payée, avec une règle de
   remise paiement anticipé active.
2. Cocher **« Toute la scolarité »** → la remise s'applique, visible dans le récapitulatif.
3. **Décocher un seul mois** parmi ceux cochés → la remise doit immédiatement disparaître et chaque
   facture revenir à son montant initial (plus de mention de remise).
4. Recocher tous les mois → la remise doit se réappliquer normalement.
5. Recharger la page en cours de route (après avoir décoché) → vérifier que la remise n'est plus
   présente sur les factures (elle a bien été annulée côté serveur, pas seulement masquée à l'écran).

*Résultat attendu* : la remise « paiement annuel complet » n'est jamais acquise en dehors du moment où
la sélection couvre réellement la totalité des tranches restantes.

---

## 26. Fiche enseignant — niveaux enseignés (dropdown/checklist)

**Ce qui change** : on peut désormais spécifier dans quel(s) niveau(x) (cycle) un enseignant est
habilité à enseigner (ex. Primaire, Collège), via une liste à cocher listant les niveaux existants —
comme cela existait déjà pour les matières enseignées. Accessible dès l'étape « Affectations » du
formulaire d'inscription d'un enseignant, et modifiable ensuite depuis sa fiche.

En passant, un bug latent identique à celui du §17 (RBAC) a été corrigé sur l'affectation des
**matières** enseignées : cocher une matière supplémentaire sans décocher les précédentes pouvait
provoquer la même erreur « duplicate key » (même cause : delete-then-insert sur une contrainte unique).

**Où** : `/teachers` → inscrire un enseignant → étape « Affectations » ; et `/teachers/{id}` → carte
« Niveaux enseignés ».

**Test** :
1. Inscrire un nouvel enseignant, avancer jusqu'à l'étape « Affectations » → cocher un ou plusieurs
   niveaux dans « Niveaux enseignés », enregistrer.
2. Ouvrir la fiche de cet enseignant (`/teachers/{id}`) → la carte « Niveaux enseignés » doit refléter
   la sélection faite à l'inscription.
3. Cocher un niveau supplémentaire **sans décocher** les précédents, enregistrer → doit réussir sans
   erreur (régression du bug « duplicate key »), tous les niveaux (anciens + nouveau) restent cochés.
4. Faire le même test sur « Matières enseignées » (cocher en plus des matières déjà cochées) → doit
   aussi réussir sans erreur.

*Résultat attendu* : les niveaux enseignés se gèrent aussi simplement que les matières enseignées, sans
jamais provoquer d'erreur lors de l'ajout à une sélection existante.

---

## 27. NimbaSMS + téléphone parent obligatoire + diffusion segmentée aux parents/enseignants

**Ce qui change** : le SMS réel est désormais possible via **NimbaSMS** (nouveau fournisseur actif par
défaut pour le canal SMS, tant qu'aucune clé Orange n'est explicitement activée). Le téléphone d'un
parent est désormais **obligatoire** à la création comme à la modification. Une nouvelle **diffusion
segmentée** permet d'envoyer un message à *tous les parents*, aux parents d'*une classe*, aux parents
des *garçons* ou des *filles* d'une classe, à *tous les enseignants*, ou au(x) parent(s) d'*un élève
précis* — sans sélectionner les destinataires un par un : le serveur résout lui-même les numéros/e-mails.
Le cas « notifier le parent d'un élève en retard/absent » était déjà automatique (`StudentAbsentEvent`)
et enverra désormais un vrai SMS dès que NimbaSMS est configuré avec de vraies clés.

**Où** :
- Téléphone obligatoire : `/students/{id}` → onglet **Parents** → ajouter/modifier un parent.
- Diffusion segmentée : menu **Communication** (`/communications`) → bouton **« Diffusion segmentée »**.
- Configuration NimbaSMS (backend, pas d'UI) : variables d'environnement `NIMBA_SMS_ENABLED`,
  `NIMBA_SMS_SERVICE_ID`, `NIMBA_SMS_SECRET_TOKEN`, `NIMBA_SMS_SENDER_NAME`.

**Test — téléphone obligatoire** :
1. Sur la fiche d'un élève, onglet **Parents** → **Ajouter un parent** → laisser le téléphone vide,
   enregistrer → doit être bloqué avec le message « Requis ».
2. Renseigner un téléphone valide → enregistrement réussi.
3. Modifier un parent existant, vider le champ téléphone → doit être bloqué de la même façon.

**Test — diffusion segmentée** (compte `SCHOOL_ADMIN`, `DIRECTOR`, `SECRETARY` ou `TEACHER`) :
1. Aller sur `/communications` → **Diffusion segmentée**.
2. Choisir l'audience **« Tous les parents »**, canal SMS, écrire un message, **Diffuser** → un
   message apparaît dans l'historique avec le type de destinataire « Parents » ; ouvrir **Détail**
   pour voir un destinataire par numéro de téléphone de parent existant dans l'école.
3. Choisir **« Parents d'une classe »** → sélectionner une classe → **Diffuser** → seuls les parents
   des élèves de cette classe apparaissent en destinataires (un parent avec plusieurs enfants dans la
   même classe ne doit apparaître qu'une seule fois).
4. Choisir **« Parents des garçons d'une classe »** puis **« … des filles »** sur la même classe →
   vérifier que les destinataires changent en fonction du sexe des élèves.
5. Choisir **« Tous les enseignants »** → **Diffuser** → les destinataires sont les numéros/e-mails des
   enseignants de l'école (pas des parents).
6. Choisir **« Parent(s) d'un élève précis »** sans sélectionner d'élève → **Diffuser** doit être
   bloqué côté formulaire (« Sélectionnez un élève »).
7. Choisir une audience dont personne n'a de numéro/e-mail renseigné (ex. école de test sans données)
   → le serveur doit répondre par un message clair (« Aucun destinataire joignable »), pas une erreur
   technique.

*Résultat attendu* : la diffusion segmentée résout toujours de vrais destinataires (jamais une liste
vide silencieuse), le téléphone parent est désormais une donnée fiable pour toute notification SMS, et
les notifications automatiques de retard/absence utilisent le même fournisseur SMS réel une fois
NimbaSMS configuré.

---

## 28. Self-service profil (infos, mot de passe, photo) pour tous les rôles

**Ce qui change** : chaque compte (parent, élève, enseignant, comptable, secrétaire, directeur, admin
d'école, super admin, rédacteur) peut désormais, une fois connecté, mettre à jour ses propres
coordonnées, changer son mot de passe (déjà possible avant) et ajouter/remplacer sa photo de profil —
le tout depuis une seule page. Avant ce correctif, seul le parent avait un backend prêt pour ça, et même
lui n'avait aucun bouton dans l'interface pour s'en servir.

**Où** : menu utilisateur (avatar en haut à droite) → **Mon compte** (`/account`), accessible à tous les
rôles connectés.

**Test** :
1. Se connecter avec un compte **parent** → `/account` → carte « Modifier mes informations » : le
   téléphone, l'adresse et la profession doivent apparaître pré-remplis avec les vraies valeurs (pas
   vides) → modifier le téléphone → **Enregistrer** → recharger la page → la modification est conservée.
2. Sur la même page, téléverser une photo (bouton **Téléverser** ou **Prendre une photo**) → l'avatar de
   la carte « Profil » ET l'avatar dans le header (haut à droite) doivent afficher la nouvelle photo
   immédiatement, sans rechargement.
3. Se connecter avec un compte **élève** → `/account` → seuls téléphone et adresse sont proposés (pas
   de profession) → modifier + photo → même vérification qu'à l'étape 2.
4. Se connecter avec un compte **enseignant** → `/account` → seul le téléphone est proposé (ni adresse
   ni profession) → modifier + photo → même vérification.
5. Se connecter avec un compte **comptable**, **secrétaire**, **directeur**, **admin d'école** ou
   **super admin** → `/account` → seul le téléphone est proposé → modifier + photo → même vérification
   (ces rôles n'ont pas de fiche métier dédiée, la photo/téléphone sont stockés directement sur le
   compte).
6. Sur n'importe quel rôle : changer le mot de passe (section déjà existante) → vérifier que ça
   fonctionne toujours normalement (non-régression).
7. Vérifier qu'aucun rôle ne peut modifier son propre nom ni son e-mail depuis cette page (champs
   affichés en lecture seule) — ce sont des données que seul un administrateur peut changer.

*Résultat attendu* : chaque rôle peut se mettre à jour lui-même sans dépendre d'un administrateur, la
photo se reflète partout dans l'app immédiatement après l'upload, et rien n'écrase les données
administratives (matricule, spécialité, état civil…) qui restent réservées à l'administration.

---

## 29. Déplacement ponctuel d'un créneau d'emploi du temps (par semaine) + niveaux du primaire détaillés

**Ce qui change** : un créneau récurrent de l'emploi du temps (ex. « Éveil sensoriel » le lundi 8h–8h30)
peut désormais être déplacé vers un autre jour/horaire **pour une seule semaine précise**, sans toucher
au reste de l'année scolaire — utile pour la garderie, le préscolaire (Petite/Moyenne/Grande Section) et
le primaire, où les emplois du temps très détaillés sont souvent réorganisés ponctuellement (sortie,
absence, imprévu). Il est aussi possible d'annuler un créneau pour une semaine précise (le cours n'a pas
lieu cette semaine-là) sans le supprimer du modèle récurrent. Par ailleurs, le catalogue de niveaux
propose maintenant les 6 classes réelles du primaire (CI, CP, CE1, CE2, CM1, CM2) à la place d'un niveau
générique unique « Primaire », ainsi qu'un niveau « Garderie » avant la Petite Section.

**Où** : `/timetable`, vue **Liste** (le sélecteur de semaine, déjà présent, prend maintenant effet) ;
catalogue de niveaux depuis `/referentials` (ou l'écran d'ajout de niveaux) → bouton d'ajout depuis le
catalogue prédéfini.

**Test — déplacement/annulation ponctuels** (compte `SCHOOL_ADMIN` ou `DIRECTOR`) :
1. Aller sur `/timetable`, mode **Par classe**, vue **Liste** → créer (ou repérer) un créneau récurrent,
   ex. lundi 8h–8h30. Il doit apparaître identiquement quelle que soit la semaine sélectionnée dans le
   sélecteur de semaine.
2. Sur ce créneau, cliquer l'icône de déplacement (horloge) → choisir un autre jour/horaire (ex. mardi
   9h) → **Déplacer**. Vérifier que le créneau disparaît du lundi et apparaît au mardi 9h avec le badge
   « Déplacé depuis Lundi 08:00 » **uniquement pour la semaine sélectionnée**.
3. Changer la semaine (sélecteur) vers la semaine suivante → le créneau doit être revenu à sa position
   habituelle (lundi 8h, sans badge) — la modification ne concerne que la semaine choisie à l'étape 2.
4. Revenir à la semaine modifiée → rouvrir le déplacement → bouton **Annuler ce déplacement** → le
   créneau revient à sa position d'origine pour cette semaine.
5. Créer un autre créneau qui occuperait déjà mardi 9h pour la même classe/salle/enseignant → tenter de
   déplacer le créneau du test vers ce même horaire → une erreur de conflit doit apparaître (pas de
   déplacement silencieux vers un horaire déjà occupé).
6. Passer en vue **Grille** → vérifier que les créneaux y sont toujours affichés à leur position
   récurrente d'origine (la Grille édite uniquement le modèle récurrent, jamais une exception
   ponctuelle) et que **Modifier/Supprimer** dans cette vue continuent de fonctionner normalement
   (non-régression).
7. Imprimer/télécharger depuis la vue Liste d'une semaine avec déplacement → le PDF/impression reflète
   bien la disposition de cette semaine-là (créneau à sa position déplacée).

**Test — niveaux du primaire** (compte `SCHOOL_ADMIN` ou `DIRECTOR`) :
1. Sur l'écran d'ajout de niveaux depuis le catalogue prédéfini, vérifier la présence de **Garderie**,
   **Petite Section**, **Moyenne Section**, **Grande Section**, puis des 6 niveaux **CI, CP, CE1, CE2,
   CM1, CM2** (et non plus un seul niveau générique « Primaire »).
2. Cocher CI à CM2 → **Ajouter la sélection** → les 6 niveaux doivent être créés et utilisables ensuite
   pour créer des classes (ex. « CM2 A »).

*Résultat attendu* : le modèle récurrent de l'emploi du temps n'est jamais modifié par un déplacement
ponctuel (seule la semaine choisie change), les conflits restent détectés même pour un déplacement
ponctuel, et les écoles avec primaire/garderie disposent des vrais intitulés de classes dès la création
des niveaux.

---

## 31. Frais d'usage de la plateforme (montant SUPER_ADMIN ajouté aux mensualités + règlement Djomy)

**Ce qui change** : le SUPER_ADMIN peut désormais définir un montant de frais d'usage de la
plateforme. Ce montant est automatiquement ajouté à chaque facture de mensualité (frais type
« Mensualité » uniquement — jamais transport/cantine/inscription/examen) générée par une école, visible
sur la facture PDF comme une ligne « dont frais de service plateforme ». À chaque paiement encaissé sur
une telle facture, la part correspondante est mise de côté dans un registre propre à l'école. Chaque
école voit dans son tableau de bord finance le solde qu'elle doit à la plateforme, avec un bouton
**Verser** qui redirige vers le portail Djomy pour régler ce solde.

**Où** : `/platform/frais` (SUPER_ADMIN, définir le montant) ; `/reports` section « Financier »
(SCHOOL_ADMIN/DIRECTOR/ACCOUNTANT, carte « Frais plateforme » avec le bouton Verser).

**Test — configuration SUPER_ADMIN** :
1. Se connecter en SUPER_ADMIN → `/platform/frais` → définir un montant (ex. 2000) → **Enregistrer** →
   recharger la page → le montant affiché est bien 2000.
2. Depuis une école, générer une nouvelle facture de mensualité (`POST /billing/generate-monthly` ou
   via l'écran de facturation existant) → la facture doit inclure le montant total = mensualité de
   base + 2000, et le PDF de la facture doit afficher une ligne « dont frais de service plateforme :
   2000 GNF ».
3. Générer une facture de **transport** ou de **cantine** pour le même élève → vérifier qu'aucun frais
   plateforme n'est ajouté (montant = frais de base uniquement).
4. Changer le montant SUPER_ADMIN à 3000 → vérifier que les factures de mensualité **déjà émises**
   avant ce changement gardent leur ancien montant (pas de recalcul rétroactif) ; seules les nouvelles
   factures générées après reflètent 3000.

**Test — encaissement et solde école** :
1. Sur la facture de mensualité générée à l'étape 2 ci-dessus, enregistrer un paiement partiel (ex.
   60% du total) → aller sur `/reports` (compte SCHOOL_ADMIN/DIRECTOR/ACCOUNTANT) → la carte « Frais
   plateforme » doit afficher un solde dû égal à 60% du montant de frais plateforme de cette facture
   (proportionnel au paiement reçu, pas la totalité de suite).
2. Compléter le paiement de la facture (payer le solde restant) → recharger `/reports` → le solde dû
   à la plateforme doit maintenant refléter 100% du frais plateforme de cette facture.
3. Cliquer **Verser** → saisir un numéro de test → confirmer → vérifier la redirection vers le portail
   Djomy (ou la page de test Djomy en environnement de développement).
4. Après un règlement réussi (confirmé côté Djomy), recharger `/reports` → le solde dû doit être
   revenu à zéro (ou réduit du montant réglé) — l'historique du règlement doit apparaître si l'écran
   d'historique est consulté (`GET /platform-fee/settlements`).
5. Tenter de cliquer **Verser** quand le solde est à zéro → le bouton doit être désactivé (rien à
   verser).

*Résultat attendu* : le frais plateforme n'affecte jamais les frais autres que la mensualité, un
changement de montant ne modifie jamais les factures déjà émises, le solde dû par une école suit
fidèlement les encaissements réels (jamais la totalité facturée avant paiement), et le règlement se
fait exclusivement via le parcours Djomy existant, sans jamais transiter par un autre canal.

---

## 32. Fondation DPE — préfectures/communes, périmètre cross-tenant et journal d'audit

**Ce qui change** : SUPER_ADMIN peut désormais créer des préfectures/communes (DPE/DCE), y rattacher
des écoles et y affecter des comptes `DPE_ADMIN` — la brique de sécurité qui permettra, dans un second
temps, de construire le tableau de bord de supervision DPE. Toute écriture sensible déjà identifiée
(connexion, changement de rôle/permission, création/suspension/réactivation/archivage d'une école,
paiement réussi/remboursé) est désormais journalisée dans un registre d'audit consultable, jamais
modifiable. **Ce lot est backend uniquement — aucune UI n'existe encore pour ces endpoints.**

**Test — gestion des préfectures (SUPER_ADMIN, via un client HTTP type Postman/curl)** :
1. `POST /api/v1/platform/prefectures` avec `{"nom":"Coyah","type":"DPE","region":"Kindia","pays":"Guinée"}`
   → 201, la préfecture est créée avec `statut: ACTIVE`.
2. `PUT /api/v1/platform/prefectures/{id}/ecoles/{tenantId}` sur une école existante → 200, l'école
   apparaît ensuite dans `GET /api/v1/platform/prefectures/{id}/ecoles`.
3. Se connecter avec le compte SCHOOL_ADMIN de cette école → vérifier que **rien ne change** dans son
   expérience normale (isolation tenant classique inchangée, aucune donnée d'une autre école visible).
4. `DELETE /api/v1/platform/prefectures/ecoles/{tenantId}` → l'école disparaît de la liste de la
   préfecture.
5. `PATCH /api/v1/platform/prefectures/{id}/archiver` puis `/reactiver` → le statut bascule
   correctement dans les deux sens.
6. Créer un utilisateur avec le rôle `DPE_ADMIN` (via les endpoints rôles/utilisateurs existants) puis
   `POST /api/v1/platform/prefectures/{id}/utilisateurs` avec son `userId` → l'affectation est créée
   (aucun endpoint de lecture directe dans ce lot ; à vérifier via une requête SQL ou un futur écran).

**Test — journal d'audit** :
1. Se connecter avec n'importe quel compte → `GET /api/v1/audit-logs` (SCHOOL_ADMIN/DIRECTOR) doit
   afficher une ligne `action: LOGIN` récente pour cette connexion.
2. Affecter un rôle à un utilisateur (`POST` sur l'endpoint de gestion des rôles existant) → une
   nouvelle ligne `action: ROLE_ASSIGNED` doit apparaître dans le même journal.
3. Enregistrer un paiement réussi → une ligne `action: PAYMENT_SUCCEEDED` doit apparaître, avec le
   détail de la facture et du montant, mais **jamais** de données bancaires/personnelles sensibles.
4. En SUPER_ADMIN, créer un nouveau tenant puis le suspendre → `GET /api/v1/platform/audit-logs`
   (jamais `/api/v1/audit-logs`, qui reste vide pour ces deux actions) doit afficher `TENANT_CREATED`
   puis `TENANT_SUSPENDED`, **sans jamais exposer le mot de passe temporaire généré** dans le détail.
5. Vérifier qu'aucun endpoint ne permet de modifier ou supprimer une ligne du journal (aucune route
   `PUT`/`PATCH`/`DELETE` n'existe sur `/audit-logs`).

*Résultat attendu* : le périmètre DPE est une pure fondation additive qui ne modifie en rien
l'isolation tenant existante ni l'expérience des écoles ; le journal d'audit capture les actions
sensibles déjà identifiées sans jamais exposer de secret, et reste strictement en lecture seule via
l'API.

---

## 33. Tableau de bord DPE/DCE (première tranche : effectifs)

**Ce qui change** : un compte `DPE_ADMIN` affecté à une préfecture/commune dispose maintenant d'un
vrai tableau de bord de supervision : vue générale (nombre d'écoles, d'élèves, d'enseignants, de
classes de l'année en cours, ratio élèves/enseignant), liste des écoles du périmètre avec leurs
effectifs, et une fiche détaillée par école. **Cette tranche couvre uniquement les effectifs** — taux
de présence, moyennes et score de risque de décrochage ne sont pas encore disponibles (page de détail
école : message explicite « à venir »).

**Où** : menu « DPE / DCE » → `/dpe/dashboard` et `/dpe/schools` (visible uniquement pour le rôle
`DPE_ADMIN`).

**Test** :
1. Prérequis (SUPER_ADMIN, via un client HTTP) : créer une préfecture, y rattacher 2-3 écoles
   existantes (`PUT /api/v1/platform/prefectures/{id}/ecoles/{tenantId}`), créer un utilisateur avec
   le rôle `DPE_ADMIN` et l'affecter à cette préfecture (`POST
   /api/v1/platform/prefectures/{id}/utilisateurs`).
2. Se connecter avec ce compte `DPE_ADMIN` → le menu doit afficher une section « DPE / DCE » (et
   aucune des sections réservées aux écoles/SUPER_ADMIN).
3. `/dpe/dashboard` → les 4 cartes (écoles, élèves, enseignants, classes) doivent correspondre à la
   somme réelle des écoles rattachées ; le tableau « Écoles du périmètre » doit lister exactement ces
   écoles, jamais une école hors périmètre.
4. Cliquer sur une école → `/dpe/schools/{id}` → la fiche affiche les mêmes effectifs, avec le détail
   enseignants actifs/total.
5. Modifier l'URL pour pointer vers l'id d'une école **hors périmètre** (une école non rattachée à
   cette préfecture) → la page doit afficher un message d'erreur (403 backend), jamais les données de
   cette école.
6. Se connecter avec un compte d'une autre préfecture (ou sans aucune préfecture affectée) → le
   tableau de bord doit être vide (0 partout, tableau vide), jamais les écoles de la première
   préfecture.
7. Vérifier que rien ne change pour les comptes SCHOOL_ADMIN/DIRECTOR/ENSEIGNANT/PARENT des écoles
   concernées : ils ne voient jamais de mention DPE, leur isolation tenant reste identique à avant.

*Résultat attendu* : chaque compte DPE/DCE ne voit strictement que les écoles de son propre
périmètre, jamais plus ; les écoles elles-mêmes ne perçoivent aucun changement dans leur usage
quotidien de la plateforme.

---

## 34. Alertes DPE/DCE (première tranche : incohérences d'effectifs)

**Ce qui change** : un compte `DPE_ADMIN` dispose maintenant d'une page « Alertes » listant les
incohérences détectées automatiquement chez les écoles de son périmètre — école sans élève, sans
enseignant actif, sans classe pour l'année en cours, école suspendue/archivée, ou ratio
élèves/enseignant anormalement élevé (indicatif). Chaque alerte a un statut (Nouvelle → En cours →
Résolue) modifiable manuellement ; une alerte se résout aussi automatiquement dès que la condition
qui l'a déclenchée disparaît (réévaluée chaque nuit à 5h, ou à la demande via le bouton « Réévaluer
maintenant »).

**Où** : menu « DPE / DCE » → `/dpe/alerts`.

**Test** :
1. Sur une école de test rattachée au périmètre, vider volontairement ses classes de l'année (ou
   utiliser une école nouvellement créée sans élève/enseignant/classe) → se connecter en `DPE_ADMIN`
   → `/dpe/alerts` → cliquer **Réévaluer maintenant** → les alertes correspondantes
   (« Aucun élève », « Aucun enseignant actif », « Aucune classe ») doivent apparaître avec le statut
   « Nouvelle ».
2. Cliquer **Prendre en charge** sur une alerte → son statut passe à « En cours ».
3. Cliquer **Résoudre** → statut « Résolue » ; changer le filtre de statut vers « Résolues » → elle y
   apparaît toujours (rien n'est supprimé).
4. Régler le problème sur l'école (ajouter un élève, une classe…) → **Réévaluer maintenant** → les
   alertes correspondantes passent automatiquement à « Résolue » (sans action manuelle), même si
   elles étaient encore « Nouvelle »/« En cours ».
5. Recréer la même condition (ex. supprimer à nouveau l'élève ajouté) → **Réévaluer maintenant** →
   l'alerte déjà résolue repasse à « Nouvelle » (réouverture automatique), sans dupliquer la ligne.
6. Se connecter avec un compte d'une autre préfecture → `/dpe/alerts` ne doit jamais montrer les
   alertes de la première préfecture.
7. Vérifier qu'aucune transition de statut illégale n'est possible (ex. tenter de passer directement
   d'« En cours » à « Nouvelle » via l'API doit être rejeté).

*Résultat attendu* : les alertes reflètent toujours l'état réel des écoles (jamais périmées après
correction, jamais dupliquées en cas de récidive), restent strictement cantonnées au périmètre de
chaque compte DPE/DCE, et aucune alerte n'utilise de donnée non encore fiable (présence, résultats) —
uniquement des effectifs déjà vérifiés ailleurs dans l'application.

---

## 35. Référencement (SEO) du site public — cathclau-edu.com

**Ce qui change** : validation du domaine pour Google Search Console (fichier
`googleb53507edf28cf3e0.html`, déplacé dans `public/` pour être servi à la racine), et mise en place
complète du référencement du site public (frontend `lacaeducation-ui`) : `robots.txt` et
`sitemap.xml` générés dynamiquement, métadonnées (titre/description/Open Graph/Twitter Card) sur
toutes les pages publiques y compris Contact et Blog (converties en Server Components pour être
indexables — avant ce correctif elles étaient en `"use client"`, donc invisibles à un moteur de
recherche), données structurées JSON-LD (organisation sur tout le site public, FAQPage sur `/faqs`,
BlogPosting sur chaque article), manifeste PWA, et `noindex` explicite sur tout l'espace applicatif
authentifié.

**⚠️ Actions requises avant mise en production (pas automatiques)** :
1. Définir la variable d'environnement `NEXT_PUBLIC_SITE_URL=https://cathclau-edu.com` dans la
   configuration de déploiement du frontend (Vercel ou autre) — sans elle, le sitemap/robots/URLs
   canoniques utilisent `http://localhost:3000` par défaut.
2. S'assurer que le backend est joignable **au moment du build** du frontend (le sitemap récupère la
   liste des articles de blog publiés à la génération) — sinon le sitemap se limite aux pages
   statiques (dégradation silencieuse, pas d'échec de build).
3. Ne jamais supprimer `public/googleb53507edf28cf3e0.html` après validation dans Search Console
   (Google le redemande périodiquement pour confirmer la propriété).
4. Une fois déployé et validé, soumettre `https://cathclau-edu.com/sitemap.xml` dans Google Search
   Console (section Sitemaps).

**Test** :
1. `https://cathclau-edu.com/googleb53507edf28cf3e0.html` doit répondre 200 avec le contenu attendu
   → valider la propriété dans Search Console.
2. `https://cathclau-edu.com/robots.txt` → doit lister `Allow: /`, quelques `Disallow` sur les pages
   de connexion/transactionnelles, et un lien vers le sitemap.
3. `https://cathclau-edu.com/sitemap.xml` → doit lister toutes les pages publiques (accueil, tarifs,
   fonctionnalités, FAQ, aide, contact, inscription, blog + chaque article publié) — jamais une page
   de l'espace applicatif (dashboard, élèves, factures…).
4. Ouvrir le code source (Ctrl+U) de `/`, `/tarifs`, `/blog`, `/blog/{un-article}`, `/contact` → un
   `<title>` et une `<meta name="description">` uniques doivent être présents dans le HTML **avant
   toute exécution JavaScript** (view-source, pas les DevTools qui montrent le DOM après hydratation).
5. Se connecter à l'application (n'importe quel rôle) → ouvrir le code source d'une page du
   dashboard → une balise `<meta name="robots" content="noindex...">` doit être présente.
6. Coller l'URL d'un article de blog dans l'outil [Rich Results Test de
   Google](https://search.google.com/test/rich-results) → les données structurées `BlogPosting`
   doivent être détectées sans erreur ; faire de même sur `/faqs` pour `FAQPage`.
7. Sur `/blog/{un-article}`, tester le partage Facebook/X/WhatsApp et le formulaire de commentaire →
   doivent fonctionner normalement malgré le passage en Server Component (formulaire isolé dans un
   composant client dédié).

*Résultat attendu* : chaque page publique est indexable avec un contenu unique et réellement présent
dans le HTML initial (pas seulement après chargement JavaScript), l'espace applicatif authentifié
n'apparaît jamais dans les résultats de recherche, et aucune fonctionnalité interactive existante
(commentaires, partage, formulaire de contact) n'a régressé pendant la conversion en Server
Components.

---

## 36. Gestion des préfectures DPE/DCE côté SUPER_ADMIN (correctif : dashboard DPE inutilisable)

**Ce qui change** : le tableau de bord DPE (`/dpe/dashboard`) renvoyait une erreur générique pour un
SUPER_ADMIN car il n'existait tout simplement aucune interface pour créer une préfecture/commune, y
rattacher une école, ou prévisualiser son tableau de bord — seul le backend existait. Ajout de
`/platform/prefectures` (liste, création, archivage), d'une fiche préfecture (rattachement/détachement
d'écoles et de comptes DPE_ADMIN), et d'un sélecteur de préfecture affiché uniquement pour SUPER_ADMIN
sur les 4 pages `/dpe/*` (inutile et invisible pour un compte DPE_ADMIN, dont le périmètre reste
toujours automatique).

**Où** : menu « Plateforme » → « Préfectures DPE/DCE » (SUPER_ADMIN uniquement).

**Test** :
1. SUPER_ADMIN → `/platform/prefectures` → **Ajouter** → créer une préfecture de test (type DPE, nom
   « Coyah ») → elle apparaît dans la liste avec le statut Active.
2. Cliquer sur son nom → sur la fiche, **Rattacher une école** → choisir une école existante dans le
   sélecteur → elle apparaît dans « Écoles rattachées ».
3. **Affecter un compte** → choisir un utilisateur (idéalement déjà `DPE_ADMIN`, un avertissement
   s'affiche sinon) → il apparaît dans « Comptes DPE_ADMIN affectés ».
4. Cliquer **Aperçu tableau de bord** (ou retourner sur `/dpe/dashboard` en SUPER_ADMIN) → un bandeau
   « Aperçu SUPER_ADMIN » doit apparaître avec un sélecteur → choisir « Coyah » → les effectifs de
   l'école rattachée doivent s'afficher (ce qui échouait avant ce correctif).
5. Se connecter avec le compte affecté à l'étape 3 (s'il a bien le rôle `DPE_ADMIN`) → `/dpe/dashboard`
   → aucun bandeau de sélection ne doit apparaître (périmètre automatique), et les mêmes données
   doivent s'afficher directement.
6. Détacher l'école depuis la fiche préfecture → réévaluer/recharger le tableau de bord → l'école ne
   doit plus apparaître.

*Résultat attendu* : un SUPER_ADMIN peut désormais entièrement configurer et prévisualiser un
périmètre DPE/DCE sans jamais avoir besoin d'un accès direct à la base de données ; un compte
DPE_ADMIN réel continue de ne voir que son propre périmètre, sans aucun sélecteur superflu.

---

## 37. Correctif RBAC général : un rôle nouvellement affecté restait sans aucun menu visible

**Ce qui change** : après avoir affecté un rôle à un utilisateur (constaté sur `DPE_ADMIN`, mais le bug
touchait en réalité **n'importe quel rôle** en dehors de `SUPER_ADMIN`, `REDACTEUR`, `TEACHER`, `PARENT`,
`STUDENT`, `SCHOOL_ADMIN`, `DIRECTOR`, `ACCOUNTANT`, `SECRETARY`), l'utilisateur se connectait et restait
bloqué sur un écran de chargement, sans jamais voir la barre latérale ni aucun menu. La cause n'était ni
une permission manquante, ni un cache obsolète, ni un problème d'affectation en base : la fonction
`roleHome()` (qui calcule la page d'accueil après connexion, et la page de repli quand l'utilisateur
atterrit sur une page qui ne lui est pas destinée) renvoyait un chemin fixe (`/dashboard`) pour tout rôle
non explicitement prévu — or `/dashboard` est lui-même réservé aux rôles
`SCHOOL_ADMIN/DIRECTOR/ACCOUNTANT/SECRETARY`. L'utilisateur était donc redirigé en boucle vers une page
qui lui était aussitôt refusée, sans jamais atteindre un état stable. `roleHome()` calcule maintenant
dynamiquement la première page de navigation réellement accessible au rôle de l'utilisateur, à partir de
la même source de vérité que le reste du contrôle d'accès (`NAVIGATION`), ce qui rend le correctif valable
pour tout rôle actuel ou futur, pas seulement `DPE_ADMIN`.

**Où** : concerne la connexion de tout utilisateur, quel que soit son rôle (`src/lib/auth/role-access.ts`,
fonction `roleHome`).

**Test** :
1. Créer ou choisir un utilisateur avec un rôle qui n'a **pas** accès à `/dashboard` (ex. `DPE_ADMIN`,
   ou `RESPONSABLE_PEDAGOGIQUE`) → affecter le rôle depuis `/users` si besoin.
2. Se connecter avec ce compte → la page d'accueil doit s'afficher normalement (menu latéral visible,
   pas d'écran de chargement bloqué indéfiniment).
3. Répéter avec un compte `DPE_ADMIN` déjà affecté à une préfecture (voir §36) → doit atterrir sur
   `/dpe/dashboard` avec ses données, sans bandeau de sélection.
4. Vérifier la non-régression des rôles déjà gérés explicitement : `SUPER_ADMIN` → `/platform/dashboard`,
   `REDACTEUR` → `/platform/blog/posts`, `TEACHER` → `/dashboard/enseignant`, `PARENT` → `/children`,
   `STUDENT` → `/my-results`, et `SCHOOL_ADMIN`/`DIRECTOR`/`ACCOUNTANT`/`SECRETARY` → `/dashboard`.
5. Depuis un compte à rôle restreint, naviguer manuellement (barre d'adresse) vers une page hors de son
   périmètre → doit rediriger vers sa propre page d'accueil (pas de boucle, pas d'écran blanc).

*Résultat attendu* : tout rôle, actuel ou ajouté ultérieurement, atterrit systématiquement sur une page
qu'il a le droit de voir après connexion — plus aucun rôle ne peut se retrouver sans menu visible du
simple fait qu'il n'était pas explicitement listé dans `roleHome()`.

---

## 38. Correctif sitemap/robots.txt en production (Google Search Console ne pouvait pas les lire)

**Ce qui change** : le sitemap (`/sitemap.xml`) et `robots.txt` déployés en production pointaient tous
les deux vers `http://localhost:3000/...` au lieu de `https://cathclau-edu.com/...` — inutilisable par
Google Search Console (Google ne peut évidemment pas atteindre le `localhost` du serveur de build).
Constaté en interrogeant directement `https://cathclau-edu.com/sitemap.xml` en production. Cause : ces
fichiers sont générés une fois **au moment du build** Next.js à partir de la variable d'environnement
`NEXT_PUBLIC_SITE_URL` (voir `src/lib/site.ts` — `${SITE_URL}` avec un repli sur `localhost:3000` si la
variable est absente) ; le `Dockerfile` ne déclarait/transmettait que `NEXT_PUBLIC_API_BASE_URL` comme
build-arg, jamais `NEXT_PUBLIC_SITE_URL`, et `.github/workflows/deploy.yml` ne le passait pas non plus au
`docker build` — c'était l'action « avant mise en production » documentée (mais jamais faite) au §35.

**Correctif** : `.github/workflows/deploy.yml` calcule maintenant un output `site` (`https://cathclau-edu.com`
sur `main`, `https://dev.cathclau-edu.com` sur `develop`, même logique que l'output `api` existant) et le
transmet en build-arg `NEXT_PUBLIC_SITE_URL` ; le `Dockerfile` déclare cet `ARG`/`ENV` au même endroit que
`NEXT_PUBLIC_API_BASE_URL`. Vérifié localement : un `next build` + `next start` avec
`NEXT_PUBLIC_SITE_URL=https://cathclau-edu.com` produit bien un sitemap et un `robots.txt` avec les bonnes
URLs (plus aucune trace de `localhost`).

**Où** : aucune page UI — fichiers de déploiement uniquement (`Dockerfile`, `.github/workflows/deploy.yml`
du dépôt `lacaeducation-ui`). Effet visible sur `https://cathclau-edu.com/sitemap.xml` et
`https://cathclau-edu.com/robots.txt` après le prochain déploiement.

**Test** (après déploiement, pas testable avant puisque c'est le pipeline CI/CD qui est corrigé) :
1. Pousser sur `main` (ou déclencher manuellement le workflow `Deploy frontend`) → attendre la fin du job
   `build-push` puis `deploy`.
2. `https://cathclau-edu.com/sitemap.xml` → toutes les `<loc>` doivent commencer par
   `https://cathclau-edu.com`, plus aucune occurrence de `localhost`.
3. `https://cathclau-edu.com/robots.txt` → la ligne `Sitemap:` doit pointer vers
   `https://cathclau-edu.com/sitemap.xml`.
4. Dans Google Search Console → Sitemaps → soumettre (ou re-soumettre) `sitemap.xml` → le statut doit
   passer à « Réussite » (et non plus une erreur de récupération).
5. Vérifier le même comportement sur l'environnement `dev` (`https://dev.cathclau-edu.com/sitemap.xml`)
   après un déploiement sur la branche `develop`.

*Résultat attendu* : Google Search Console peut enfin récupérer un sitemap avec des URLs réellement
atteignables, condition préalable à toute indexation du site public.

---

## 39. Mode hors-ligne — Phase 1 : encaissement des frais au guichet (espèces, Orange Money, virement)

**Ce qui change** : jusqu'ici, une coupure réseau pendant l'encaissement d'un élève bloquait totalement
le secrétariat — impossible d'enregistrer le paiement, le parent repartait sans preuve. Les encaissements
« guichet » (espèces, Orange Money manuel, virement bancaire — pas les paiements en ligne Djomy/carte)
fonctionnent maintenant même sans connexion : l'agent voit son encaissement pris en compte immédiatement,
un reçu **provisoire** imprimable est délivré séance tenante, et le vrai paiement se synchronise
automatiquement dès le retour du réseau, sans jamais créer de doublon (clé d'idempotence côté serveur).

**Où** : `/students/{id}` → bouton d'encaissement → modale « Facture & encaissement » (`CollectFeeModal`)
et l'encaissement groupé multi-enfants (fratrie). Un bandeau d'état apparaît en haut de chaque page dès
qu'il y a une coupure réseau ou des paiements en attente/en échec de synchronisation.

**Limite assumée** : seuls les canaux « guichet » (espèces, Orange Money, virement) fonctionnent
hors-ligne — un paiement en ligne (Djomy) nécessite par nature une connexion et affichera une erreur
immédiate s'il est tenté hors-ligne, jamais mis en file. Cette phase suppose aussi que la fiche de
l'élève a déjà été ouverte **avant** la coupure (pas de préchargement pour ouvrir une fiche à froid
hors-ligne — limitation documentée, à traiter dans une phase ultérieure).

**Test** :
1. Ouvrir la fiche d'un élève ayant des factures impayées, en étant connecté — laisser la modale
   d'encaissement ouverte ou revenir dessus juste après.
2. Couper le réseau (DevTools → onglet Network → « Offline », ou désactiver le Wi-Fi) → le bandeau ambre
   « Hors-ligne » doit apparaître en haut de la page dans les secondes qui suivent.
3. Cocher une facture, choisir le mode « Espèces », cliquer **Encaisser** → notification « Encaissement
   enregistré hors-ligne » ; une section « Reçus provisoires (hors-ligne) » apparaît dans la modale avec
   une référence locale `OFFLINE-XXXXXXXX` → cliquer **Reçu provisoire** → une fenêtre s'ouvre avec un
   reçu marqué « PROVISOIRE » et lance l'impression.
4. Rétablir le réseau → dans les secondes qui suivent, le bandeau doit passer à « Synchronisation… » puis
   disparaître ; rouvrir la fiche de l'élève → la facture est passée à PAYÉE, un vrai reçu (référence
   officielle, PDF téléchargeable) est visible dans « Reçus de paiement ».
5. Vérifier dans l'historique des paiements de l'élève qu'**un seul** paiement a été créé (pas de doublon)
   malgré l'éventuel aller-retour hors-ligne/en-ligne.
6. Couper le réseau puis choisir le mode « En ligne (Djomy) » et tenter d'encaisser → une erreur doit
   s'afficher immédiatement (pas de mise en file, pas de faux « en attente »).
7. Répéter les étapes 2-5 sur l'encaissement groupé multi-enfants (fratrie).

*Résultat attendu* : un agent de secrétariat peut continuer à encaisser des espèces malgré une coupure
réseau, sans double encaissement possible au retour de la connexion, et avec une trace claire (bandeau +
reçu provisoire) de ce qui reste à synchroniser.

---

## 40. Mode hors-ligne — Phase 2 : inscription élève + consultation de l'emploi du temps

**Ce qui change** : deux extensions du mode hors-ligne (§39), en réutilisant les mêmes fondations
(détection réseau, file d'attente IndexedDB, bandeau global) :
- **Inscription élève** : créer un élève (et, si la classe est déjà connue, l'inscrire) fonctionne
  maintenant même sans connexion — l'élève apparaît en « attente de synchronisation » avec un matricule
  provisoire, et devient un élève normal (matricule officiel, inscription validée) une fois synchronisé,
  sans jamais créer de doublon (clé d'idempotence côté serveur, même mécanisme que les paiements).
- **Emploi du temps (consultation seule)** : le tableau de bord enseignant, la fiche d'un enseignant et
  la fiche d'un enfant côté parent affichent désormais le dernier emploi du temps connu même hors-ligne
  (mis en cache localement), avec un badge « Données en cache — hors-ligne » quand c'est le cas — plutôt
  que l'écran vide/en erreur d'avant.

**Limites assumées** : le lien élève-parent reste une étape à faire une fois reconnecté (déjà le cas
aujourd'hui même en ligne) ; le formulaire d'inscription hors-ligne ne propose que année + classe (pas
d'historique de réinscription ni d'abonnements transport/cantine, qui nécessitent un élève déjà réel) ;
l'emploi du temps affiché hors-ligne est celui de la dernière visite en ligne de cette page précise (pas
de préchargement pour une toute première consultation à froid hors-ligne).

**Où** : `/students` (encart « Élèves en attente de synchronisation »), `/dashboard/enseignant`,
`/teachers/{id}`, `/children` (portail parent). Le même bandeau global qu'en §39 agrège maintenant
paiements ET inscriptions en attente/en échec.

**Test** :
1. **Emploi du temps** : ouvrir `/dashboard/enseignant` en étant connecté (le cache se remplit
   silencieusement) → couper le réseau (DevTools → Network → Offline) → recharger la page → l'emploi du
   temps reste affiché avec le badge « Données en cache — hors-ligne » au lieu d'un écran vide. Répéter
   sur `/teachers/{id}` et sur `/children` (compte parent, avec un enfant déjà consulté au moins une
   fois en ligne).
2. **Inscription hors-ligne** : couper le réseau sur `/students` → **Nouvel élève** → remplir le
   formulaire → un encart « Élèves en attente de synchronisation » apparaît avec le nom saisi et
   « Matricule : en attente » ; le bandeau global compte 1 en attente.
3. Cliquer **Choisir la classe** sur cette entrée → le formulaire simplifié (année + classe) doit
   proposer les années/classes déjà connues (chargées en cache lors d'une visite précédente en ligne) →
   valider → l'entrée passe à « Classe choisie — en attente de connexion ».
4. Rétablir le réseau → synchronisation automatique (bandeau se vide) → recharger `/students` → l'élève
   apparaît normalement dans la liste avec son vrai matricule ; ouvrir sa fiche → son inscription est
   bien validée dans la classe choisie.
5. Vérifier qu'aucun élève ni inscription en double n'apparaît malgré un éventuel aller-retour réseau
   pendant la synchronisation.
6. Tester le cas quota élèves dépassé pendant que l'élève est en file (hors-ligne) : à la synchronisation,
   l'entrée doit passer en « échec » avec un message clair (pas de perte silencieuse de la saisie), avec
   les boutons Réessayer/Ignorer dans la modale de détail du bandeau.

*Résultat attendu* : une secrétaire peut inscrire des élèves pendant une coupure réseau (cas fréquent en
période de rentrée) et consulter les emplois du temps déjà visités sans être bloquée par une panne
réseau, avec la même garantie qu'en §39 de ne jamais créer de doublon au retour de la connexion.

---

## 41. Mode hors-ligne — Phase 3 : création d'enseignant + création/liaison de parent

**Ce qui change** : deux nouvelles extensions du mode hors-ligne (§39-40), même principe (réseau
d'abord, repli local, clé d'idempotence côté serveur) :
- **Enseignant** : créer un enseignant fonctionne maintenant même sans connexion via un formulaire
  simplifié (identité + informations professionnelles) — l'accès portail et l'étape « Affectations »
  (niveaux/classes/matières) restent à compléter depuis sa fiche une fois synchronisé.
- **Parent** : créer un nouveau parent (sans accès portail) ou lier un parent déjà existant à un élève
  fonctionne aussi hors-ligne — la déduplication du lien élève-parent (aucune contrainte unique côté
  serveur sur cette table) est vérifiée côté client avant de le recréer à la synchronisation.

**Limite assumée** : la création d'un compte de connexion (« créer un accès portail ») est bloquée
hors-ligne pour les deux (enseignant et parent), avec un message explicite — elle nécessite un second
appel réseau séparé (`createUserWithRole`) sans idempotence propre ; à faire une fois reconnecté.

**Où** : `/teachers` (encart « Enseignants en attente »), fiche d'un élève → section Parents (encart
« Parents en attente »). Le bandeau global (§39-40) agrège désormais 4 files : paiements, inscriptions
élèves, enseignants, parents.

**Test** :
1. Couper le réseau sur `/teachers` → **Nouvel enseignant** → le formulaire simplifié apparaît (pas
   d'étape Compte ni Affectations) → créer → entrée « en attente de synchronisation » ; rétablir le
   réseau → l'enseignant apparaît normalement avec son vrai matricule, aucun doublon.
2. Couper le réseau sur la fiche d'un élève → **Ajouter un parent** → mode « Nouveau parent », sans
   cocher « créer un accès » → créer → entrée « en attente » dans la section Parents ; rétablir le
   réseau → le parent apparaît lié normalement, aucun doublon de lien.
3. Répéter en mode « Parent existant » (rechercher un parent déjà en base pendant que le réseau était
   disponible, puis couper le réseau et le rattacher).
4. Cocher « créer un accès » puis couper le réseau (ou l'inverse) et tenter de créer un nouveau parent
   → message de blocage explicite avant tout appel réseau, aucune tentative silencieuse.
5. Vérifier que le bandeau global compte bien les 4 types (paiements, inscriptions, enseignants,
   parents) et que la modale de détail des échecs les distingue clairement.

*Résultat attendu* : un secrétariat peut continuer à enregistrer enseignants et parents pendant une
coupure réseau, sans jamais créer de doublon (élève, enseignant, parent, ou lien) au retour de la
connexion, avec la même transparence (bandeau + sections « en attente ») que les phases précédentes.

---

## 42. Responsivité mobile/tablette — formulaires de l'espace applicatif

**Ce qui change** : audit de la responsivité mobile/tablette (page d'accueil, cards, boutons, menus,
onglets). Vérification concrète (CSS vendor du template public, calculs de largeur réels) : la page
d'accueil publique, la navigation, le tableau de données, les modales et le tableau de bord étaient
**déjà** correctement responsive — aucune modification nécessaire là-dessus. Le vrai problème
identifié : presque tous les formulaires de création/édition de l'espace applicatif (élève, enseignant,
parent, classe, salle, matière, créneau, transport, comptabilité, paramètres…) utilisaient des grilles
à 2 ou 3 colonnes **fixes**, sans repli à 1 colonne sur petit écran — dans une modale sur téléphone,
chaque champ ne disposait plus que de ~100 à 165 px, illisible. Corrigé sur 111 grilles dans 41
fichiers (repli automatique en 1 colonne en dessous de 640px, disposition normale au-delà) ; une seule
grille volontairement laissée telle quelle (`parents-manager.tsx`, bascule « Parent existant / Nouveau
parent », qui est un contrôle à deux boutons côte à côte, pas une paire de champs).

**⚠️ Non vérifié visuellement** : aucun outil de capture d'écran/navigateur n'est disponible dans cet
environnement — la vérification s'est limitée au code (build, typage, lint, relecture des classes
générées). Une vérification visuelle réelle sur téléphone/tablette reste à faire par l'utilisateur.

**Où** : concerne quasiment tous les formulaires en modale de l'espace applicatif — élèves, enseignants,
parents, classes, salles, matières, emploi du temps, transport, comptabilité/budget/paie, paramètres,
utilisateurs, communications, cartes d'élève, biométrie.

**Test** : sur un téléphone (ou DevTools en mode responsive, ~375px de large), ouvrir successivement
quelques formulaires représentatifs — **Nouvel élève** (`/students`), **Nouvel enseignant**
(`/teachers`), **Ajouter un parent** (fiche élève), **Nouvelle classe** (`/academic-years` ou
équivalent), un formulaire de `/accounting` ou `/payroll` — chaque paire/triplet de champs (ex.
Prénom/Nom, Date début/Date fin) doit s'afficher **empilée verticalement** sur téléphone (pas de champs
écrasés côte à côte), et repasser en colonnes normalement dès qu'on élargit au-delà d'une tablette
(~640px). Vérifier aussi que la bascule « Parent existant / Nouveau parent » dans **Ajouter un parent**
reste bien affichée sur deux boutons côte à côte à toutes les tailles (elle n'a pas été modifiée).

*Résultat attendu* : tous les formulaires de saisie de l'application restent utilisables sur téléphone,
sans champs illisiblement étroits, sans régression sur desktop/tablette.

---

## 43. SMS Nimba — alerte au fondateur à chaque paiement reçu

**Ce qui change** : en plus du SMS déjà envoyé au parent à chaque paiement reçu, le(s) compte(s)
`SCHOOL_ADMIN` de l'école reçoivent désormais un SMS séparé les informant du paiement (élève, montant),
sur le numéro de téléphone enregistré sur leur compte. Décision actée : pas de numéro de fondateur dédié
pour l'instant — c'est le compte administrateur de l'école qui reçoit l'alerte.

**Où** : aucune interface — déclenché automatiquement en arrière-plan à chaque paiement réussi (guichet
ou en ligne), qu'il soit lié à une facture scolaire ou non.

**Prérequis pour un envoi RÉEL** (sinon le SMS est simplement journalisé, pas envoyé) : dans `.env`,
`NIMBA_SMS_ENABLED=true` + `NIMBA_SMS_SERVICE_ID` + `NIMBA_SMS_SECRET_TOKEN` + `NIMBA_SMS_SENDER_NAME`
tous renseignés (les 4 sont obligatoires — un seul vide et l'envoi reste simulé).

**Test** :
1. Vérifier qu'un compte `SCHOOL_ADMIN` de l'école a bien un numéro de téléphone renseigné dans sa
   fiche (`/users`).
2. Encaisser un paiement (guichet ou en ligne) pour un élève quelconque.
3. Vérifier que **deux** SMS partent : un au parent de l'élève (déjà existant), un au compte
   `SCHOOL_ADMIN` avec le nom de l'élève et le montant.
4. Si aucun compte `SCHOOL_ADMIN` n'a de téléphone renseigné pour ce tenant, l'alerte fondateur est
   silencieusement ignorée (pas d'erreur, juste un message de niveau debug dans les logs) — le paiement
   et le SMS parent continuent de fonctionner normalement.

*Résultat attendu* : le fondateur/administrateur de l'école est informé par SMS de chaque encaissement,
sans jamais bloquer le paiement lui-même si son numéro n'est pas disponible.

## 44. Créer et payer l'abonnement d'une école directement depuis sa fiche (SUPER_ADMIN)

**Contexte** : le modèle réel d'exploitation n'est pas la souscription en libre-service par l'école —
c'est le SUPER_ADMIN qui crée directement chaque école. Or aucun écran n'exposait la fonction d'API
existante `initiateSubscription` (création d'abonnement côté admin) : impossible de faire apparaître
une facture à payer pour une école tout juste créée. Corrigé en rendant les deux actions visibles
directement sur la fiche de l'école.

**Où** : `/platform/tenants/{id}` (fiche détail d'une école, SUPER_ADMIN).

**Ce qui change** :
1. Un bouton **« Basculer sur cette école (abonnement) »** en tête de fiche (avant « Modifier ») —
   bascule l'affichage sur cette école (mécanisme d'impersonation déjà existant mais jusque-là sans
   déclencheur visible) et redirige vers `/subscription/invoices`.
2. Dans la carte « Abonnement & quota », si l'école n'a **aucun abonnement**, un mini-formulaire
   apparaît directement (« Aucun abonnement — en créer un ») : choix du plan + périodicité (mensuel/
   annuel) + bouton « Créer l'abonnement ». À la création, la facture (`ISSUED`, payable
   immédiatement) est générée automatiquement et on est redirigé vers `/subscription/invoices` en
   mode « bascule sur cette école » pour payer directement.

**Test** :
1. Créer une nouvelle école (`/platform/tenants` → créer).
2. Ouvrir sa fiche : vérifier que la carte « Abonnement & quota » affiche le mini-formulaire de
   création (aucun abonnement existant).
3. Choisir un plan + une périodicité, cliquer « Créer l'abonnement » : vérifier la redirection vers
   `/subscription/invoices` avec une facture visible et payable (bouton « Payer »).
4. Depuis la fiche d'une école ayant déjà un abonnement, cliquer « Basculer sur cette école
   (abonnement) » : vérifier l'arrivée sur `/subscription/invoices` avec les factures de cette école
   (et non celles d'une autre école précédemment consultée).

*Résultat attendu* : le SUPER_ADMIN peut, sans quitter la fiche de l'école, créer son abonnement et
enchaîner immédiatement sur le paiement (Djomy) — plus besoin que l'école se souscrive elle-même.
