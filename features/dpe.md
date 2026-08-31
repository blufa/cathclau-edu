# Mission : Concevoir le module de partenariat DPE et le tableau de bord de supervision scolaire

Tu travailles sur une plateforme guinéenne de suivi scolaire destinée aux écoles, parents, enseignants et à la Direction Préfectorale de l'Éducation (DPE).

Je veux que tu analyses d'abord le projet existant avant de modifier quoi que ce soit.

## 1. Contexte du projet

L'objectif est de proposer à une DPE, notamment la DPE de Coyah, une phase pilote permettant de connecter plusieurs écoles privées à notre plateforme.

Notre plateforme doit permettre notamment :

* le suivi des élèves ;
* le suivi des présences/absences ;
* la communication école-parent ;
* la consultation des notes et résultats ;
* les alertes aux parents ;
* les statistiques scolaires ;
* un tableau de bord de supervision destiné à la DPE.

Important :

Le Ministère de l'Éducation dispose déjà d'une plateforme officielle permettant notamment de calculer les notes et de générer les bulletins.

Nous ne voulons PAS remplacer cet algorithme officiel.

Nous voulons nous positionner comme une couche complémentaire de suivi, de communication et de supervision.

---

# 2. Première tâche : audit complet du projet

Avant de coder :

1. Analyse toute l'architecture existante.
2. Identifie :

    * backend ;
    * frontend ;
    * base de données ;
    * API existantes ;
    * système d'authentification ;
    * gestion des rôles ;
    * gestion des écoles ;
    * gestion des élèves ;
    * gestion des classes ;
    * gestion des enseignants ;
    * gestion des notes ;
    * gestion des présences ;
    * système de notifications ;
    * tableaux de bord existants.
3. Identifie les modèles/tables déjà disponibles.
4. Identifie les API existantes.
5. Identifie les fonctionnalités réutilisables.
6. Identifie les problèmes d'architecture ou de sécurité qui pourraient empêcher le déploiement à plusieurs écoles.
7. Ne recrée pas une fonctionnalité qui existe déjà.

À la fin de cette analyse, présente-moi :

* ce qui existe déjà ;
* ce qui manque ;
* ce qui peut être réutilisé ;
* ce qui doit être modifié ;
* ce qui doit être créé.

NE CODE PAS immédiatement.

---

# 3. Intégration avec la plateforme officielle du ministère

C'est une partie critique.

Le ministère dispose déjà d'un système qui calcule les notes et génère les bulletins.

Nous devons étudier DEUX scénarios.

## Scénario A — API officielle disponible

Vérifie dans le projet s'il existe déjà une possibilité d'intégration API.

Analyse comment notre système pourrait se connecter à une éventuelle API officielle du ministère.

Propose une architecture permettant de :

* récupérer les élèves ;
* récupérer les classes ;
* récupérer les matières ;
* récupérer les notes ;
* récupérer les moyennes ;
* récupérer les résultats ;
* éventuellement récupérer les bulletins ;
* synchroniser les données sans modifier les calculs officiels.

Notre système doit considérer la plateforme du ministère comme une source de vérité pour les résultats officiels.

Important :

Ne suppose pas que les API existent.

Si aucune API officielle n'est connue ou accessible, ne l'invente pas.

---

# 4. Scénario B — Import Excel

Si aucune API officielle n'est disponible, nous devons prévoir une deuxième méthode.

L'école doit pouvoir exporter les données depuis la plateforme du ministère sous forme de fichier Excel/CSV, puis importer ce fichier dans notre plateforme.

Conçois un système robuste d'importation.

Le système doit pouvoir :

1. uploader un fichier Excel/CSV ;
2. analyser automatiquement les colonnes ;
3. proposer un mapping des colonnes ;
4. permettre à l'administrateur de corriger le mapping ;
5. détecter les élèves existants ;
6. identifier les nouveaux élèves ;
7. éviter les doublons ;
8. détecter les notes invalides ;
9. détecter les élèves inconnus ;
10. détecter les classes inconnues ;
11. afficher un aperçu avant import ;
12. demander confirmation avant l'import définitif ;
13. enregistrer l'historique de chaque import ;
14. permettre de savoir qui a importé le fichier ;
15. enregistrer la date et l'heure de l'import ;
16. produire un rapport d'erreurs ;
17. permettre de corriger les erreurs puis recommencer ;
18. garantir l'idempotence : importer deux fois le même fichier ne doit pas créer de doublons.

Prévois également un système de version/historique afin de pouvoir savoir quelles données provenaient de quel import.

---

# 5. Architecture recommandée pour les notes

Ne duplique pas inutilement l'algorithme de calcul des notes du ministère.

Nous devons distinguer :

### Données officielles

* notes officielles ;
* moyennes officielles ;
* résultats officiels ;
* bulletins officiels.

### Données de suivi de notre plateforme

* présence ;
* absences ;
* retards ;
* comportement ;
* communication ;
* alertes ;
* statistiques ;
* évolution de l'élève.

Notre application doit pouvoir afficher les résultats officiels sans devenir le système officiel de calcul.

Si un calcul est nécessaire pour des statistiques internes, indique clairement qu'il s'agit d'une statistique de notre plateforme et non d'un résultat officiel du ministère.

---

# 6. Concevoir le rôle DPE

Créer ou améliorer un rôle :

`DPE_ADMIN`

La DPE doit avoir accès uniquement aux données relevant de son périmètre géographique/administratif.

Une DPE ne doit pas pouvoir voir les données d'une autre préfecture.

Prévoir une architecture permettant éventuellement plus tard :

* DPE ;
* Direction Régionale ;
* Ministère ;
* Administrateur national.

---

# 7. Tableau de bord DPE

C'est l'une des fonctionnalités les plus importantes du projet.

Conçois un tableau de bord destiné au Directeur Préfectoral et à ses services.

Le tableau de bord doit permettre d'avoir une vision globale de l'éducation dans les écoles partenaires.

## A. Vue générale

Afficher notamment :

* nombre total d'écoles partenaires ;
* nombre total d'élèves ;
* nombre total d'enseignants ;
* nombre total de classes ;
* effectif par école ;
* effectif par niveau ;
* effectif filles/garçons si cette donnée est disponible et légalement pertinente ;
* nombre d'élèves actifs ;
* nombre d'élèves en situation d'absence répétée ;
* taux global de présence ;
* taux global d'absence ;
* évolution mensuelle de la présence ;
* nombre d'élèves à risque de décrochage.

---

# 8. Statistiques par école

La DPE doit pouvoir sélectionner une école et consulter :

* nombre d'élèves ;
* nombre d'enseignants ;
* nombre de classes ;
* effectif par classe ;
* effectif par niveau ;
* taux de présence ;
* taux d'absence ;
* taux de retard ;
* évolution de la présence ;
* nombre d'élèves avec absences répétées ;
* nombre d'élèves à risque ;
* évolution des performances scolaires ;
* taux de réussite selon les données disponibles ;
* progression des résultats ;
* taux de saisie des données ;
* fréquence des mises à jour ;
* dernière synchronisation/importation avec le système officiel.

---

# 9. Statistiques par classe

La DPE doit pouvoir descendre :

DPE → École → Niveau → Classe → Élèves

Pour chaque classe :

* effectif ;
* présence moyenne ;
* absences ;
* retards ;
* évolution des résultats ;
* distribution des moyennes ;
* élèves en difficulté ;
* élèves avec absences répétées ;
* élèves présentant plusieurs indicateurs de risque.

---

# 10. Indicateur de risque de décrochage

Propose un système de scoring transparent.

Ne crée pas un algorithme opaque.

Par exemple, le score peut tenir compte de :

* fréquence des absences ;
* absences consécutives ;
* retards ;
* baisse importante des résultats ;
* absence prolongée ;
* autres indicateurs réellement disponibles dans la base.

Le tableau de bord DPE pourrait afficher :

🟢 Situation normale
🟠 Attention
🔴 Risque élevé

Mais :

* documente précisément la formule ;
* rends les seuils configurables ;
* ne présente jamais ce score comme un diagnostic ;
* indique clairement qu'il s'agit d'un indicateur statistique d'alerte.

---

# 11. Statistiques temporelles

La DPE doit pouvoir filtrer les statistiques par :

* aujourd'hui ;
* cette semaine ;
* ce mois ;
* trimestre ;
* année scolaire ;
* période personnalisée.

Prévoir des graphiques montrant notamment :

* évolution des présences ;
* évolution des absences ;
* évolution des retards ;
* évolution des effectifs ;
* évolution des performances ;
* évolution du nombre d'élèves à risque.

---

# 12. Comparaison des écoles

Prévoir une fonctionnalité permettant à la DPE de comparer les écoles partenaires.

Exemples :

| Indicateur       | École A | École B | École C |
| ---------------- | ------: | ------: | ------: |
| Effectif         |         |         |         |
| Présence         |         |         |         |
| Absence          |         |         |         |
| Retards          |         |         |         |
| Moyenne générale |         |         |         |
| Élèves à risque  |         |         |         |

Attention :

Les comparaisons doivent être présentées avec prudence.

Ne pas créer de classement arbitraire pouvant être interprété comme une sanction officielle.

---

# 13. Alertes DPE

Prévoir une page "Alertes".

Exemples :

* école avec taux d'absence anormalement élevé ;
* augmentation soudaine des absences ;
* classe avec beaucoup d'absences ;
* élèves avec absences répétées ;
* données non mises à jour ;
* école n'ayant pas synchronisé/importé ses données depuis longtemps ;
* problème d'import Excel ;
* incohérences dans les effectifs.

Chaque alerte doit avoir :

* niveau de priorité ;
* date ;
* école concernée ;
* classe concernée si applicable ;
* description ;
* statut : nouvelle / en cours / résolue ;
* historique.

---

# 14. Carte / vision géographique

Si les coordonnées des écoles sont disponibles, étudie la possibilité d'afficher une carte de la préfecture.

Chaque école pourrait être représentée avec un indicateur synthétique.

Exemple :

🟢 fonctionnement normal
🟠 plusieurs indicateurs à surveiller
🔴 situation nécessitant attention

La carte doit rester un outil de supervision, pas un classement public.

---

# 15. Rapports DPE

Prévoir la génération de rapports.

La DPE devrait pouvoir exporter :

* PDF ;
* Excel ;
* CSV si nécessaire.

Rapports possibles :

### Rapport mensuel DPE

* effectifs ;
* présence ;
* absences ;
* retards ;
* résultats ;
* alertes ;
* évolution par rapport au mois précédent.

### Rapport par école

* synthèse complète de l'école.

### Rapport par classe

* statistiques détaillées.

### Rapport annuel

* évolution de la situation scolaire sur l'année.

Prévoir également un système d'export anonymisé lorsque les données nominatives ne sont pas nécessaires.

---

# 16. Confidentialité et sécurité

Cette fonctionnalité concerne des données sensibles concernant des mineurs.

Tu dois donc intégrer une architecture de sécurité sérieuse.

Prévoir :

* contrôle d'accès par rôle ;
* contrôle d'accès par préfecture ;
* contrôle d'accès par école ;
* journalisation des accès ;
* audit des exports ;
* historique des imports ;
* chiffrement des données sensibles lorsque nécessaire ;
* limitation des données visibles selon le rôle ;
* protection contre l'accès à des élèves d'une autre école/préfecture ;
* aucune donnée sensible dans les logs ;
* protection des fichiers Excel importés ;
* suppression ou archivage sécurisé des fichiers temporaires.

Analyse également les risques RGPD et les obligations locales pertinentes, sans supposer qu'une seule réglementation étrangère s'applique automatiquement à la Guinée.

---

# 17. Architecture multi-écoles

Le système doit être pensé comme une plateforme multi-tenant.

Structure logique :

Ministère
↓
Direction Régionale
↓
DPE
↓
Écoles
↓
Classes
↓
Élèves

Une école ne doit accéder qu'à ses propres données.

Une DPE doit voir uniquement les écoles de son périmètre.

Un administrateur national pourra éventuellement voir plusieurs DPE.

Analyse l'architecture actuelle et propose les changements nécessaires.

---

# 18. API

Analyse les API actuelles du projet.

Propose les endpoints nécessaires pour le module DPE.

Exemples :

GET /api/dpe/dashboard

GET /api/dpe/schools

GET /api/dpe/schools/:id/statistics

GET /api/dpe/classes/:id/statistics

GET /api/dpe/alerts

GET /api/dpe/reports

POST /api/import/official-results

GET /api/import/history

GET /api/import/:id/errors

Mais adapte ces routes à l'architecture réelle du projet.

Ne crée pas ces endpoints aveuglément si une convention différente existe déjà.

---

# 19. Architecture d'intégration officielle

Propose une architecture abstraite permettant de supporter plusieurs sources de données.

Exemple conceptuel :

Official Education System
|
| API
|
v
OfficialDataAdapter
|
|------------------|
|                  |
v                  v
API Import            Excel Import
|                  |
|------------------|
|
v
Normalisation des données
|
v
Database
|
|--------|---------|
v        v         v
École     Parent    DPE

L'objectif est que si demain le ministère fournit une API officielle, nous puissions remplacer ou compléter l'import Excel sans refaire toute l'application.

---

# 20. Synchronisation

Si une API officielle existe :

prévoir une synchronisation :

* manuelle ;
* automatique ;
* planifiée.

Prévoir :

* dernière synchronisation ;
* statut ;
* nombre d'enregistrements récupérés ;
* nombre d'erreurs ;
* journal de synchronisation.

Si nous utilisons Excel :

afficher :

"Dernier import : 28/08/2026"

"Source : plateforme officielle du ministère"

"Nombre d'élèves importés : X"

"Nombre d'erreurs : X"

---

# 21. Dashboard directeur DPE

Créer une interface simple et professionnelle.

Le Directeur doit comprendre la situation de sa préfecture en moins de 30 secondes.

En haut :

* Écoles
* Élèves
* Enseignants
* Classes

Puis :

* Présence
* Absence
* Retards
* Élèves à risque

Puis :

* graphique évolution présence ;
* graphique évolution résultats ;
* écoles nécessitant attention ;
* dernières alertes.

Puis :

* bouton "Rapport DPE"
* bouton "Exporter Excel"
* bouton "Exporter PDF"

L'interface doit être adaptée au desktop mais rester responsive.

---

# 22. Principe fondamental

Ne construis pas un deuxième système scolaire officiel.

Notre positionnement est :

### Le ministère calcule et produit les résultats officiels.

### Notre plateforme collecte, centralise, communique, analyse et supervise.

La valeur ajoutée de notre plateforme est notamment :

* présence en temps réel ;
* communication parents-écoles ;
* alertes ;
* suivi des élèves ;
* statistiques ;
* supervision DPE ;
* visualisation ;
* détection précoce des situations à risque ;
* centralisation des informations.

---

# 23. Ce que je veux de toi AVANT de coder

Fais d'abord une analyse complète du repository.

Puis produis un document :

`DPE_ARCHITECTURE_ANALYSIS.md`

Ce document doit contenir :

1. Architecture actuelle
2. Fonctionnalités existantes
3. Fonctionnalités manquantes
4. Architecture proposée
5. Modèle de données
6. Architecture multi-écoles
7. Architecture multi-DPE
8. Système de rôles
9. API nécessaires
10. Système d'import Excel
11. Stratégie d'intégration API officielle
12. Stratégie fallback Excel
13. Dashboard DPE
14. Statistiques
15. Système d'alertes
16. Système de rapports
17. Sécurité
18. Audit/logging
19. Plan de migration
20. Plan de déploiement pilote
21. Risques techniques
22. Risques liés aux données
23. Recommandations

Ajoute également un diagramme d'architecture en Mermaid.

---

# 24. Deuxième document

Crée également :

`DPE_STATISTICS_SPECIFICATION.md`

Ce document doit définir précisément toutes les statistiques disponibles pour la DPE.

Pour chaque statistique, indique :

* nom ;
* définition ;
* formule ;
* source des données ;
* fréquence de calcul ;
* niveau : préfecture / école / classe / élève ;
* rôle autorisé ;
* période ;
* possibilité d'export ;
* limites/interprétation.

Exemple :

### Taux de présence

Formule :

élèves présents / élèves attendus × 100

Source :

module de présence.

Niveau :

DPE / école / classe / élève.

---

# 25. Troisième document

Crée :

`OFFICIAL_SYSTEM_INTEGRATION.md`

Il doit comparer :

### Option 1

Intégration API officielle.

### Option 2

Import Excel/CSV.

Pour chaque option :

* avantages ;
* inconvénients ;
* sécurité ;
* complexité ;
* maintenance ;
* fiabilité ;
* synchronisation ;
* gestion des erreurs ;
* coût ;
* dépendance au ministère.

Puis recommande la meilleure architecture pour commencer le pilote.

---

# 26. Important : ne pas inventer

Ne suppose jamais :

* qu'une API du ministère existe ;
* que nous avons accès à cette API ;
* que le format Excel est connu ;
* que les colonnes sont fixes ;
* que nous pouvons accéder directement aux données du ministère.

Si l'information n'existe pas dans le repository, indique :

`À CONFIRMER AVEC LE MINISTÈRE`

et propose une architecture permettant de continuer avec l'import Excel.

---

# 27. Méthode de travail

Travaille en plusieurs phases :

### Phase 1

Audit du projet.

### Phase 2

Analyse architecture DPE.

### Phase 3

Analyse intégration ministère.

### Phase 4

Conception statistiques.

### Phase 5

Conception dashboard.

### Phase 6

Conception import Excel.

### Phase 7

Plan d'implémentation.

Ne commence pas par modifier massivement le code.

Après l'analyse, donne-moi :

1. les principaux constats ;
2. les fichiers qui devront être modifiés ;
3. les nouvelles tables/modèles nécessaires ;
4. les nouveaux endpoints ;
5. les nouvelles pages ;
6. les risques ;
7. le plan d'implémentation par étapes.

Attends ensuite ma validation avant de réaliser les changements majeurs.

---

# Objectif final

Je veux obtenir une plateforme capable de convaincre une DPE que notre solution ne cherche pas à remplacer le système du ministère.

Elle doit au contraire montrer :

**Ministère = système officiel de calcul et de bulletins**

**Notre plateforme = système de suivi, communication, analyse et supervision**

Le Directeur DPE doit pouvoir ouvrir son tableau de bord et répondre immédiatement à des questions comme :

* Combien d'écoles partenaires avons-nous ?
* Combien d'élèves sont suivis ?
* Quelle école a le meilleur taux de présence ?
* Quelle école connaît une hausse des absences ?
* Quelles classes nécessitent une attention particulière ?
* Combien d'élèves présentent des signes de risque de décrochage ?
* Les effectifs évoluent-ils ?
* Les résultats évoluent-ils ?
* Quelle école n'a pas mis ses données à jour ?
* Quand les dernières données officielles ont-elles été importées ?
* Quelle est la situation globale de la préfecture ?
* Comment cette situation évolue-t-elle par rapport au mois précédent ?

Le résultat doit être une solution réaliste, sécurisée, extensible et adaptée à un pilote DPE en Guinée.


# MODULE : SUPERVISION DES ENSEIGNANTS PAR LA DPE

La DPE doit avoir une visibilité structurée sur les enseignants qui exercent dans chaque école partenaire de son périmètre.

L'objectif n'est pas de permettre à la DPE de gérer directement les enseignants à la place des écoles, mais de lui donner une **vision de supervision** sur le corps enseignant.

---

## 1. Vue globale des enseignants

Dans le dashboard DPE, ajouter une section :

### "Corps enseignant"

Afficher :

* nombre total d'enseignants ;
* nombre d'enseignants actifs ;
* nombre d'enseignants par école ;
* nombre d'enseignants par niveau ;
* nombre d'enseignants par matière ;
* nombre d'enseignants par classe ;
* nombre d'enseignants nouvellement ajoutés ;
* nombre d'enseignants ayant quitté une école ;
* enseignants actuellement affectés à plusieurs écoles, si cela est autorisé ;
* taux de complétude des informations des enseignants.

---

# 2. Liste des écoles et enseignants

La DPE doit pouvoir naviguer :

**DPE → École → Enseignants**

Pour chaque école, afficher :

* nom de l'école ;
* directeur ;
* nombre d'enseignants ;
* liste des enseignants ;
* matières enseignées ;
* classes enseignées ;
* statut de l'enseignant ;
* date d'affectation dans l'école ;
* dernière activité sur la plateforme.

Exemple :

### École ABC

| Enseignant   | Matière       | Classes      | Statut |
| ------------ | ------------- | ------------ | ------ |
| Enseignant A | Mathématiques | 10e A, 10e B | Actif  |
| Enseignant B | Français      | 7e A, 8e A   | Actif  |
| Enseignant C | Physique      | 11e A        | Actif  |

---

# 3. Fiche détaillée d'un enseignant

La DPE doit pouvoir cliquer sur un enseignant pour consulter une fiche de supervision.

Afficher uniquement les informations pertinentes et autorisées.

### Informations générales

* nom ;
* prénom ;
* identifiant interne ;
* sexe si nécessaire ;
* contact professionnel si autorisé ;
* statut ;
* école principale ;
* date d'intégration dans l'école.

### Affectations

Afficher :

* école ;
* matière ;
* niveau ;
* classe ;
* période d'affectation.

Exemple :

**Enseignant : Mamadou X**

École :
École ABC

Matières :

* Mathématiques

Classes :

* 10e A
* 10e B
* 11e A

---

# 4. Matières enseignées

La DPE doit pouvoir voir la répartition des enseignants par matière.

Exemple :

Mathématiques : 15 enseignants

Français : 18 enseignants

Anglais : 9 enseignants

Physique : 7 enseignants

Chimie : 6 enseignants

Histoire : 10 enseignants

Géographie : 10 enseignants

Cela permet à la DPE d'identifier rapidement les matières avec un manque potentiel d'enseignants.

---

# 5. Enseignants par classe

La DPE doit pouvoir descendre jusqu'à :

**École → Classe → Matière → Enseignant**

Exemple :

### 10e A

Mathématiques → Enseignant A

Français → Enseignant B

Anglais → Enseignant C

Physique → Enseignant D

Cela permet de répondre rapidement à :

> "Qui enseigne cette matière dans cette classe ?"

---

# 6. Charge d'enseignement

Si les données nécessaires existent dans le système, prévoir un indicateur de charge.

Pour chaque enseignant :

* nombre de classes ;
* nombre de matières ;
* nombre d'élèves suivis ;
* nombre d'heures prévues ;
* nombre d'heures réalisées, si cette donnée existe.

Attention :

Ne pas inventer les heures d'enseignement si elles ne sont pas enregistrées dans la plateforme.

Si ces données n'existent pas, prévoir l'architecture permettant de les ajouter ultérieurement.

---

# 7. Activité des enseignants sur la plateforme

La DPE peut avoir des indicateurs d'activité numérique.

Exemples :

* présence saisie ;
* notes importées/saisies ;
* cours ou contenus publiés, si cette fonctionnalité existe ;
* communications envoyées aux parents ;
* dernières connexions ;
* dernière activité.

Attention :

Ces données doivent être présentées comme des **indicateurs d'activité sur la plateforme**, et non comme une évaluation automatique de la qualité pédagogique de l'enseignant.

---

# 8. Alertes concernant les enseignants

Prévoir une section "Alertes enseignants".

Exemples :

* classe sans enseignant affecté ;
* matière sans enseignant affecté ;
* enseignant affecté à une classe mais aucune activité enregistrée ;
* enseignant affecté à plusieurs classes avec charge potentiellement excessive ;
* enseignant désactivé mais toujours affecté à une classe ;
* affectation incohérente ;
* données administratives incomplètes.

Chaque alerte doit être explicable.

---

# 9. Historique des affectations

C'est important pour la supervision DPE.

Conserver l'historique :

* enseignant ;
* école ;
* matière ;
* classe ;
* date de début ;
* date de fin ;
* utilisateur ayant effectué la modification.

Exemple :

**Enseignant A**

2025-2026 :
École ABC → Mathématiques → 10e A

2026-2027 :
École ABC → Mathématiques → 11e A

La DPE peut ainsi comprendre l'évolution du corps enseignant.

---

# 10. Comparaison entre écoles

La DPE doit pouvoir comparer les ressources humaines des écoles.

Exemple :

| École   | Élèves | Enseignants | Ratio élèves/enseignant |
| ------- | -----: | ----------: | ----------------------: |
| École A |    800 |          35 |                    22,9 |
| École B |    450 |          25 |                      18 |
| École C |    700 |          28 |                      25 |

Le ratio doit être clairement documenté et ne doit pas être présenté comme une norme officielle si aucune norme officielle n'est définie.

---

# 11. Indicateurs RH pour la DPE

Ajouter au dashboard :

### Ressources humaines

* enseignants actifs ;
* enseignants par école ;
* enseignants par matière ;
* enseignants par niveau ;
* enseignants par classe ;
* ratio élèves/enseignant ;
* évolution du nombre d'enseignants ;
* nouvelles affectations ;
* départs ;
* classes sans enseignant ;
* matières sans enseignant.

---

# 12. Gestion des droits

La DPE peut :

### Voir

* enseignants des écoles de son périmètre ;
* affectations ;
* matières ;
* classes ;
* indicateurs d'activité ;
* historique.

### Ne peut pas

* modifier les informations personnelles d'un enseignant sans autorisation ;
* supprimer un enseignant ;
* modifier une affectation directement si cette responsabilité appartient à l'école ;
* consulter les données des écoles hors de son périmètre.

Si une fonctionnalité de validation DPE est nécessaire ultérieurement, elle devra être séparée du simple accès en consultation.

---

# 13. Relation entre enseignants, classes et élèves

Le modèle de données doit permettre cette relation :

DPE
↓
École
↓
Enseignant
↓
Matière
↓
Classe
↓
Élèves

Mais également :

Enseignant
↓
Plusieurs classes
↓
Plusieurs élèves

et :

Élève
↓
Plusieurs matières
↓
Plusieurs enseignants

Utiliser une table d'affectation plutôt que de stocker simplement `teacher_id` directement dans la classe si le système doit gérer plusieurs enseignants, matières et années scolaires.

---

# 14. Année scolaire

Les affectations doivent être liées à une année scolaire.

Exemple :

`2025-2026`

`2026-2027`

Une même personne peut donc avoir différentes affectations selon l'année scolaire.

Ne jamais écraser l'historique.

---

# 15. Statistiques DPE supplémentaires

Ajouter au document `DPE_STATISTICS_SPECIFICATION.md` :

### Statistiques enseignants

Pour chaque statistique, documenter :

* définition ;
* formule ;
* source ;
* niveau ;
* période ;
* rôle autorisé ;
* fréquence ;
* interprétation.

Statistiques minimales :

1. Nombre d'enseignants actifs
2. Nombre d'enseignants par école
3. Nombre d'enseignants par matière
4. Nombre d'enseignants par niveau
5. Nombre d'enseignants par classe
6. Ratio élèves/enseignant
7. Nombre de classes sans enseignant
8. Nombre de matières sans enseignant
9. Évolution des effectifs enseignants
10. Nombre de nouvelles affectations
11. Nombre de départs
12. Taux de complétude des données enseignants
13. Activité numérique des enseignants
14. Nombre d'enseignants affectés à plusieurs écoles
15. Nombre d'enseignants avec affectations incohérentes

---

# 16. Vue synthétique pour le Directeur DPE

Sur la page principale du dashboard, ajouter une carte :

## 👨‍🏫 Enseignants

**1 245 enseignants**

* 1 180 actifs
* 65 nouveaux
* 12 classes sans enseignant
* 4 matières nécessitant une attention

Puis permettre de cliquer sur cette carte pour accéder au détail.

---

# 17. Objectif de supervision

La DPE doit pouvoir répondre rapidement à ces questions :

* Combien d'enseignants travaillent dans les écoles de ma préfecture ?
* Dans quelles écoles travaillent-ils ?
* Qui enseigne quelle matière ?
* Qui enseigne dans quelle classe ?
* Combien d'élèves sont pris en charge par chaque enseignant ?
* Existe-t-il des classes sans enseignant ?
* Existe-t-il des matières sans enseignant ?
* Comment évolue le corps enseignant ?
* Quels enseignants ont récemment été affectés ?
* Quelles écoles ont besoin de ressources humaines supplémentaires ?
* Les informations des enseignants sont-elles correctement renseignées ?

Le système doit fournir une **vision de supervision**, sans transformer automatiquement les données numériques en jugement sur la qualité pédagogique des enseignants.
