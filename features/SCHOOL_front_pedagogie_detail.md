# SCHOOL SaaS — Module PÉDAGOGIE : prompt FRONT d'implémentation (écran par écran)

> Version « implémentation » du cahier des charges `SCHOOL_pedagogie_workflow.md`. À coller dans Claude Code.
> Couvre : Évaluations & Notes, Bulletins (+ conseil de classe), Présences.
> Socle : Next.js (App Router, TS) + Tailwind + Mantine, client API typé (OpenAPI), TanStack Query,
> @mantine/form + zod, i18n FR, AppShell, DataTable, erreurs en toasts (400/401/403/404/409/422/429).
> Contexte : école guinéenne, 3 trimestres (ou semestres selon paramétrage), composition de fin de période,
> moyennes pondérées /20, rang par classe, distinctions, bulletins signés + cachetés. L'UI s'ADAPTE au
> barème de la classe (NOTE_CHIFFREE | COMPETENCE | CREDITS). Devise GNF si montants. Mobile-friendly pour
> la saisie enseignant.

---

## 0. COMPOSANTS COMMUNS À CONSTRUIRE D'ABORD

```
- PeriodSelect : sélecteur de période (trimestre/semestre) de l'année courante ; "courante" par défaut.
- GradeInput : champ de note intelligent -> selon le barème de la classe, rend un input numérique
  (0..note_sur, validation live, couleur rouge/orange/vert selon seuil), OU un select de compétence
  (A/B/C/D avec couleurs), OU une saisie crédits/UE. Props : {mode, noteSur, seuil, value, onChange}.
- GradeEntryGrid : grille de saisie type tableur (1 ligne/élève) basée sur GradeInput, navigation clavier
  (Entrée/Tab = suivant), collage Excel, compteur "X/Y saisies", colonne Absent/Dispensé, brouillon auto.
- LockBadge : indicateur verrouillé/déverrouillé (notes validées, période clôturée).
- MentionBadge : Tableau d'honneur / Encouragements / Félicitations / Prix d'excellence (couleurs).
- RankPill : rang "5e/42".
- BulletinView : composant d'affichage d'un bulletin qui s'adapte au barème (chiffré / compétences /
  crédits), réutilisé pour l'aperçu et le PDF (mise en page = identité école via TenantSettings).
- AttendanceQuickMark : boutons PRÉSENT/ABSENT/RETARD tactiles pour l'appel mobile.
- AssiduityChart : graphique taux d'assiduité par période.
Règles transverses : période courante par défaut, FR (i18n), adaptation barème partout, respect des droits
(enseignant = ses classes/matières -> 403 sinon), période clôturée = lecture seule, statut école/modules
actifs respectés, erreurs en toasts, responsive (saisie au téléphone).
```

---

## 1. ÉVALUATIONS & NOTES — `/notes`

### 1.a Mes évaluations — `/notes/evaluations`
```
DataTable (GET /api/v1/evaluations, filtré par enseignant connecté ; vue élargie si ADMIN/DIRECTEUR) :
- Colonnes : date, classe, matière, type (INTERROGATION|DEVOIR|COMPOSITION|EXAMEN), note_sur, coefficient,
  avancement saisie (badge "28/30"), statut (BROUILLON|SAISIE_EN_COURS|VALIDEE + LockBadge).
- Filtres : classe, matière, période (PeriodSelect), type, statut.
- Actions par ligne : Saisir/Modifier les notes, Voir, Valider, Supprimer (si non validée).
- Bouton "Nouvelle évaluation".
- État vide : "Aucune évaluation sur cette période. Créez-en une pour commencer la saisie des notes."
```

### 1.b Créer / éditer une évaluation — `/notes/evaluations/nouvelle`
```
Formulaire (zod) :
- classe* (ClassSelect limité aux classes de l'enseignant),
- matiere* (limitée aux matières qu'il enseigne dans cette classe -> sinon 403),
- periode* (PeriodSelect, courante par défaut),
- type* (INTERROGATION|DEVOIR|COMPOSITION|EXAMEN),
- titre/objet (texte court), date* (datepicker),
- note_sur* (défaut selon barème, ex. 20), coefficient* (poids dans la matière, défaut 1).
Règles : bloqué si période clôturée ou année non EN_COURS (champ désactivé + message explicite).
Submit -> redirige vers la grille de saisie (1.c).
```

### 1.c Saisie des notes — `/notes/evaluations/{id}/saisie`
```
En-tête fixe (sticky) : classe, matière, période, type, note_sur, coefficient, compteur "X/Y saisies",
  LockBadge. Boutons : "Enregistrer", "Valider les notes".
GradeEntryGrid : une ligne par élève (photo, matricule, nom) + GradeInput (adapté au barème) + case
  Absent/Dispensé. 
- NOTE_CHIFFREE : input 0..note_sur, validation live (422 visuel si hors borne), couleur selon seuil.
- COMPETENCE (primaire) : select A/B/C/D (couleurs) au lieu d'une note.
- CREDITS (supérieur) : saisie par UE.
Fonctions : "Tout absent", collage depuis Excel, tri par nom/matricule, recherche élève, brouillon auto.
VALIDATION : "Valider les notes" -> confirmation -> verrouille (cadenas). Une fois validées, édition
  nécessite un déverrouillage par le directeur (bouton visible seulement pour rôle autorisé).
Bloqué si période clôturée (lecture seule + message).
POST /api/v1/evaluations/{id}/notes/lot ; PATCH /api/v1/evaluations/{id}/notes/valider ;
PUT /api/v1/notes/{id} (correction unitaire si déverrouillé).
```

### 1.d Examens / compositions — `/notes/examens`
```
- Liste (filtres classe, période, statut). Planifier (POST) : classe, période, type
  (BLANC|SEMESTRIEL|ANNUEL|NATIONAL), date. Changer statut (PATCH : PLANIFIE→EN_COURS→TERMINE).
- Générer la CONVOCATION PDF (GET /examens/{id}/convocation, asynchrone) — utile pour compositions
  trimestrielles et BEPC/Bac blancs.
```

### 1.e Relevé de notes (dans le dossier élève / portail)
```
- Vue lecture par élève : évaluations + notes + moyennes par matière de la période. Parent/élève en
  lecture seule.
```

---

## 2. BULLETINS — `/bulletins`

### 2.a Génération — `/bulletins/generer`
```
- Sélection classe + période (PeriodSelect). Bouton "Générer les bulletins".
- Activé UNIQUEMENT si la période est CLÔTURÉE (sinon encart explicatif + lien vers academics).
- Génération asynchrone : barre de progression, suivi du job. Notification quand prêt.
- Indiquer que la génération peut aussi se déclencher automatiquement à la clôture de période.
POST /api/v1/bulletins/generer.
```

### 2.b Liste & suivi — `/bulletins`
```
DataTable (GET /api/v1/bulletins, filtres élève, classe, période, statut BROUILLON|GENERE|PUBLIE) :
- Colonnes : élève, classe, période, moyenne générale, RankPill, MentionBadge, statut.
- Actions : ouvrir, publier (unitaire), PDF, "Publier toute la classe", "Imprimer la classe" (lot PDF).
```

### 2.c Détail d'un bulletin — `/bulletins/{id}` (BulletinView)
```
Affichage adapté au barème (BulletinView) :
- En-tête identité école (logo/nom/adresse/cachet via TenantSettings) + élève (photo, matricule, classe,
  année, période).
- [CHIFFRÉ] Tableau par matière : matière, coefficient, moyenne élève, MOYENNE CLASSE, MIN, MAX,
  appréciation du prof, enseignant. Synthèse : moyenne générale, RankPill ("5e/42"), MentionBadge,
  décision, total coefficients.
- [COMPÉTENCE / primaire] Domaines & compétences avec niveaux A/B/C/D (couleurs) + appréciations ; rang
  optionnel.
- [CRÉDITS / supérieur] UE, crédits, validation, moyenne pondérée crédits.
- Vie scolaire : absences/retards de la période (depuis attendance).
- Appréciation du conseil de classe + visa directeur + distinction.
- Espaces signatures (directeur + parent) + cachet.
- Boutons : "Publier" (-> PUBLIE + notifie parent via communication), "PDF" (GET /{id}/pdf),
  "Imprimer la classe".
- Vue parent/élève : lecture seule des bulletins PUBLIÉS.
```

### 2.d Conseil de classe — `/bulletins/conseil-de-classe`
```
Écran de délibération par classe + période (rôles DIRECTEUR/ADMIN + profs en consultation) :
- Tableau de TOUS les élèves triés par rang : moyenne générale, moyennes par matière (colonnes), absences.
- Saisie en lot : appréciation générale par élève (champ texte avec garde anti-formulations dévalorisantes :
  suggestions d'appréciations bienveillantes), attribution de distinction (PRÉ-PROPOSÉE selon seuils du
  barème : ≥13 Tableau d'honneur, ≥15 Félicitations, ≥17 Prix d'excellence — modifiable), avertissements
  travail/conduite.
- Statistiques de classe : moyenne, taux de réussite, répartition (histogramme), majors, élèves en
  difficulté.
- (FIN D'ANNÉE) Décision de passage par élève : ADMIS | REDOUBLE | ADMIS_CONDITION -> alimente le livret
  (report-card) et la proposition de classe à la réinscription.
- "Valider le conseil" -> verrouille les appréciations + autorise la publication groupée des bulletins.
```

### 2.e Palmarès & synthèse annuelle — `/bulletins/palmares`
```
- Tableau d'honneur / palmarès par classe ou école et par période (majors, distinctions). Export/impression.
- Bulletin de synthèse annuelle : moyenne annuelle (moyenne des périodes, pondération paramétrable),
  rang annuel, décision. PDF.
```

---

## 3. PRÉSENCES — `/presences`

### 3.a Appel de classe — `/presences/appel`
```
- Sélecteurs : classe + date (défaut aujourd'hui) [+ créneau/cours si appel par cours].
- Liste des élèves (photo, nom) ; par défaut tous PRÉSENTS. AttendanceQuickMark : basculer ABSENT/RETARD
  en un tap + motif optionnel. Boutons "Tout présent", compteur présents/absents.
- Enregistrer -> POST /api/v1/presences-eleves/lot. Une présence par élève/date (ou /cours), correction
  possible. Optimisé mobile (gros boutons, peu de saisie).
- À l'enregistrement d'une absence/retard : notification auto au parent (communication) ; afficher l'état
  "parent notifié".
```

### 3.b Suivi & justificatifs — `/presences/eleves`
```
- DataTable (GET /api/v1/presences-eleves, filtres élève, classe, date, statut).
- Marquer JUSTIFIÉE (motif + pièce jointe : certificat, mot du parent) / NON JUSTIFIÉE. Correction (PUT).
- Statistiques par élève (GET /api/v1/eleves/{id}/presences/statistiques) : taux d'assiduité par période
  (AssiduityChart), nb absences justifiées/non justifiées, retards. Alerte absentéisme (seuil) ->
  proposer convocation parent.
```

### 3.c Présences enseignants — `/presences/enseignants`
```
- Pointage/suivi PRÉSENT/ABSENT (table SÉPARÉE des élèves), heures effectuées (base paie horaire/vacataire).
  Stats par enseignant. Lien vers expenses/accounting (paie).
```

### 3.d Vue parent — `/portail/presences`
```
- Présences/absences de son enfant, taux d'assiduité, justifier une absence en ligne (upload du motif).
```

---

## 4. DÉFINITION DE TERMINÉ (module Pédagogie)
```
[ ] Composants : PeriodSelect, GradeInput (3 modes), GradeEntryGrid, LockBadge, MentionBadge, RankPill,
    BulletinView (adapté barème), AttendanceQuickMark, AssiduityChart.
[ ] Évaluations : mes évaluations + création (droits classe/matière) + saisie tableur rapide adaptée au
    barème + validation/verrou + déverrouillage directeur + examens/convocation + relevé élève.
[ ] Bulletins : génération en lot (après clôture) + liste/suivi + détail adapté au barème (chiffré/
    compétences/crédits) avec moyenne classe + min/max + rang + mention + décision + signatures/cachet +
    publication + PDF + impression classe.
[ ] Conseil de classe : tableau délibération + appréciations (bienveillantes) + distinctions pré-proposées +
    statistiques + décisions de passage (fin d'année) + validation/verrou.
[ ] Palmarès + synthèse annuelle.
[ ] Présences : appel mobile rapide + notification parent + justificatifs + statistiques/assiduité +
    présences enseignants séparées + vue parent.
[ ] Transverse : période courante par défaut, adaptation barème partout, droits enseignant (403),
    période clôturée en lecture seule, i18n FR, responsive, erreurs en toasts.
```

> Ce prompt implémente le cahier des charges `SCHOOL_pedagogie_workflow.md`. Il se branche sur grading
> (barèmes/notes/bulletins), attendance (présences) et report-card (livret). Respecter l'ergonomie
> « saisie rapide enseignant », l'adaptation au type d'établissement (primaire/collège/lycée/supérieur),
> et l'identité visuelle de l'école (TenantSettings) sur les bulletins et convocations.
```