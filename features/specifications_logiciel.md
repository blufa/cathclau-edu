# Document de Spécifications et Directives de Refactoring Logiciel

## 🎯 Objectif du Projet
Tu es un ingénieur logiciel expert. Ce document contient la cartographie officielle des matières scolaires (selon les normes MEPUA de Guinée) à injecter dans le système, ainsi qu'une liste de correctifs critiques, de règles de gestion et de simplifications fonctionnelles à implémenter immédiatement dans l'application de gestion scolaire.

---

## 📚 PARTIE 1 : Base de Données des Matières & Coefficients (Normes MEPUA)

Ces données doivent servir de dictionnaires ou de tables de configuration initiales pour la programmation annuelle effectuée par le Directeur des Études.

### 1.1 Enseignement Primaire
* **Classes concernées :** CP1, CP2, CE1, CE2, CM1, CM2.
* **Règle de gestion :** Tronc commun standard. Par défaut, coefficient uniforme (1) ou basé sur la pondération horaire de l'établissement.
* **Liste des matières :**
    - Rédaction / Composition
    - Dictée - Questions
    - Calcul Écrit
    - Calcul Mental
    - Histoire
    - Géographie
    - Sciences d'Observation
    - Éducation Civique et Morale (ECM)
    - Dessin / Travail Manuel
    - Éducation Physique et Sportive (EPS)

### 1.2 Enseignement Secondaire - Premier Cycle (Collège)
* **Classes concernées :** 7ème Année, 8ème Année, 9ème Année, 10ème Année.
* **Tableau des coefficients officiels (BEPC / Moyennes) :**

| Code Matière | Libellé de la Matière | Coefficient |
| :--- | :--- | :---: |
| `REDAC` | Rédaction | 2 |
| `DICT_Q` | Dictée - Questions | 2 |
| `MATH` | Mathématiques | 4 |
| `PHYS` | Sciences Physique | 2 |
| `SVT` | Sciences de la Vie et de la Terre | 2 |
| `HIST` | Histoire | 1 |
| `GEO` | Géographie | 1 |
| `ECM` | Éducation Civique et Morale | 1 |
| `ANG` | Anglais | 2 |
| `EPS` | Éducation Physique et Sportive | 1 |

### 1.3 Enseignement Secondaire - Second Cycle (Lycée)

#### Classe de 11ème Année (Tronc Commun de Transition)
* **Profil : Sciences (Futurs SM / SE)**
    - Mathématiques (Coeff : 4) | Physique (Coeff : 3) | Chimie (Coeff : 2) | SVT (Coeff : 2) | Français (Coeff : 2) | Anglais (Coeff : 2) | Histoire-Géographie (Coeff : 1) | EPS (Coeff : 1)
* **Profil : Sciences Sociales (Futurs SS)**
    - Histoire (Coeff : 3) | Géographie (Coeff : 3) | Français / Littérature (Coeff : 3) | ECM (Coeff : 2) | Anglais (Coeff : 2) | Mathématiques Appliquées (Coeff : 2) | Philosophie (Intro) (Coeff : 1) | EPS (Coeff : 1)

#### Classes de 12ème et Terminales (Profils du Baccalauréat Unique)

| Code Matière | Libellé | Sciences Mathématiques (SM) | Sciences Expérimentales (SE) | Sciences Sociales (SS) |
| :--- | :--- | :---: | :---: | :---: |
| `MATH` | Mathématiques | **5** | 3 | 2 |
| `PHYS` | Physique | 4 | 3 | - |
| `CHIM` | Chimie | 3 | 3 | - |
| `SVT` | SVT (Biologie) | 1 | **5** | - |
| `FRAN` | Français / Littérature | 2 | 2 | 3 |
| `PHILO` | Philosophie | 2 | 2 | **4** |
| `HIST` | Histoire | - | - | 3 |
| `GEO` | Géographie | - | - | 3 |
| `ECON_ECM`| Économie / ECM | - | - | 2 |
| `ANG` | Anglais | 2 | 2 | 2 |

---

## 🛠️ PARTIE 2 : Directives de Refactoring & Règles Métiers

### 🔐 1. Gestion des Droits d'Accès et des Interfaces (Contrôle d'Accès RBAC)
* **Interface Enseignant :** Supprimer complètement le tableau de bord de l'administration générale et l'accès aux interfaces de configuration globale. L'enseignant doit être redirigé et restreint exclusivement à son **Espace Enseignant** (ses classes, ses notes, son emploi du temps).
* **Interface Élève :** Masquer et interdire strictement l'accès au tableau de bord de l'administration. L'élève accède uniquement à son profil, ses notes et ses frais.

### 🗓️ 2. Module Emploi du Temps, Disponibilités & Conflits
* **Saisie des Disponibilités Professeur :** Lors de la saisie de ses créneaux, le professeur doit obligatoirement sélectionner la **Classe** et la **Matière** associées.
* **Visibilité des Bloquages :** Dès qu'un créneau horaire est réservé ou occupé, l'interface du professeur doit afficher visuellement les jours occupés ainsi que les classes concernées pour éviter les doubles réservations.
* **Génération Automatique (Responsable Pédagogique) :** Lors du lancement de la génération automatique de l'emploi du temps, l'algorithme doit immédiatement intercepter et signaler visuellement toute collision ou conflit de ressources (professeur ou salle indisponible).
* **Affectation et Grille Horaire :**
    - Au début de l'année, le Directeur des Études affecte des codes uniques aux professeurs par matière (ex: premier prof de chimie = `CH1`, deuxième = `CH2`).
    - L'interface d'emploi du temps par classe doit se présenter sous forme de matrice avec des plages horaires fixes : `08h-10h`, `10h-12h`, `12h-14h`, `14h-16h`.
    - Dans chaque cellule de cette grille, intégrer un menu déroulant (**Dropdown**) permettant d'affecter ou de modifier facilement le code du professeur (`CH1`, `MATH2`, etc.).
    - Si un professeur demande un changement, le responsable doit pouvoir le retirer et en sélectionner un autre directement depuis ce Dropdown.
    - Cette grille sur les jours ouvrés doit alimenter automatiquement le compteur du nombre d'heures mensuelles du professeur.

### 📝 3. Flux d'Actions Enseignant (Cahier de Textes & Incidents)
* **Suivi des Séances :** Sur le formulaire d'ajout d'une séance de cours, **retirer le bouton "Réaliser"** du champ d'action de l'enseignant (la validation de réalisation relève de l'administration). Remplacer ou privilégier la possibilité pour l'enseignant de **Modifier** sa saisie.
* **Signalements d'Incidents :** Lorsqu'un enseignant crée un ticket de signalement, **supprimer les boutons "Résoudre" et "Classer"**. Ces actions sont réservées exclusivement aux rôles disposant des privilèges administratifs adéquats.

### 💰 4. Inscription, Facturation & Scolarité Élève
* **Frais Supplémentaires Flexibles :** Sur le formulaire d'inscription de l'élève, ajouter un système de lignes dynamiques pour les frais annexes (tenue scolaire, rames de papier, bus de transport, etc.). Le système doit permettre de créer ces catégories de frais à la volée et d'y appliquer un prix au moment de fixer les échéances de la classe.
* **Prise en Charge / Bourses :** Dès l'inscription, ajouter un indicateur/champ définissant si l'élève bénéficie d'une prise en charge (totale ou partielle) s'appliquant sur l'ensemble de la scolarité et de l'inscription.
* **Paiement Intégral & Calcul de Remise :** Si l'utilisateur coche l'option de paiement intégral (paiement de toute l'année en une fois), le formulaire doit afficher dynamiquement un champ d'entrée (`input`) pour saisir une **remise en pourcentage (%)**. Le système doit calculer automatiquement en temps réel et afficher le montant net final que le parent doit payer.
* **Recherche & Restant Dû rapide :** Développer un module de recherche rapide d'élève pour le guichet d'accueil. Sur saisie du nom/matricule de l'enfant, le système doit afficher instantanément son solde financier et générer un **reçu imprimable du reste à payer** à remettre directement au parent demandeur.

### 📊 5. Simplification de la Comptabilité & Vision Fondateur
* **Suppression de la Complexité :** Supprimer entièrement le menu "Comptabilité" avancé et lourd. Rendre le système extrêmement simple.
* **Gestion Budgétaire par Soustraction :**
    - L'administration crée de simples catégories de dépenses spécifiques (ex: *Test d'inscription, Documents, Craies, Carburant, Garage, Rémunération Profs*).
    - L'utilisateur fixe un budget global sur cette catégorie.
    - À chaque fois qu'une dépense est imputée sur une catégorie, le système effectue une soustraction automatique pour mettre à jour le solde du budget de la catégorie.
* **Vue Globale Fondateur :** Mettre à disposition un tableau de bord épuré pour le Fondateur, centré principalement sur la consolidation en temps réel des **Recettes de l'école** (entrées de scolarité, inscriptions, frais annexes) et de la balance globale.

### 💶 6. Automatisation de la Paie des Enseignants
* **Règle de Calcul Horaires :** Les taux horaires ou salaires de base sont prédéfinis dans le système selon la typologie de la classe (classe normale vs classe d'examen).
* **Calcul Automatique du Net à Percevoir :** Le module de paie doit être directement interconnecté avec l'emploi du temps validé. À la fin du mois, le système calcule le nombre d'heures théoriques qui devaient être effectuées sur les jours ouvrés selon les classes de rattachement du professeur, applique le tarif horaire correspondant, et détermine automatiquement la somme exacte à percevoir par l'enseignant sans saisie manuelle.

---
## 🚀 Consigne pour le Refactoring Code
Génère le plan d'action de modification des tables de la base de données, les routes API de contrôle d'accès adaptées, et les modifications d'interface (UI) pour valider l'ensemble de ces règles métiers.

# Refonte complète du moteur de calcul des notes

Je veux que tu analyses l'architecture actuelle du module de gestion des notes avant de modifier quoi que ce soit. Le moteur actuel est devenu difficile à comprendre et à maintenir. Je souhaite le remplacer par un moteur de calcul beaucoup plus simple, flexible et facilement paramétrable.

## Contexte

L'application est destinée à la gestion d'un établissement scolaire.

Le Directeur des Études effectue les opérations suivantes :

* crée les classes ;
* crée les matières ;
* affecte les matières aux classes ;
* affecte les professeurs aux matières et aux classes ;
* génère l'emploi du temps ;
* programme les évaluations de chaque matière.

L'année scolaire comporte **9 mois**.

Au secondaire (Collège et Lycée), elle est découpée en **2 semestres**.

### Premier semestre

* Octobre
* Novembre
* Décembre
* Composition du 1er semestre (generalement en janvier)
* Bulletin

### Deuxième semestre

* Février
* Mars
* Avril
* Composition du 2ème semestre (generalement en Mai)
* Bulletin

Pour chaque matière, chaque mois comporte **2 évaluations** :

* une évaluation orale ;
* une évaluation écrite.

À la fin de chaque semestre, les élèves passent une **composition**.

---

# Calcul des moyennes

Le calcul doit être identique pour le Collège et le Lycée.

La seule différence provient du coefficient de la matière.

---

## Matières de coefficient 1

### Étape 1 : moyenne mensuelle

Pour chaque mois :

```
Moyenne mensuelle =
(Orale + Écrite) / 2
```

Exemple :

Orale = 14

Écrite = 16

```
Moyenne d'octobre = (14 + 16) / 2 = 15
```

---

### Étape 2 : moyenne de cours

```
Moyenne de cours =
(Moyenne Octobre +
 Moyenne Novembre +
 Moyenne Décembre) / 3
```

---

### Étape 3 : moyenne de composition

C'est simplement la note obtenue à la composition.

```
Moyenne composition = Note de composition
```

---

### Étape 4 : moyenne semestrielle

```
Moyenne semestrielle =
(Moyenne de cours + Moyenne composition) / 2
```

---

# Matières de coefficient 2

Les évaluations mensuelles sont pondérées.

Chaque note compte avec le coefficient 2.

### Étape 1 : moyenne mensuelle

```
Moyenne mensuelle =
(Orale × 2 + Écrite × 2) / 4
```

ou plus généralement

```
Somme(notes × coefficient)
/
Somme(coefficients)
```

---

### Étape 2 : moyenne de cours

```
(Moyenne Octobre +
 Moyenne Novembre +
 Moyenne Décembre)
/ 3
```

---

### Étape 3 : moyenne de composition

La composition est également pondérée.

```
Moyenne composition =
Composition × 2
```

---

### Étape 4 : moyenne semestrielle

```
(Moyenne de cours + Moyenne composition)
/ 3
```

Le diviseur est 3 car :

* moyenne de cours = poids 1
* composition = poids 2

Poids total = 3.

---

# Ce que je souhaite

Je ne veux plus d'un moteur codé en dur avec des conditions complexes.

Je veux un moteur générique.

Le calcul doit être piloté par un système de paramétrage.

---

# Proposition de conception

Analyse le code existant puis propose une architecture plus propre.

Je pense qu'il serait préférable de créer une table de paramétrage des calculs.

Par exemple :

```
Règle de calcul

- Niveau
- Semestre
- Matière
- Coefficient
- Type d'évaluation
- Pondération
- Ordre
- Formule
```

ou toute autre structure que tu jugeras plus pertinente.

---

# Paramétrage des évaluations

Je souhaite que le paramétrage fonctionne comme la création des évaluations ou des examens.

Par exemple, lorsqu'on crée une programmation, une ligne est générée automatiquement.

Ensuite on peut choisir :

* semestre ;
* matière ;
* type d'évaluation ;
* coefficient ;
* poids ;
* formule.

Le moteur utilisera ensuite ces paramètres pour effectuer les calculs.

Je veux éviter toute logique codée en dur.

---

# Contraintes

* Ne casse pas les fonctionnalités existantes.
* Le code doit être propre.
* Le code doit être facilement maintenable.
* Les calculs doivent être centralisés.
* Les formules ne doivent pas être dispersées dans plusieurs fichiers.
* Les calculs doivent être réutilisables pour les bulletins, classements et statistiques.

---

# Ce que j'attends de toi

Avant de coder :

1. Analyse complètement le système actuel.
2. Explique ce qui ne va pas.
3. Propose une architecture plus simple.
4. Explique les avantages et les inconvénients.
5. Choisis la meilleure solution.
6. Implémente cette solution.
7. Migre les calculs existants vers ce nouveau moteur.
8. Vérifie que les anciens bulletins donnent les mêmes résultats.
9. Supprime le code devenu inutile.

L'objectif est d'obtenir un moteur de calcul des notes simple, générique, extensible et compréhensible, capable de gérer facilement de nouvelles règles de calcul sans modifier le code métier.
