# SCHOOL SaaS — Module SCOLARITÉ : workflow métier réel (école privée guinéenne)

> Spécification fonctionnelle **fonctionnelle/UX** des menus Élèves, Inscriptions, Parents, Classes, Enseignants, Emploi du temps. Conçue à partir du **fonctionnement réel des écoles privées en Guinée** (Conakry et intérieur). Sert de cahier des charges au front (écrans, champs, étapes) ET complète les specs back-end (champs à compléter).
>
> **Repères système éducatif guinéen** (intégrés dans les écrans) :
> - **Préscolaire / Maternelle** : Petite, Moyenne, Grande Section (3–6 ans), entièrement privé.
> - **Primaire (6 ans)** : CI → CP → CE1 → CE2 → CM1 → CM2, sanctionné par le **CEP** (Certificat d'Études Primaires).
> - **Collège / 1er cycle secondaire (4 ans)** : 7e → 8e → 9e → 10e, sanctionné par le **BEPC**.
> - **Lycée / 2nd cycle (3 ans)** : 11e → 12e → Terminale (séries : Sciences Sociales SS, Sciences Mathématiques SM, Sciences Expérimentales SE), sanctionné par le **Baccalauréat**.
> - **Année scolaire** : ~octobre → juin/juillet, découpée en **3 trimestres**.
> - **APEAE** : Association des Parents d'Élèves et Amis de l'École (interlocuteur clé).
> - Paiements : **espèces au guichet** (très courant), **Orange Money / MTN MoMo / Wave**, parfois virement. La scolarité se paie souvent **par tranches** (inscription + mensualités/trimestres).

---

## PRINCIPE D'ERGONOMIE (transverse à tous les menus)

```
Objectif : qu'une secrétaire d'école puisse tout faire vite, même avec peu de formation informatique.
- Recherche universelle en haut (élève par nom/matricule/téléphone parent).
- Saisie assistée : valeurs par défaut intelligentes (année courante pré-sélectionnée, classe déduite du
  niveau, matricule auto-généré), listes déroulantes plutôt que saisie libre.
- Tout formulaire long = STEPPER (étapes claires) avec brouillon auto-sauvegardé.
- Actions principales toujours visibles (boutons gros, libellés explicites en français).
- Codes couleur : vert (à jour/validé), orange (en attente/incomplet), rouge (impayé/bloqué).
- Chaque liste : filtres + export (PDF/Excel) + impression.
- Mobile : la secrétaire/le surveillant peut faire l'appel ou encaisser depuis un téléphone.
- Toujours afficher l'état financier de l'élève là où c'est utile (un élève en règle vs en retard).
```

---

# 1) MENU « ÉLÈVES » — Dossier de l'enfant

### Workflow réel
En Guinée, le dossier de l'élève est un **dossier physique** que l'école reconstitue chaque année. La plateforme le **numérise**. À la première inscription on ouvre le dossier ; aux réinscriptions on le complète. Le dossier suit l'enfant toute sa scolarité (lien avec le **livret scolaire**).

### Sous-écrans & workflow

**1.a Liste des élèves**
```
- Tableau : photo, matricule, nom & prénoms, classe courante, sexe, âge, statut inscription (badge),
  statut financier (à jour / doit X GNF), parent principal + téléphone.
- Filtres : année scolaire, niveau, classe, sexe, statut inscription, statut financier (en règle/impayé),
  nouveaux vs anciens (redoublants/réinscrits).
- Recherche : nom, matricule, téléphone parent.
- Actions rapides par ligne : voir dossier, réinscrire, encaisser un paiement, imprimer la carte, contacter
  le parent (WhatsApp/SMS).
- Boutons : "Nouvel élève", "Import Excel" (rentrée : importer une liste), "Export", "Impression liste".
```

**1.b Création d'un élève (ouverture du dossier) — STEPPER**
```
Étape 1 — Identité de l'enfant
  - Nom, Prénom(s), Sexe, Date de naissance, Lieu de naissance, Nationalité (défaut Guinéenne),
    Numéro d'acte de naissance, Langue maternelle (optionnel), Groupe sanguin (optionnel),
    Photo (upload ou prise par webcam au guichet).
  - Matricule : auto-généré (format paramétrable, ex. ANNÉE-SEQ : 2025-0148), modifiable par l'admin.
Étape 2 — Coordonnées & domicile
  - Quartier / commune (liste localités), adresse, point de repère (très utile en Guinée),
    téléphone de l'enfant (si grand), email (optionnel).
Étape 3 — Parents / Tuteur (au moins un obligatoire)
  - Rechercher un parent existant (par téléphone) OU créer : Père, Mère, Tuteur.
    Pour chaque : nom, prénoms, lien (PÈRE/MÈRE/TUTEUR), profession, téléphone(s) (obligatoire),
    email, employeur (optionnel), est le contact principal ? (oui/non), autorisé à récupérer l'enfant ?
  - Personne à contacter en cas d'urgence (nom + téléphone) si différent.
Étape 4 — Scolarité antérieure
  - École précédente, dernière classe fréquentée, redoublant ? (oui/non),
    résultats antérieurs (optionnel), motif de transfert (optionnel).
Étape 5 — Santé (optionnel mais utile)
  - Allergies, maladies chroniques, traitement, médecin/contact, observations.
Étape 6 — Documents du dossier (upload, voir liste ci-dessous) + cases "fourni / manquant".
Étape 7 — Récapitulatif -> Enregistrer.
  -> Propose ENCHAÎNER sur l'inscription dans une classe (voir menu Inscriptions).

CONTRÔLE QUOTA : avant enregistrement, vérifier le quota d'élèves du plan (403 si dépassé -> message +
lien abonnement). Une école en TRIAL/SUSPENDED voit le comportement adapté.
```

**1.c Dossier de l'élève (fiche à onglets)**
```
- Onglet RÉSUMÉ : photo, identité, classe, parent principal, statut financier (encadré : total dû / payé /
  reste), raccourcis (réinscrire, encaisser, imprimer carte, livret, contacter parent).
- Onglet INSCRIPTIONS : historique par année (classe, statut, date), bouton réinscrire.
- Onglet PARENTS/TUTEURS : liste + liens + qui récupère l'enfant + contact principal.
- Onglet DOCUMENTS : pièces fournies/manquantes (checklist), upload, aperçu, date d'expiration éventuelle.
- Onglet SCOLARITÉ : notes & bulletins par trimestre (lien grading), livret scolaire (lien report-card).
- Onglet FINANCES : factures de frais, échéances, paiements, reçus (lien billing/payments) + solde.
- Onglet PRÉSENCES : assiduité, absences/retards, justificatifs.
- Onglet SANTÉ : infos médicales.
- Onglet CARTE : carte scolaire (générer/imprimer/QR).
- Onglet HISTORIQUE : journal des actions sur le dossier (audit).
```

**Documents du dossier élève (checklist réelle GN)**
```
Obligatoires : Extrait d'acte de naissance (ou jugement supplétif), Photos d'identité (2 à 4),
  Certificat/bulletin de l'année précédente OU certificat de radiation/transfert (si vient d'une autre école),
  Fiche d'inscription signée par le parent.
Selon le cas : Certificat médical / carnet de vaccination (surtout maternelle/primaire),
  Certificat de nationalité (parfois demandé), Copie pièce d'identité du parent/tuteur,
  Photocopie du dernier diplôme (CEP/BEPC) pour entrée en 7e / 11e, Bulletin trimestriels antérieurs.
Le système : checklist "fourni / manquant", relance auto des pièces manquantes (via communication),
  blocage configurable de certaines actions si pièces obligatoires manquantes (ex. édition du certificat).
```

---

# 2) MENU « INSCRIPTIONS / RÉINSCRIPTIONS »

### Workflow réel (le cœur de la rentrée)
En Guinée, à la rentrée : le parent vient, **paie les frais d'inscription** (souvent condition pour valider la place), choisit la classe/série, signe la fiche. Les **anciens élèves se réinscrivent** (priorité, parfois tarif réduit), les **nouveaux** déposent un dossier. La place n'est **définitive qu'après le 1er versement**. Beaucoup d'écoles gèrent une **liste d'attente** quand la classe est pleine.

### Workflow plateforme

**2.a Campagne de réinscription (anciens élèves)**
```
- À l'ouverture d'une nouvelle année : lancer la "campagne de réinscription".
- Liste des élèves de l'an dernier avec proposition de classe SUIVANTE automatique (CM2->6e n'existe pas en
  GN : CM2->7e ; 10e->11e avec choix de SÉRIE SS/SM/SE ; Terminale->sortie/Bac). Le système propose la
  classe via la progression des niveaux ; gère le REDOUBLEMENT (même niveau) selon la décision de passage
  du livret/conseil de classe.
- Réinscription en 1 clic (préremplie) -> génère la facture d'inscription -> à valider au paiement.
- Suivi : qui a réinscrit / qui n'a pas encore (relances WhatsApp/SMS en masse).
```

**2.b Nouvelle inscription**
```
- Depuis un dossier élève créé (menu Élèves) OU formulaire combiné "créer élève + inscrire".
- Choisir : année scolaire (courante), niveau, classe (affiche places restantes : effectif/capacité),
  série si lycée (SS/SM/SE), régime (externe / demi-pensionnaire / interne si applicable),
  transport scolaire ? cantine ? (options qui ajoutent des frais).
- Type : INSCRIPTION (nouveau) ou RÉINSCRIPTION (ancien). Statut initial : EN_ATTENTE.
- À l'enregistrement -> génère automatiquement la FACTURE DE FRAIS D'INSCRIPTION (+ options choisies).
- VALIDATION : l'inscription passe VALIDÉE soit manuellement (admin), soit automatiquement au 1er paiement
  réussi (configurable). Tant qu'EN_ATTENTE, la place est "réservée" (option : expire après X jours).
- Si classe pleine (capacité atteinte) : proposer LISTE D'ATTENTE (position) ou une autre classe du même
  niveau.
- Édite : reçu d'inscription + (option) certificat de scolarité.
- CARTE SCOLAIRE : dès l'inscription validée (et, selon la pratique de l'école, après le 1er paiement),
  la carte de l'élève est générée dans la foulée — c'est le réflexe en Guinée : inscrit + payé = carte
  délivrée. La carte reprend photo, matricule, classe, année et un QR. Impression immédiate au guichet,
  ou impression groupée par classe juste avant la rentrée.
```

**2.c Règles & garde-fous**
```
- Une seule inscription VALIDÉE par élève et par année.
- Refus si classe pleine (409) -> liste d'attente.
- Réinscription : contrôle de la décision de passage (ADMIS/REDOUBLE) issue de l'année précédente.
- Lien billing : InscriptionValidée -> facture ; au paiement -> place confirmée + reçu multicanal au parent.
- Transfert sortant : émettre un CERTIFICAT DE RADIATION / TRANSFERT (document officiel signé + cachet).
```

**2.d Écrans annexes**
```
- Tableau de bord rentrée : nb inscrits / objectif, par niveau/classe, taux de réinscription, places
  restantes, recettes d'inscription encaissées.
- Liste d'attente par classe (gérer les promotions quand une place se libère).
- Mouvements : arrivées (nouvelles inscriptions) / départs (radiations, transferts) sur l'année.
- Cartes scolaires : file des élèves inscrits validés dont la carte n'est pas encore éditée ->
  impression individuelle ou par classe (planche), juste avant la rentrée.
```

---

# 3) MENU « PARENTS / TUTEURS »

### Workflow réel
Le parent est le **payeur** et le **destinataire des communications** (bulletins, absences, convocations, relances). Un parent a souvent **plusieurs enfants** dans la même école. En Guinée, le **téléphone (WhatsApp)** est le canal n°1.

### Workflow plateforme
```
3.a Liste des parents : nom, téléphone(s), email, nb d'enfants, solde global dû (cumul des enfants),
    canal de communication préféré. Filtres : a des impayés, classe d'un enfant.
3.b Fiche parent :
    - Coordonnées complètes, profession/employeur, pièce d'identité.
    - ENFANTS rattachés (avec classe + statut financier de chacun) -> vue consolidée "famille".
    - SITUATION FINANCIÈRE FAMILLE : total dû / payé pour tous les enfants, échéances à venir.
    - Préférences de communication (WhatsApp/SMS/Email, langue) + opt-in/opt-out.
    - Accès PORTAIL PARENT (compte optionnel) : activer/désactiver, renvoyer l'invitation.
    - Historique des messages envoyés (reçus, relances, bulletins).
3.c Lien de parenté : lier/délier un parent à un élève (PÈRE/MÈRE/TUTEUR), définir le contact principal
    et qui est autorisé à récupérer l'enfant.
3.d Portail parent (côté parent) : voir ses enfants, bulletins, livret, absences, factures, PAYER en ligne,
    télécharger reçus, messages de l'école.
```

---

# 4) MENU « CLASSES »

### Workflow réel
La classe = un niveau + une année + (au lycée) une série, avec une **capacité** (souvent 30–60 en privé), un **professeur principal/titulaire**, une **salle**. On dédouble parfois (6e A, 6e B…).

### Workflow plateforme
```
4.a Liste des classes : nom (ex. "7e A"), niveau, série (lycée), prof principal, salle, effectif/capacité
    (jauge colorée), année. Filtres niveau/année.
4.b Création : code/nom, niveau (CI…Terminale), série si lycée, année scolaire, capacité max,
    professeur principal (enseignant), salle, langue d'enseignement (si pertinent).
    - Interdit si l'année est clôturée/archivée.
4.c Fiche classe (onglets) :
    - ÉLÈVES : liste des inscrits validés (photo, matricule, sexe, statut financier), ajouter/retirer,
      transférer un élève vers une autre classe, imprimer la liste/trombinoscope.
    - EFFECTIF : courant vs capacité, répartition filles/garçons.
    - MATIÈRES : matières affectées + enseignant + COEFFICIENT (modifiable par classe -> clé notation).
    - EMPLOI DU TEMPS : grille de la classe.
    - RÉSULTATS : moyennes de classe par trimestre, taux de réussite (lien grading).
    - FINANCES : taux de recouvrement des frais de la classe.
4.d Actions : passage de classe en masse (fin d'année -> promotion vers le niveau suivant selon décisions),
    fusion/scission de classes.
```

---

# 5) MENU « ENSEIGNANTS / PERSONNEL »

### Workflow réel
En Guinée, beaucoup d'enseignants du privé sont **vacataires/contractuels payés à l'heure ou au forfait** ; certains sont permanents. L'école suit leurs **matières**, **classes**, **emploi du temps**, **présence** (pour la paie), et garde leur **dossier administratif** (diplômes, autorisation d'enseigner — exigée par le SNEPUP pour le privé).

### Workflow plateforme
```
5.a Liste des enseignants : photo, matricule, nom, spécialité/matières, statut (permanent/vacataire),
    nb de classes, téléphone, statut (actif/inactif). Filtres spécialité/statut.
5.b Création (dossier enseignant) — STEPPER :
    Étape 1 Identité : nom, prénoms, sexe, date naissance, nationalité, photo, téléphone(s), email, adresse.
    Étape 2 Professionnel : matricule (auto), spécialité, diplôme le plus élevé, statut (PERMANENT|
      VACATAIRE|CONTRACTUEL), date d'embauche, type de rémunération (MENSUEL|HORAIRE|FORFAIT) + taux/montant,
      numéro autorisation d'enseigner (SNEPUP), n° CNSS (optionnel).
    Étape 3 Documents : CV, copies diplômes, autorisation d'enseigner, pièce d'identité, casier judiciaire
      (souvent demandé), certificat médical, contrat. Checklist fourni/manquant.
    Étape 4 Compte : créer un compte utilisateur (rôle ENSEIGNANT) pour l'espace prof ? (oui/non).
5.c Fiche enseignant (onglets) :
    - INFOS : identité + professionnel + statut.
    - AFFECTATIONS : classes + matières enseignées (ClasseMatiere) + coefficient.
    - EMPLOI DU TEMPS : planning consolidé (toutes ses classes), heures/semaine (utile pour la paie horaire).
    - PRÉSENCES : pointage, heures effectuées, absences (lien attendance, base de la paie vacataire).
    - DOCUMENTS : dossier administratif.
    - PAIE (si géré) : éléments de rémunération, états d'heures (peut alimenter expenses/accounting : salaires).
5.d Règles : matricule unique ; suppression interdite si lié à des notes -> désactivation ;
    l'enseignant ne saisit des notes que sur ses classes/matières.
```

---

# 6) MENU « EMPLOI DU TEMPS »

### Workflow réel
L'emploi du temps est construit par classe (créneaux jour × heure → matière + enseignant + salle). Contraintes fortes : **un enseignant ne peut pas être à deux endroits à la fois**, **une salle ne peut accueillir deux classes en même temps**, **une classe a un seul cours à un instant donné**. Beaucoup d'écoles GN font encore l'EDT sur papier/Excel → fort besoin de simplification.

### Workflow plateforme
```
6.a Vue calendrier hebdomadaire (grille) : axes Jours (Lun–Sam, le samedi matin est courant en GN) ×
    Créneaux horaires (paramétrables, ex. 08:00–10:00, pauses incluses). Filtre par CLASSE, ENSEIGNANT ou SALLE.
6.b Création d'un créneau : glisser-déposer ou formulaire (classe, matière, enseignant, salle, jour,
    heure début/fin). Le système propose les enseignants qui enseignent cette matière à cette classe.
6.c DÉTECTION DE CONFLITS (refus 409, message clair) :
    - même enseignant sur deux créneaux qui se chevauchent,
    - même salle occupée deux fois,
    - même classe avec deux cours simultanés.
    Afficher précisément le conflit (qui/quoi/quand) et proposer une alternative (autre salle/créneau).
6.d Contrôles : heure_debut < heure_fin ; dans les horaires d'ouverture ; volume horaire par matière
    (alerte si dépassement du quota hebdo prévu).
6.e Sorties : impression EDT par classe (affichage salle), par enseignant (son planning perso),
    par salle (taux d'occupation). Export PDF. Publication -> notification aux enseignants/élèves
    (communication). Modèle réutilisable d'une année sur l'autre (dupliquer l'EDT).
```

---

## RÉCAPITULATIF DES ENCHAÎNEMENTS (parcours type rentrée)

```
1. (Avant rentrée) Admin : créer/activer l'ANNÉE scolaire + TRIMESTRES, créer NIVEAUX, SALLES, CLASSES,
   MATIÈRES + coefficients, EMPLOI DU TEMPS, et le BARÈME de notation (grading).
2. (Rentrée) Réinscription des anciens : campagne -> propositions de classe -> facture d'inscription ->
   paiement (espèces/Mobile Money) -> place confirmée -> reçu multicanal au parent.
3. (Rentrée) Nouveaux : ouvrir dossier élève (+ documents) -> inscrire dans une classe -> facture ->
   paiement -> validation -> CARTE SCOLAIRE générée et imprimée dans la foulée (photo obligatoire).
4. (Année) Présences/appel, évaluations & notes, bulletins par trimestre, livret scolaire, communication
   parents, encaissement des mensualités/tranches, dépenses & comptabilité.
5. (Fin d'année) Conseils de classe -> décisions de passage -> clôture trimestres/année -> bulletins &
   livrets -> passage de classe en masse -> archivage -> nouvelle année.
```

> Ce document est le **cahier des charges fonctionnel** du module Scolarité. Il se combine avec les prompts
> back-end (students, academics, staff) et les prompts front (Partie 1) : ces derniers doivent implémenter
> CES workflows, CES champs et CES garde-fous, avec l'ergonomie décrite (stepper, recherche universelle,
> codes couleur, statut financier visible, conflits EDT, checklists de documents, relances multicanal).
```
NB: sur le front je doit pouvoir previsualiser les document dans l'application, les cartes aussi