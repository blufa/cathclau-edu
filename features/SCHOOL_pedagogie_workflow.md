# SCHOOL SaaS — Module PÉDAGOGIE : workflow métier réel (école guinéenne)

> Spécification fonctionnelle des menus **Évaluations & Notes**, **Bulletins**, **Présences**. Conçue à partir du fonctionnement réel des écoles guinéennes (primaire, collège, lycée) et des pratiques d'évaluation d'Afrique de l'Ouest. Sert de cahier des charges au front et complète les specs back-end (grading, attendance, report-card).
>
> **Repères réels intégrés :**
> - **Découpage** : primaire = **3 trimestres** avec **composition trimestrielle** ; secondaire = souvent **semestriel** (selon calendrier officiel guinéen), mais beaucoup d'écoles privées restent en trimestres. → le système doit gérer **les deux** (périodes paramétrables, voir academics).
> - **Composition** : chaque trimestre se termine par une **composition** (examen de fin de période) qui pèse lourd dans la moyenne. À côté : **devoirs/interrogations** (contrôle continu).
> - **Calcul** : moyenne pondérée — chaque note ramenée sur l'échelle (souvent /20), `moyenne_matière = Σ(note×coeff_éval)/Σ(coeff_éval)`, `moyenne_générale = Σ(moyenne_matière×coeff_matière)/Σ(coeff_matière)`. **Rang par classe**. Au primaire, on note aussi par **compétences/appréciations**.
> - **Distinctions** (paramétrables, pratique courante) : Tableau d'honneur (≥13/20), Encouragements, Félicitations (≥15/20), Prix d'excellence (≥17/20).
> - **Bulletin** : par matière → note + coeff + moyenne + **moyenne de la classe** + **note min/max** + appréciation du prof ; puis moyenne générale, rang, mention, décision, appréciation du conseil de classe + visa du directeur ; **signatures** (directeur + parent), **cachet** de l'école.
> - **Conseil de classe** : en fin de période, valide les moyennes, attribue distinctions et appréciation générale, et (fin d'année) la **décision de passage**.
> - **Présences** : **registre d'appel** quotidien (réflexe enseignant en GN), absences justifiées/non justifiées, retards ; relance des parents par WhatsApp/SMS.
> - **Barème paramétrable** par type d'établissement (primaire = compétences, collège/lycée = notes/20, supérieur = crédits) — déjà spécifié dans grading ; ici on décrit le **workflow d'usage**.

---

## PRINCIPE D'ERGONOMIE (transverse)

```
- L'ENSEIGNANT doit saisir vite, depuis un téléphone si besoin : grille de saisie type tableur, une ligne
  par élève, navigation clavier (Entrée = élève suivant), enregistrement automatique du brouillon.
- Toujours afficher le contexte : classe, matière, période, type d'évaluation, note_sur, coefficient.
- Codes couleur des notes : rouge < seuil de réussite, orange proche, vert au-dessus ; note manquante = gris.
- Verrouillage clair : une note validée a un cadenas ; une période clôturée bloque toute saisie (message).
- L'UI s'adapte au BARÈME de la classe : notes chiffrées (collège/lycée), compétences A/B/C/D (primaire),
  crédits/UE (supérieur).
- Le DIRECTEUR/ADMIN pilote : génération des bulletins en lot, conseil de classe, publication, impression.
- Le PARENT/ÉLÈVE consulte les bulletins publiés (lecture seule) et reçoit une notification à la publication.
```

---

# 1) MENU « ÉVALUATIONS & NOTES »

### Workflow réel
L'enseignant programme ses **évaluations** (interrogation, devoir, composition) sur la période en cours, pour ses classes/matières. Il **saisit les notes** après correction, puis **valide** (verrouille). En fin de trimestre, la **composition** est l'évaluation majeure. Tout est cadré par la période (pas de saisie hors période ouverte) et par les droits (un prof ne note que ses classes/matières).

### 1.a Mes évaluations (espace enseignant) — `/notes/evaluations`
```
- Liste des évaluations DE L'ENSEIGNANT (filtres : classe, matière, période, type, statut saisie).
  Colonnes : date, classe, matière, type (INTERRO|DEVOIR|COMPOSITION|EXAMEN), note_sur, coefficient,
  avancement saisie (ex. 28/30 notes saisies), statut (BROUILLON|SAISIE_EN_COURS|VALIDEE).
- Bouton "Nouvelle évaluation".
- Vue ADMIN/DIRECTEUR : toutes les évaluations de l'école (supervision, filtres élargis).
```

### 1.b Créer une évaluation — `/notes/evaluations/nouvelle`
```
Champs : classe* (limitée à celles de l'enseignant), matière* (limitée à ce qu'il enseigne dans la classe),
  période* (courante par défaut), type* (INTERROGATION|DEVOIR|COMPOSITION|EXAMEN), titre/objet,
  date*, note_sur* (défaut selon barème, ex. 20), coefficient* (poids de l'évaluation dans la matière).
Règles : interdit si période clôturée ou année non EN_COURS (désactiver + message). Le prof ne peut créer
  que sur ses ClasseMatiere (403 sinon).
```

### 1.c Saisie des notes — `/notes/evaluations/{id}/saisie`
```
GRILLE DE SAISIE (cœur du module, doit être ultra-rapide) :
- En-tête : classe, matière, période, type, note_sur, coefficient. Compteur "X/Y notes saisies".
- Tableau : une ligne par élève (photo miniature, matricule, nom). Colonne note (input numérique) +
  colonne "absent/dispensé" (N/D). Validation 0 ≤ note ≤ note_sur (cellule rouge + message si hors borne).
- Navigation : Entrée/Tab = élève suivant ; collage depuis Excel possible ; tri par nom/matricule.
- Brouillon auto-sauvegardé en continu. Bouton "Enregistrer".
- MODE BARÈME :
  * NOTE_CHIFFREE (collège/lycée) : saisie numérique sur note_sur.
  * COMPETENCE (primaire) : au lieu d'une note, sélection d'un niveau (A/B/C/D ou Acquis/En cours/Non
    acquis) depuis l'échelle de compétences du barème ; couleurs.
  * CREDITS (supérieur) : saisie par UE/ECTS.
- VALIDATION : bouton "Valider les notes" -> verrouille (cadenas). Correction ensuite = nécessite
  déverrouillage par un rôle autorisé (directeur). Publie NotesValidees (recalcul des moyennes).
```

### 1.d Relevé de notes de l'élève — (dans le dossier élève)
```
- GET notes par élève (filtres période, matière) : toutes les évaluations + notes + moyennes par matière
  recalculées. Vue parent/élève en lecture seule.
```

### 1.e Examens (compositions officielles / blancs) — `/notes/examens`
```
- Planifier un examen (classe, période, type BLANC|SEMESTRIEL|ANNUEL|NATIONAL, date), changer son statut
  (PLANIFIE→EN_COURS→TERMINE), générer la CONVOCATION PDF. Utile pour les compositions trimestrielles et
  les bacs/BEPC blancs.
```

---

# 2) MENU « BULLETINS »

### Workflow réel
À la fin de chaque période (après la composition et la **clôture de période**), l'école **génère les bulletins** de chaque classe. Le **conseil de classe** se réunit : il examine les moyennes, attribue les **distinctions** (tableau d'honneur, félicitations…) et une **appréciation générale**, puis le directeur **vise**. Les bulletins sont **publiés** (visibles parents/élèves) et **imprimés** (signature + cachet). En fin d'année, le conseil prononce la **décision de passage** (admis/redouble) qui alimente le livret.

### 2.a Génération des bulletins — `/bulletins/generer`
```
- Choisir classe + période -> "Générer les bulletins" (lot). Possible uniquement après CLÔTURE de la
  période (sinon message). Génération asynchrone avec barre de progression.
- À la clôture de période (academics), la génération peut se déclencher AUTOMATIQUEMENT (événement).
- Calcul effectué selon le BARÈME applicable à la classe : moyennes par matière (pondérées par coeff
  évaluation), moyenne générale (pondérée par coeff matière), rang par classe, statistiques de classe
  (moyenne classe, min, max, effectif), mention + décision (depuis le barème). Primaire : agrégation des
  compétences + appréciations.
```

### 2.b Liste & suivi des bulletins — `/bulletins`
```
- DataTable (filtres élève, classe, période, statut : BROUILLON|GENERE|PUBLIE). Colonnes : élève, classe,
  période, moyenne générale, rang, mention, statut.
- Actions : ouvrir, publier, télécharger PDF, imprimer en lot (toute la classe).
```

### 2.c Détail d'un bulletin — `/bulletins/{id}`
```
AFFICHAGE (adapté au barème) :
- En-tête : identité école (logo, nom, adresse via TenantSettings), élève (photo, matricule, classe),
  période, année.
- Tableau par matière : matière, coefficient, note/moyenne de l'élève, MOYENNE DE LA CLASSE, note MIN/MAX,
  appréciation du professeur, nom de l'enseignant.
- Synthèse : moyenne générale, rang (ex. "5e/42"), mention, décision, total des coefficients.
- Vie scolaire : absences/retards de la période (depuis attendance).
- Appréciation du CONSEIL DE CLASSE + visa du directeur. Distinction attribuée (tableau d'honneur /
  félicitations / encouragements / prix d'excellence).
- Espaces signatures (directeur + parent) + cachet.
PRIMAIRE : présentation par compétences/domaines (couleurs A/B/C/D) + appréciations, rang optionnel.
SUPÉRIEUR : UE validées, crédits acquis, moyenne pondérée par crédits.
- Bouton "Publier" (BROUILLON/GENERE → PUBLIE) : rend visible parent/élève + notifie (communication).
- Bouton "PDF" / "Imprimer la classe".
```

### 2.d Conseil de classe — `/bulletins/conseil-de-classe`
```
- Écran dédié par classe + période : tableau de TOUS les élèves avec leurs moyennes/rangs côte à côte
  (tri par rang), pour faciliter la délibération.
- Saisie en lot : appréciation générale par élève, attribution des distinctions (proposées automatiquement
  selon les seuils du barème : ≥13 tableau d'honneur, ≥15 félicitations, ≥17 excellence — modifiables),
  avertissements (travail/conduite).
- Statistiques de classe : moyenne, taux de réussite, répartition, meilleurs/plus en difficulté.
- (Fin d'année) Décision de passage par élève (ADMIS|REDOUBLE|ADMIS_CONDITION) -> alimente le livret
  (report-card) et la réinscription (proposition de classe suivante).
- Validation du conseil -> verrouille les appréciations, autorise la publication groupée.
```

### 2.e Tableaux & palmarès — `/bulletins/palmares`
```
- Tableau d'honneur / palmarès de la classe ou de l'école par période (les majors, distinctions).
- Bulletins de synthèse annuelle (moyenne annuelle = moyenne des périodes, pondération paramétrable).
- Export/impression (affichage en classe, communication aux parents).
```

---

# 3) MENU « PRÉSENCES »

### Workflow réel
Chaque jour (ou chaque cours), l'enseignant fait l'**appel** (registre d'appel). Les absences/retards sont notés, justifiés ou non. Les parents sont **prévenus** des absences (WhatsApp/SMS) — très attendu par les familles. L'assiduité figure sur le bulletin et le livret. Les présences des **enseignants** sont suivies séparément (base de la paie des vacataires).

### 3.a Appel de classe — `/presences/appel`
```
- Sélection classe + date (+ créneau/cours si appel par cours). Charge la liste des élèves.
- Saisie rapide : par défaut tous PRÉSENTS, l'enseignant bascule les ABSENT / RETARD + motif optionnel.
  Boutons "Tout présent", marquage en un tap (mobile-friendly). Enregistrer -> POST en lot.
- Une présence par élève/classe/date (ou /cours). Correction possible.
- À l'enregistrement d'une absence/retard -> notification automatique au parent (communication) ;
  indicateur "parent notifié".
```

### 3.b Suivi & justificatifs — `/presences/eleves`
```
- DataTable (filtres élève, classe, date, statut). Marquer une absence comme JUSTIFIÉE (motif + pièce
  jointe : certificat médical, mot du parent) ou NON JUSTIFIÉE.
- Statistiques par élève : taux d'assiduité par période (graphique), nb absences justifiées/non justifiées,
  retards. Alerte si absentéisme élevé (seuil paramétrable) -> convocation parent.
```

### 3.c Présences des enseignants — `/presences/enseignants`
```
- Pointage/suivi PRÉSENT/ABSENT (table SÉPARÉE des élèves), heures effectuées (utile paie vacataire/horaire).
  Statistiques par enseignant. Lien vers expenses/accounting pour la paie.
```

### 3.d Vue parent — portail
```
- Présences/absences de son enfant, taux d'assiduité, justifier une absence en ligne (upload du motif).
```

---

## ENCHAÎNEMENTS (cycle d'une période / trimestre)

```
1. (Début période) Enseignants créent leurs évaluations (interros, devoirs) ; appel quotidien.
2. (Cours) Saisie des notes au fil de l'eau + validation ; relances parents sur absences.
3. (Fin période) Composition (examen de fin de trimestre) -> notes saisies + validées.
4. Clôture de la période (academics) -> FIGE les notes -> génération des bulletins (lot, auto possible).
5. Conseil de classe : moyennes/rangs, distinctions, appréciations générales, (fin d'année) décisions de
   passage.
6. Publication des bulletins -> notification parents (WhatsApp/SMS/email) -> impression (signature + cachet).
7. Alimentation du LIVRET scolaire (report-card) : synthèse période + (fin d'année) moyenne annuelle, rang,
   décision, assiduité.
8. (Fin d'année) Palmarès, décisions de passage -> réinscription (proposition classe suivante) -> archivage.
```

> Cahier des charges fonctionnel du module Pédagogie. Se combine aux prompts back-end (grading, attendance,
> report-card) et au prompt front correspondant : ceux-ci doivent implémenter CES workflows, CES champs,
> CES garde-fous et CETTE ergonomie (saisie tableur rapide, adaptation au barème, conseil de classe,
> registre d'appel, relances parents, bulletins signés/cachetés selon l'identité de l'école).
```