# Spécification des statistiques DPE

> Document produit conformément à `features/dpe.md` §24 et à l'extension « Supervision des
> enseignants » (§15 de ce même fichier). Toutes les formules s'appuient sur des données **déjà
> présentes dans le modèle actuel** (voir `DPE_ARCHITECTURE_ANALYSIS.md` §2-3) ; quand une donnée
> nécessaire n'existe pas encore, c'est indiqué explicitement dans « Limites ».
>
> **Principe transversal** : toute statistique de moyenne/résultat scolaire produite par cette
> spécification est une **statistique de suivi de notre plateforme**, jamais un résultat officiel du
> ministère (voir `OFFICIAL_SYSTEM_INTEGRATION.md` et §5 du cahier des charges). Chaque écran DPE
> affichant une moyenne doit porter cette mention.
>
> **Rôles autorisés** : sauf mention contraire, `DPE_ADMIN` (scopé à son périmètre) et `SUPER_ADMIN`
> (sans restriction). `SCHOOL_ADMIN`/`DIRECTOR` d'une école voient déjà l'équivalent de ces stats pour
> leur propre tenant via les dashboards existants (`SchoolDashboardController`) — non dupliqué ici.

## A. Vue générale de la préfecture (niveau DPE)

| # | Statistique | Formule | Source | Fréquence | Période | Export |
|---|---|---|---|---|---|---|
| A1 | Nombre d'écoles partenaires | `COUNT(tenants) WHERE prefecture_id = :p AND status = 'ACTIVE'` | `platform.Tenant` | Temps réel | Instantané | Oui |
| A2 | Nombre total d'élèves | `COUNT(students) WHERE tenant_id IN (:tenantIds) AND deleted_at IS NULL` | `students.Student` | Temps réel | Instantané | Oui |
| A3 | Nombre total d'enseignants | `COUNT(teachers) WHERE tenant_id IN (:tenantIds) AND active = true` | `staff.Teacher` | Temps réel | Instantané | Oui |
| A4 | Nombre total de classes | `COUNT(school_classes) WHERE tenant_id IN (:tenantIds)` pour l'année courante | `academics.Classe` | Temps réel | Année scolaire | Oui |
| A5 | Effectif par école | A2 groupé par `tenant_id` | `students.Student` | Temps réel | Instantané | Oui |
| A6 | Effectif par niveau | Regroupement par `Niveau.cycle` (pas par libellé texte — voir limite) | `academics.Niveau` + `Enrollment` | Temps réel | Année scolaire | Oui |
| A7 | Effectif filles/garçons | `COUNT(students) GROUP BY gender` | `students.Student.gender` | Temps réel | Instantané | Oui, avec avertissement de sensibilité |
| A8 | Nombre d'élèves actifs | `COUNT(students) WHERE deleted_at IS NULL` (le champ « actif » se déduit du soft-delete, aucun statut « inactif » distinct n'existe) | `students.Student` | Temps réel | Instantané | Oui |
| A9 | Élèves en absence répétée | `COUNT(students)` ayant ≥ N absences non justifiées sur la période (seuil configurable, voir §C — Score de risque) | `attendance.StudentAttendance` | Quotidien (job) | Configurable | Oui |
| A10 | Taux global de présence | `présences / (présences + absences) × 100`, agrégé sur le périmètre | `attendance.StudentAttendance` | Quotidien (job, mis en cache) | Configurable | Oui |
| A11 | Taux global d'absence | `100 − A10` | idem | Quotidien | Configurable | Oui |
| A12 | Évolution mensuelle de la présence | Série temporelle de A10 par mois | idem, agrégé mensuellement | Quotidien | 12 derniers mois | Oui |
| A13 | Élèves à risque de décrochage | `COUNT(students) WHERE risk_level IN ('ORANGE','ROUGE')` | `student_risk_scores` (nouvelle table, §C) | Quotidien (job) | Instantané | Oui |

**Limite A6** : `Niveau` est défini indépendamment par chaque tenant (pas de référentiel partagé).
L'agrégation par niveau au sens strict (« CM2 ») n'est fiable qu'au niveau d'une seule école ; à
l'échelle DPE, seul le regroupement par `cycle` (MATERNELLE/PRIMAIRE/COLLEGE/LYCEE/SUPERIEUR) est
garanti cohérent entre écoles.

**Limite A10-A12** : le taux de présence élève n'est aujourd'hui **pas pré-calculé** dans le code
existant (seul le taux enseignant l'est, à la volée). Il faudra soit calculer à la volée pour le
périmètre DPE (risque de coût si le périmètre est large), soit introduire un job planifié de mise en
cache quotidienne (recommandé, cohérent avec `DeadlineCheckScheduler`/`AbsenceAutoMarkScheduler`
existants).

## B. Statistiques par école (DPE → École)

| # | Statistique | Formule | Source | Niveau |
|---|---|---|---|---|
| B1-B5 | Élèves / enseignants / classes / effectif par classe / effectif par niveau | Identique à A2-A6, filtré sur un seul `tenant_id` | idem | École |
| B6 | Taux de présence | Identique à A10, filtré sur un seul `tenant_id` | `attendance.StudentAttendance` | École |
| B7 | Taux d'absence | `100 − B6` | idem | École |
| B8 | Taux de retard | `COUNT(status='LATE') / COUNT(*) × 100` sur la période | `attendance.StudentAttendance` | École |
| B9 | Évolution de la présence | Série temporelle de B6 | idem | École |
| B10 | Élèves avec absences répétées | Identique à A9, filtré | idem | École |
| B11 | Élèves à risque | Identique à A13, filtré | `student_risk_scores` | École |
| B12 | Évolution des performances scolaires | Série temporelle de `ReportCard.overallAverage` moyen par période, **libellée « statistique de suivi, pas un résultat officiel »** | `grading.ReportCard` | École |
| B13 | Taux de réussite (selon données disponibles) | `COUNT(ReportCard.decision = 'ADMIS') / COUNT(ReportCard) × 100` si le champ `decision` est renseigné par l'école ; **N/A sinon, jamais 0% par défaut** | `grading.ReportCard.decision` | École |
| B14 | Progression des résultats | Delta de B12 entre deux périodes | idem | École |
| B15 | Taux de saisie des données | `COUNT(présences saisies attendues) / COUNT(présences théoriques) × 100` — proxy de complétude, voir Limite générale ci-dessous | `attendance` | École |
| B16 | Fréquence des mises à jour | Date du dernier enregistrement (présence, note ou import) par école | multi-source (`MAX(created_at)`) | École |
| B17 | Dernière synchronisation/import officiel | `import_batches.importe_le` le plus récent pour ce tenant | `import_batches` (nouvelle table) | École |

**Limite transversale B15-B17** : ces indicateurs de « complétude » sont **indispensables** pour
éviter la mauvaise interprétation d'une absence de données comme une bonne performance (cf.
`DPE_ARCHITECTURE_ANALYSIS.md` §22) — ils doivent être affichés à côté de toute statistique agrégée,
pas dans un écran séparé consulté séparément.

## C. Statistiques par classe (DPE → École → Niveau → Classe → Élèves)

| # | Statistique | Formule | Source |
|---|---|---|---|
| C1 | Effectif | `Classe.headcount` (déjà maintenu à jour par événement) | `academics.Classe` |
| C2 | Présence moyenne | B6 filtré par `class_id` | `attendance.StudentAttendance` |
| C3 | Absences | `COUNT(status='ABSENT')` sur la période, par classe | idem |
| C4 | Retards | `COUNT(status='LATE')` sur la période, par classe | idem |
| C5 | Évolution des résultats | B12 filtré par classe | `grading.ReportCard` |
| C6 | Distribution des moyennes | Histogramme de `ReportCard.overallAverage` pour la classe/période | `grading.ReportCard` |
| C7 | Élèves en difficulté | `COUNT(ReportCard) WHERE overallAverage < seuil` (seuil configurable par tenant, cf. barème déjà paramétrable) | `grading.ReportCard` |
| C8 | Élèves avec absences répétées | A9 filtré par classe | `attendance.StudentAttendance` |
| C9 | Élèves multi-risques | `COUNT(students)` cumulant ≥ 2 facteurs de risque simultanés (voir score §D) | `student_risk_scores.facteurs` | 

### Score de risque de décrochage (§10 cahier des charges)

**Formule proposée (transparente, seuils configurables par tenant)** :

```
score = (poids_absence × absences_non_justifiees_normalisees)
      + (poids_absence_consecutive × plus_longue_serie_consecutive_normalisee)
      + (poids_retard × retards_normalises)
      + (poids_baisse_resultat × baisse_moyenne_normalisee)
```

- Chaque composante est normalisée sur `[0, 1]` par rapport à un seuil configurable (ex. « 5 absences
  non justifiées sur la période = 1.0 »).
- Poids par défaut suggérés (modifiables par tenant dans les paramètres, table `ParametreTenant`
  prévue au cahier des charges §15.2, actuellement absente et à créer) :
  `poids_absence=0.35, poids_absence_consecutive=0.30, poids_retard=0.10, poids_baisse_resultat=0.25`.
- **Seuils d'affichage** : 🟢 `score < 0.35` (Situation normale) · 🟠 `0.35 ≤ score < 0.65`
  (Attention) · 🔴 `score ≥ 0.65` (Risque élevé) — seuils eux-mêmes configurables.
- Résultat stocké dans `student_risk_scores.facteurs` (jsonb) pour que le détail du calcul reste
  consultable (« pourquoi ce score ? ») — jamais une boîte noire.
- **Limite explicite, à afficher dans l'UI à chaque occurrence** : *« Indicateur statistique
  d'alerte basé sur les données de présence et de résultats disponibles dans la plateforme — ne
  constitue pas un diagnostic. »*
- **Ne pas calculer si les données sont insuffisantes** : un élève avec moins de N jours de présence
  enregistrés sur la période ne doit pas recevoir de score (afficher « Données insuffisantes », pas un
  score à 0 qui serait faussement rassurant).

## D. Comparaison inter-écoles (§12)

Tableau généré à la demande (pas de pré-calcul), réunissant B1, B6, B7, B8, B12, B13 pour un ensemble
d'écoles choisi par la DPE. **Aucun classement ordonné par défaut** — tri disponible à la demande de
l'utilisateur uniquement, jamais un ordre imposé pouvant être lu comme un palmarès officiel.

## E. Statistiques enseignants / RH (extension §15 du cahier des charges)

| # | Statistique | Formule | Source | Niveau |
|---|---|---|---|---|
| E1 | Nombre d'enseignants actifs | `COUNT(teachers) WHERE active = true` | `staff.Teacher` | Préfecture/École |
| E2 | Enseignants par école | E1 groupé par `tenant_id` | idem | Préfecture |
| E3 | Enseignants par matière | `COUNT(DISTINCT teacher_id) GROUP BY subject_id` via `ClasseMatiere` | `academics.ClasseMatiere` | École/Préfecture |
| E4 | Enseignants par niveau | Via `ClasseMatiere.schoolClassId → Classe.levelId → Niveau.cycle` (même limite qu'A6 : agrégation fiable par `cycle` uniquement) | `academics.ClasseMatiere` + `Niveau` | École/Préfecture |
| E5 | Enseignants par classe | `COUNT(DISTINCT teacher_id) GROUP BY school_class_id` | `academics.ClasseMatiere` | École |
| E6 | Enseignants nouvellement ajoutés | `COUNT(teachers) WHERE hireDate ∈ période` ou, une fois `teacher_assignments` en place, `COUNT WHERE date_debut ∈ période` | `staff.Teacher.hireDate` / `teacher_assignments` | École/Préfecture |
| E7 | Enseignants ayant quitté une école | Nécessite `teacher_assignments.date_fin` renseignée — **N/A tant que la table n'existe pas** (voir `DPE_ARCHITECTURE_ANALYSIS.md` §5) | `teacher_assignments` (nouvelle) | École/Préfecture |
| E8 | Enseignants affectés à plusieurs écoles | `COUNT(teacher_id) HAVING COUNT(DISTINCT tenant_id) > 1` — **à vérifier si autorisé métier** ; `Teacher.tenantId` étant unique par ligne aujourd'hui (isolation stricte par tenant), un même enseignant physique dans 2 écoles correspond à 2 lignes `Teacher` distinctes sans lien entre elles. **À CONFIRMER** si un identifiant partagé (ex. numéro CNSS) doit servir de clé de rapprochement. | `staff.Teacher.cnssNumber` (rapprochement approximatif) | Préfecture |
| E9 | Taux de complétude des informations enseignants | `champs_renseignes / champs_attendus × 100` par enseignant, moyenné | `staff.Teacher` (tous champs) | École/Préfecture |
| E10 | Ratio élèves/enseignant | `A2 / A3` (ou B équivalent par école) | `students.Student` + `staff.Teacher` | École/Préfecture |
| E11 | Classes sans enseignant | `COUNT(school_classes) WHERE id NOT IN (SELECT school_class_id FROM classe_matiere)` | `academics.Classe` + `ClasseMatiere` | École/Préfecture |
| E12 | Matières sans enseignant | `COUNT(matieres) WHERE id NOT IN (SELECT subject_id FROM classe_matiere WHERE teacher_id IS NOT NULL)` | `academics.Matiere` + `ClasseMatiere` | École/Préfecture |
| E13 | Évolution du nombre d'enseignants | Série temporelle de E1 | `staff.Teacher.hireDate` | Préfecture |
| E14 | Nouvelles affectations / départs | Depuis `teacher_assignments.date_debut`/`date_fin` (nouvelle table, historique démarrant à l'activation — voir limite en §19 de l'analyse d'architecture) | `teacher_assignments` | École/Préfecture |
| E15 | Activité numérique des enseignants | Présence saisie (`TeacherAttendance`), notes saisies (`Grade.enteredAt`), communications envoyées, dernière connexion (`User.lastActivityAt`) — **à présenter uniquement comme indicateur d'usage de la plateforme, jamais comme évaluation pédagogique** | `attendance.TeacherAttendance`, `grading.Grade`, `communication`, `iam.User` | École |
| E16 | Charge d'enseignement (heures) | Heures prévues = dérivées de `EmploiDuTemps` (durée hebdomadaire × occurrences) ; heures réalisées = dérivées de `TeacherAttendance` si le pointage existe. **Ne pas inventer une donnée « heures réalisées » si le pointage n'est pas utilisé par l'école — afficher N/A** | `academics.EmploiDuTemps`, `attendance.TeacherAttendance` | Enseignant |

**Limite E8 — la plus importante de cette section** : le modèle actuel isole chaque `Teacher` par
tenant sans identifiant global. Le cahier des charges suppose la possibilité qu'un même enseignant
travaille dans plusieurs écoles ; le rapprochement ne peut se faire que par une donnée d'identité
commune (ex. numéro CNSS s'il est renseigné et fiable) — **résultat approximatif tant qu'aucun
identifiant national fiable n'est disponible**, à documenter clairement dans l'UI plutôt que de
présenter un chiffre comme exact.

## F. Alertes (référence croisée avec `dpe_alerts`, `DPE_ARCHITECTURE_ANALYSIS.md` §15)

Les alertes ne sont pas des « statistiques » au sens calcul/formule mais des règles de seuil
appliquées aux statistiques ci-dessus (ex. alerte si B7 dépasse un seuil configurable, ou si B17 date
de plus de N jours). Chaque règle doit documenter la statistique source et le seuil, exactement selon
le même principe de transparence que le score de risque (§C).
