# Vision Fondateur — Tableau de bord financier avancé & IA de prévision

> **Destiné à Claude Code.** Document à donner tel quel en prompt. Contexte : les fondateurs
> d'établissements scolaires perdent souvent de l'argent faute de visibilité sur le recouvrement des
> frais, la masse salariale réelle, et les bourses/prises en charge accordées. Ce document décrit le
> tableau de bord « Vision Fondateur » enrichi et un module d'analyse prédictive (IA), avec le
> processus à suivre avant de coder.

---

## Avant de coder — méthode à suivre

1. **Analyser l'existant** (voir §1 ci-dessous) — ne rien recréer qui existe déjà.
2. **Identifier les manques réels** (ce que l'existant ne calcule pas encore).
3. **Proposer une architecture** : endpoints d'agrégation en lecture seule, pas de nouvelles tables
   sauf si l'information n'est structurellement stockée nulle part (voir §5).
4. **Choisir la solution la plus simple** qui réutilise les entités déjà en place.
5. **Implémenter par étapes** (§7), en commençant par les agrégations les moins chères en calcul.
6. **Vérifier** avec des données réelles/de test que les totaux recoupent les modules sources
   (ex. la masse salariale calculée doit correspondre à la somme des fiches de paie).
7. **Ne pas casser l'existant** : `/api/v1/dashboard` (Vision Fondateur actuelle) reste fonctionnel
   pendant toute la montée en puissance ; les nouveaux indicateurs s'ajoutent, ne remplacent rien.

---

## 1. Ce qui existe déjà (à réutiliser, pas à reconstruire)

Le socle de données nécessaire à ce tableau de bord est **déjà là**, construit au fil des sessions
précédentes — le travail restant est presque entièrement de l'**agrégation en lecture seule** :

- **Isolation par année scolaire** : `Invoice.academicYearId` et `Payment.academicYearId` existent
  désormais (voir migration `V117`) — tout calcul de recouvrement peut être scopé proprement par
  année, plus de mélange entre années.
- **Échéances / tranches** : `Installment` (module `billing`), avec le système de tranches
  paramétrable par l'école (§ refonte échéances) — la notion de « combien d'élèves ont payé la
  tranche N » est directement calculable via `Invoice.installmentId` + `Invoice.status`.
- **Rémunération enseignants** : `TeacherCompensation.hourlyRateNormal` / `hourlyRateExam` (§6 paie),
  `PayrollService.computeTheoreticalMonthlyPay(teacherId, year, month)` calcule déjà la paie
  théorique mensuelle d'un enseignant à partir de son emploi du temps réel et du type de classe
  (normale/examen). C'est la brique de base pour la masse salariale globale.
- **Réductions familiales** : `FamilyDiscountUseCase.computeApplicable(tenantId, studentId, feeType)`
  calcule déjà un pourcentage de réduction par élève (ex. réduction fratrie) — à vérifier/étendre
  pour couvrir le cas « parent qui a soldé toute l'année à l'avance → 5 % ».
- **Bourses / exemptions** : module `ExemptionService` (`billing`) déjà présent — gère les élèves
  totalement ou partiellement exemptés de frais.
- **Budget annuel & dépenses** : `AnnualBudget`/`BudgetLine` (workflow de validation direction) et
  `Expense` (lié à une ligne de budget, suivi automatique du réalisé) — § refonte budget/dépenses.
- **Dashboards existants** : `GET /api/v1/dashboard` (école), `GET /api/v1/dashboard/finance`
  (comptable), `GET /api/v1/founder` ou équivalent (Vision Fondateur actuelle, à vérifier/étendre
  plutôt que dupliquer) — ces endpoints d'agrégation légère sont le bon endroit pour ajouter les
  nouveaux indicateurs, pas une nouvelle famille de endpoints séparée.

**Conséquence pratique** : ce chantier est en grande majorité du **calcul**, pas du **stockage**. Ne
créer une nouvelle table que si une donnée n'est structurellement enregistrée nulle part (ex. un
historique de prévisions IA, voir §6).

---

## 2. Recouvrement des frais

### Indicateurs à ajouter à `GET /api/v1/dashboard/founder` (ou nouvel endpoint dédié
`GET /api/v1/dashboard/founder/recouvrement`, au choix selon la taille de la réponse)

- **Taux de recouvrement global** de l'école (période sélectionnable : année en cours, mois,
  tranche) : `Σ montant_payé / Σ montant_attendu`, calculé sur les `Invoice` de l'année scolaire
  courante (déjà isolée par `academicYearId`).
- **Nombre d'élèves ayant payé par tranche** (ex. « Tranche 1 : 320/450 élèves à jour ») — calculable
  via `Invoice.installmentId` + `Invoice.status = PAID`, groupé par tranche.
- **Nombre d'élèves ayant payé par mois** — idem, groupé par mois si les frais sont mensualisés.
- **Taux de recouvrement par classe** : même calcul que le taux global, mais `GROUP BY classId`
  (via `SchoolFee.classId` → `Invoice.feeId` → `SchoolFee`).
- **Montant à recouvrer par classe** et **reste à recouvrer** (`Σ montant_total − Σ montant_payé`,
  par classe et pour l'école entière).
- **Top débiteurs** : élèves/classes avec le plus grand reste à payer (déjà en partie couvert par
  `RecoveryStage` en billing — réutiliser, pas dupliquer).

### Règles métier

- Toujours scoper par `academicYearId` (courante par défaut, sélectionnable dans l'UI comme sur les
  pages Factures/Paiements déjà mises à jour).
- Les distinctions « payé / partiellement payé / impayé » suivent `InvoiceStatus` existant
  (`UNPAID`, `PARTIAL`, `PAID`) — pas de nouvel enum.

---

## 3. Masse salariale & coût du personnel

- **Taux horaire total de tous les enseignants** : somme de `TeacherCompensation.hourlyRateNormal`
  et `hourlyRateExam` par enseignant (déjà stocké), affichée en cumul et en moyenne.
- **Masse salariale du personnel** (administratif/support, hors enseignants) : réutiliser
  `Employee.baseSalary` (module `staff`, déjà existant pour les employés non-enseignants) sommé sur
  le tenant.
- **Masse salariale mensuelle réalisée** : `Σ PayrollService.computeTheoreticalMonthlyPay(teacherId,
  year, month)` pour tous les enseignants actifs + `Σ Employee.baseSalary` — un seul appel
  d'agrégation par mois, réutilisant le calcul déjà validé en §6 paie (aucune nouvelle formule).
- **Masse salariale sur l'année** : somme des 9 mois calculés ainsi.
- **Ratio masse salariale / recettes encaissées** — un indicateur de santé financière simple et
  parlant pour un fondateur non-comptable (ex. « 62 % des encaissements partent en salaires »).

---

## 4. Élèves pris en charge / bourses

- **Nombre d'élèves pris en charge à 100 %** — via `ExemptionService` existant (exemption totale).
- **Nombre d'élèves pris en charge partiellement** — exemption partielle (`ExemptionService`) ou
  réduction familiale (`FamilyDiscountUseCase`) non nulle mais < 100 %.
- **Nombre d'élèves ayant soldé toute l'année à l'avance et bénéficié des 5 %** — nécessite de
  vérifier si cette règle des 5 % existe déjà dans `FamilyDiscountUseCase`/`ExemptionService` (à
  confirmer en lisant le code avant d'ajouter quoi que ce soit) ; si elle n'existe pas encore, c'est
  une **nouvelle règle de réduction** à ajouter dans ce moteur existant (pas un nouveau système
  parallèle) : condition = paiement `COMPLETE` d'un montant couvrant l'année entière avant une date
  seuil paramétrable, réduction automatique de 5 % appliquée à la facture.
- **Impact financier des bourses/prises en charge** : montant total « perdu » (non recouvré du fait
  des exemptions/réductions) sur l'année — donnée utile pour que le fondateur mesure le coût réel de
  sa politique sociale.

---

## 5. Ce qui manque réellement et doit être ajouté (proposé par l'assistant)

Au-delà de la demande initiale, les ajouts suivants comblent des angles morts fréquents chez les
fondateurs d'écoles en difficulté financière — à prioriser avec l'utilisateur avant implémentation :

| # | Ajout proposé | Pourquoi |
|---|----------------|----------|
| V1 | **Trésorerie prévisionnelle à 30/60/90 jours** (projection des encaissements attendus par tranche/échéance à venir moins les dépenses budgétées) | Le vrai risque de faillite d'une école, c'est un trou de trésorerie ponctuel malgré un bilan annuel positif. |
| V2 | **Alertes automatiques** (in-app + email, réutiliser `communication`) : taux de recouvrement d'une classe sous un seuil, masse salariale mensuelle dépassant les encaissements du mois, dépense hors budget | Transforme un tableau de chiffres en outil d'action, pas juste de constat. |
| V3 | **Comparatif inter-années** (recouvrement, masse salariale, effectifs) — les années scolaires étant déjà des entités distinctes, comparer N vs N-1 est peu coûteux | Un fondateur veut savoir si la situation s'améliore ou se dégrade, pas juste où il en est. |
| V4 | **Coût réel par élève** (masse salariale + dépenses de fonctionnement ÷ effectif) vs **frais moyen encaissé par élève** — rentabilité par niveau/classe | Répond directement à « est-ce que cette classe/ce niveau est rentable ? ». |
| V5 | **Historique des prévisions IA vs réalisé** (nécessite une table, seule vraie nouvelle donnée à stocker — voir §6) | Sans historique, impossible de savoir si le modèle de prévision est fiable dans le temps. |
| V6 | **Export du tableau de bord en PDF** (réutiliser l'infrastructure OpenPDF déjà en place pour factures/reçus/budgets) pour les réunions de conseil d'école | Un fondateur présente souvent ces chiffres à un conseil d'administration ou des partenaires. |

---

## 6. Outil d'analyse / IA de prévision

### Ce qu'il faut prévoir

- **Prévision du taux de recouvrement futur** (mois/trimestre suivant) à partir de l'historique des
  paiements par classe/niveau — un modèle simple (moyenne mobile pondérée, régression linéaire sur
  les N derniers mois) est largement suffisant pour démarrer ; **pas besoin d'un LLM ni d'un modèle
  entraîné lourd** pour ce cas d'usage — la donnée est structurée et le volume par école est faible.
- **Détection d'anomalies** : dépense inhabituelle, chute brutale du recouvrement d'une classe,
  enseignant dont les heures réalisées s'écartent fortement du théorique.
- **Prévision de perte/gain de fin d'année** : projection du solde (recettes attendues − masse
  salariale − dépenses budgétées) à date, extrapolé sur les mois restants.

### Architecture recommandée

- **Nouveau module `insights`** (ou sous-module de `billing`/`platform` selon préférence) exposant :
  - `GET /api/v1/dashboard/founder/previsions` — renvoie les projections calculées à la demande
    (pas de job planifié nécessaire au démarrage : le volume de données par tenant reste faible).
  - Le calcul reste **côté backend Java**, en pure logique de domaine (comme
    `SubjectAverageCalculator`/`GradingFormulaEngine` déjà dans ce codebase) — pas de service externe
    ni de dépendance à un fournisseur d'IA pour cette première version. Une intégration à un vrai
    modèle (ex. appel à une API d'IA) peut venir dans un second temps si les prévisions simples
    s'avèrent insuffisantes — à valider avec des données réelles avant d'investir dans plus complexe.
- **Nouvelle table** (la seule nécessaire de tout ce document) : `founder_forecast_snapshot`
  (`id, tenant_id, academic_year_id, computed_at, metric, predicted_value, actual_value_when_known`)
  — permet de comparer prévision vs réalisé dans le temps (§V5) sans quoi aucune amélioration du
  modèle n'est mesurable.

### Tableaux de bord & diagrammes (frontend)

- Réutiliser les composants graphiques déjà en place dans ce projet
  (`BarTrendChart`, `PieBreakdownChart` — voir `src/components/charts/`) plutôt que d'introduire une
  nouvelle bibliothèque de graphiques. Le principe déjà appliqué dans ce projet (barres/donuts avec
  valeurs et pourcentages affichés en clair, pas de camembert nu) doit rester la norme pour tout
  nouveau graphique de ce tableau de bord — public non technique.
- Une page dédiée (ex. `/founder` déjà existante à étendre, ou `/founder/previsions` en sous-onglet)
  avec : carte de synthèse (taux de recouvrement, masse salariale du mois, solde prévisionnel),
  graphique de tendance sur 9 mois, alertes actives, comparatif N vs N-1.

---

## 7. Ordre d'implémentation recommandé

1. Vérifier l'existant exact (lire le code de `FamilyDiscountUseCase`, `ExemptionService`,
   `PayrollService`, le dashboard fondateur actuel) avant d'écrire une ligne — beaucoup de ce qui est
   demandé existe peut-être déjà partiellement.
2. Indicateurs de recouvrement (§2) — le plus directement utile, agrégations pures sur `Invoice`.
3. Masse salariale (§3) — agrégations sur `TeacherCompensation`/`PayrollService`/`Employee`.
4. Élèves pris en charge (§4) — vérifier/étendre les règles de réduction existantes.
5. Alertes (§V2) et comparatif inter-années (§V3) — forte valeur, faible coût d'implémentation.
6. Prévisions IA simples (§6) — une fois les indicateurs bruts fiables et vérifiés.
7. Export PDF (§V6) et trésorerie prévisionnelle (§V1) en dernier — les plus coûteux, à ne faire que
   si les étapes précédentes confirment leur utilité réelle pour l'utilisateur.

---

*Fin du document — à valider point par point avec l'utilisateur (notamment §5 et §6, qui contiennent
des propositions au-delà de la demande initiale) avant de lancer l'implémentation.*
