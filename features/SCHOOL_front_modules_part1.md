# SCHOOL SaaS — Prompts FRONT-END par module (Next.js + Tailwind + Mantine) — Partie 1/2

> Un prompt front complet **par module** back-end. Même socle que le front platform/billing déjà livré :
> Next.js (App Router, TS), Tailwind + Mantine, TanStack Query, client API typé généré depuis l'OpenAPI,
> @mantine/form + zod, gestion centralisée des erreurs (400/401/403/404/409/422/429 en toasts), i18n FR
> (structure multilingue prête), AppShell commun, DataTable générique, états loading/empty/error,
> responsive, accessibilité. IAM externe pour l'auth.
>
> **Conventions transverses à respecter dans chaque module** (rappel) :
> - Tous les appels via le client OpenAPI typé ; jamais de fetch nu.
> - Filtrage implicite par tenant (école courante) côté back ; le front n'envoie pas le tenant_id.
> - Garde de rôle par écran (SUPER_ADMIN, ADMIN_ECOLE, DIRECTEUR, COMPTABLE, SECRETAIRE, ENSEIGNANT,
>   PARENT, ELEVE) : masquer/désactiver ce qui n'est pas autorisé, et gérer le 403.
> - Respecter le statut de l'école (bandeau global) : en SUSPENDED/EXPIRED, bloquer les actions d'écriture.
> - Composants partagés réutilisés : StatusBadge, MoneyText, DataTable, ConfirmModal, FormDrawer,
>   EmptyState, ErrorState, PageLoader, FileUpload, DateRangePicker, EntityPicker (autocomplete serveur).

---

## MODULE `iam` — Utilisateurs, rôles & permissions (RBAC)

```
Construis le front du module "iam" (administration des comptes, rôles et permissions de l'école).
Espace connecté (AppShell), accès ADMIN_ECOLE/DIRECTEUR. L'authentification elle-même est gérée par
l'IAM externe (login/refresh/logout déjà en place) — ici on gère la GESTION des utilisateurs et du RBAC.

ÉCRANS
1. Profil & sécurité (tout utilisateur) — /compte :
   - GET /api/v1/auth/me : afficher profil, rôles, permissions, école.
   - Changer le mot de passe (POST /api/v1/auth/mot-de-passe/changer) : ancien + nouveau + confirmation,
     règles de complexité affichées en live, validation zod.
   - (Le parcours "mot de passe oublié / reset" est sur les pages publiques d'auth, pas ici.)

2. Utilisateurs — /admin/utilisateurs (ADMIN_ECOLE) :
   - DataTable (GET /api/v1/utilisateurs) : nom, email, rôles (badges), statut actif/inactif, dernière
     connexion. Filtres : rôle, actif, recherche nom/email. Pagination/tri serveur.
   - Créer (POST) / éditer (PUT) en FormDrawer : nom, email, rôles (multi-select depuis /roles), actif.
     Email unique par école -> gérer 409 sur le champ email.
   - Activer/désactiver (PATCH /activer|/desactiver) avec ConfirmModal (désactiver révoque les sessions ->
     prévenir l'utilisateur). Soft delete (DELETE) avec confirmation.
   - Affecter/retirer des rôles : POST /api/v1/utilisateurs/{id}/roles, DELETE .../roles/{roleId}.

3. Rôles & permissions — /admin/roles (ADMIN_ECOLE) :
   - Liste des rôles (GET /api/v1/roles) : standards (badge "système", non supprimables) + personnalisés.
   - Créer/éditer/supprimer un rôle personnalisé (POST/PUT/DELETE) ; bloquer la suppression des rôles
     système (masquer l'action).
   - Éditeur de permissions par rôle : matrice ressource × action (catalogue via GET /api/v1/permissions,
     format `ressource:action` ex. eleve:create, note:read, bulletin:publish), cases à cocher groupées
     par ressource. Enregistrer -> POST /api/v1/roles/{id}/permissions (remplace l'ensemble).
   - Afficher quels utilisateurs ont ce rôle.

RÈGLES FRONT
- SUPER_ADMIN n'est pas géré ici (espace éditeur séparé). Ne jamais permettre d'auto-rétrograder le
  dernier ADMIN_ECOLE (garde-fou + message).
- Après modification de rôles/permissions, prévenir que les autorisations sont rafraîchies.
- États vides (aucun rôle personnalisé), loading (skeletons), 403 si non autorisé.
DÉFINITION DE TERMINÉ : profil + changement MDP ; CRUD utilisateurs + activation + rôles ; CRUD rôles
personnalisés + matrice de permissions ; i18n ; gestion 409/403/422.
```

---

## MODULE `students` — Élèves & Parents

```
Construis le front du module "students" (élèves, inscriptions, documents, parents).
Accès ADMIN_ECOLE/DIRECTEUR/SECRETAIRE (écriture), ENSEIGNANT (lecture), PARENT/ELEVE (leur fiche).

ÉCRANS
1. Liste des élèves — /eleves :
   - DataTable (GET /api/v1/eleves) : photo (avatar), matricule, nom/prénom, classe courante, sexe,
     statut d'inscription. Filtres : classe, statut inscription, sexe, recherche nom/matricule.
   - Bouton "Nouvel élève" -> formulaire. IMPORTANT : la création peut renvoyer 403 quota élèves
     ({resource:"ELEVES", limite, consomme}) -> afficher un message clair "Limite de votre plan atteinte
     (X/Y élèves). Passez à un plan supérieur." + lien vers /abonnement.
   - Export (optionnel).

2. Création / édition élève — /eleves/nouveau, /eleves/{id}/editer :
   - Formulaire (Stepper ou sections) : identité (nom, prénom, sexe, date/lieu naissance), matricule
     (auto-généré, lecture seule ou éditable selon paramétrage), coordonnées, classe d'inscription.
     Validation zod.
   - Upload photo (PATCH /api/v1/eleves/{id}/photo) avec preview.
    
3. Fiche élève — /eleves/{id} (onglets) :
   - Onglet Infos : identité complète + photo.
   - Onglet Inscriptions : historique (GET /api/v1/inscriptions?eleve=) ; inscrire/réinscrire ; valider/
     annuler (PATCH). Affiche classe + année + statut (EN_ATTENTE/VALIDEE/ANNULEE).
   - Onglet Parents : parents liés (PERE/MERE/TUTEUR), lier (POST /api/v1/eleves/{id}/parents) / délier.
   - Onglet Documents : liste + upload (acte naissance, photo, certificat médical, historique) avec
     type + validation MIME/taille ; suppression. (POST/GET/DELETE /api/v1/eleves/{id}/documents).
   - Onglet Scolarité (liens vers notes, bulletins, livret, factures de l'élève — vers autres modules).
   - Suppression : soft delete, INTERDITE si notes/paiements existent -> proposer "Archiver" à la place
     (gérer le message du back).

4. Inscriptions (vue transverse) — /inscriptions :
   - DataTable (filtres élève, classe, année, statut), actions valider/annuler en masse,
     réinscription groupée (POST /api/v1/inscriptions/reinscription) pour passage d'année.
   - Refus si classe pleine (capacite_max) -> message ; une seule inscription VALIDEE par élève/année.

5. Parents — /parents :
   - DataTable (GET /api/v1/parents) : nom, téléphone, email, nb d'enfants. CRUD.
   - Fiche parent : enfants rattachés (GET /api/v1/parents/{id}/enfants), préférences de communication
     (lien vers module communication), accès portail (compte PARENT optionnel).

RÈGLES FRONT
- Le quota élèves est la contrainte clé : afficher en permanence (sur la liste) un compteur consommé/limite
  (depuis l'abonnement courant) et anticiper le blocage.
- PARENT/ELEVE ne voient que leur périmètre. Tout filtré par école.
DÉFINITION DE TERMINÉ : CRUD élèves + photo + quota 403 géré ; inscriptions/réinscriptions + validations ;
documents ; parents + liens de parenté ; fiche élève à onglets ; i18n ; états vides/erreurs.
```

---

## MODULE `staff` — Enseignants & Personnel

```
Construis le front du module "staff" (enseignants et personnel).
Accès ADMIN_ECOLE/DIRECTEUR (écriture), lecture élargie au staff.

ÉCRANS
1. Liste des enseignants — /enseignants :
   - DataTable (GET /api/v1/enseignants) : matricule, nom, spécialité, nb de classes, nb de matières,
     statut. Filtres : spécialité, actif, recherche. CRUD (POST/PUT) en drawer.
2. Fiche enseignant — /enseignants/{id} (onglets) :
   - Infos : identité, spécialité, coordonnées, compte utilisateur lié (rôle ENSEIGNANT, optionnel).
   - Classes : dont il est responsable (GET /api/v1/enseignants/{id}/classes).
   - Matières : affectations ClasseMatiere (GET .../matieres).
   - Emploi du temps : planning consolidé (GET .../emploi-du-temps) en vue calendrier hebdomadaire.
   - Présences : statistiques (lien module attendance).
3. Suppression : interdite si lié à des notes/évaluations -> proposer la désactivation (gérer le message).

RÈGLES FRONT : spécialité informative (pas de blocage d'affectation). Compte utilisateur lié géré via iam.
DÉFINITION DE TERMINÉ : CRUD enseignants + désactivation ; fiche à onglets (classes/matières/EDT) ; i18n.
```

---

## MODULE `academics` — Années, périodes, niveaux, salles, classes, matières, emploi du temps

```
Construis le front du module "academics" (organisation scolaire). Accès ADMIN_ECOLE/DIRECTEUR/SECRETAIRE.

ÉCRANS
1. Années scolaires — /academique/annees :
   - Liste (GET) avec badge "courante" et statut (PLANIFIEE/EN_COURS/CLOTUREE/ARCHIVEE).
   - Créer/éditer ; définir courante (PATCH /definir-courante) avec ConfirmModal (bascule l'ancienne) ;
     changer statut (PATCH /statut) en respectant les transitions (clôture interdite s'il reste des
     périodes non clôturées -> message). Archivage -> prévenir lecture seule définitive.
2. Périodes — sous l'année /academique/annees/{id}/periodes :
   - Liste (trimestres/semestres, ordre, dates) ; créer/éditer ; clôturer (PATCH /cloturer) avec
     avertissement : la clôture FIGE les notes et génère les bulletins. États clôturés en lecture seule.
3. Niveaux & salles — /academique/niveaux, /academique/salles :
   - Niveaux : CRUD + ordre (drag-to-reorder) qui définit la progression.
   - Salles : CRUD (libellé, bâtiment, capacité, type), disponibilité (PATCH /disponibilite).
4. Classes — /academique/classes :
   - DataTable (GET) : nom, niveau, prof principal, salle, effectif/capacité (jauge), année.
   - Créer/éditer (niveau, année, enseignant resp., salle, capacité) — interdit si année CLOTUREE/ARCHIVEE.
   - Fiche classe (onglets) : élèves (via inscriptions validées, GET /classes/{id}/eleves), effectif
     (GET /classes/{id}/effectif), matières affectées, emploi du temps.
5. Matières & affectations — /academique/matieres :
   - CRUD matières (libellé, coefficient par défaut).
   - Sur la fiche classe : affecter matière + enseignant (POST /classes/{classeId}/matieres),
     AJUSTER LE COEFFICIENT de la matière POUR CETTE CLASSE (PUT .../matieres/{id}/coefficient) — champ
     clé pour la notation paramétrable. Retirer une affectation.
6. Emploi du temps — /academique/emploi-du-temps :
   - Vue calendrier hebdomadaire (grille jours × créneaux), filtres par classe / enseignant / salle.
   - Créer un créneau (classe, matière, enseignant, salle, jour, heure début/fin) en drag ou formulaire.
   - DÉTECTION DE CONFLITS : le back renvoie 409 (même enseignant/salle/classe sur créneaux qui se
     chevauchent) -> afficher clairement le conflit (quoi/qui/quand) et empêcher l'enregistrement.
   - Éditer/supprimer un créneau. Vérifier heure_debut < heure_fin côté front aussi.

RÈGLES FRONT : respecter les transitions de statut (désactiver les actions impossibles). Tout est rattaché
à l'année courante par défaut. Coefficient par classe = point d'entrée de la notation paramétrable.
DÉFINITION DE TERMINÉ : années (statuts + courante) ; périodes (clôture) ; niveaux/salles ; classes +
effectif ; matières + coefficient par classe ; emploi du temps avec gestion des conflits 409 ; i18n.
```

---

## MODULE `attendance` — Présences (élèves & enseignants)

```
Construis le front du module "attendance" (présences). Accès ENSEIGNANT (appel de classe),
ADMIN_ECOLE/DIRECTEUR (suivi), PARENT (consultation de son enfant).

ÉCRANS
1. Appel de classe — /presences/appel :
   - Sélection classe + date (défaut aujourd'hui). Charge la liste des élèves de la classe.
   - Saisie rapide en masse : pour chaque élève, statut PRESENT/ABSENT/RETARD + motif optionnel.
     Boutons "Tout présent" puis ajustements. Enregistrer -> POST /api/v1/presences-eleves/lot.
   - Une présence par élève/classe/date (le back gère l'unicité ; permettre la correction).
2. Suivi des présences élèves — /presences/eleves :
   - DataTable (GET /api/v1/presences-eleves) : filtres élève, classe, date, statut. Correction (PUT).
   - Statistiques par élève (GET /api/v1/eleves/{id}/presences/statistiques) : taux d'assiduité par
     période, graphique. Indicateur "parent notifié" (notifie_parent).
3. Présences enseignants — /presences/enseignants (ADMIN/DIRECTEUR) :
   - Saisie/suivi (POST/GET/PUT) PRESENT/ABSENT, table SÉPARÉE des élèves. Statistiques par enseignant.
4. Vue parent — sur le portail parent : présences/absences de son enfant + taux d'assiduité.

RÈGLES FRONT : une absence/retard déclenche côté back une notification au parent (module communication) ;
afficher l'état "notifié". Optimiser la saisie en masse (clavier, raccourcis). États vides/erreurs.
DÉFINITION DE TERMINÉ : appel de classe en masse ; suivi + correction ; stats élève (graphique) ;
présences enseignants séparées ; vue parent ; i18n.
```

> Suite (grading, billing, payments, webhooks, report-card, communication, expenses) dans la partie 2.
