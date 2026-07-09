# Prompts Frontend — Next.js + Mantine + Tailwind (projet existant)

> À donner à Claude Code dans l'ordre. Rythme : un prompt = un module = un commit = ta validation.
> Stack RÉELLE : Next.js (App Router, TS) + **Mantine UI + Tailwind ensemble** + react-hook-form + zod.
> Data fetching : **non figé** — Claude Code propose (recommandation : TanStack Query) à l'étape F0.
> ⚠️ Le projet front est **DÉJÀ initialisé** et **des écrans métier sont déjà commencés**.
> Donc : NE RIEN réinitialiser, NE RIEN écraser sans audit. Toujours réutiliser/compléter l'existant.
> Le frontend appelle TON backend (/api/v1), jamais Djomy directement.
> Réfs : CLAUDE.md (modèle/archi), features/00-conventions.md (pagination
> { content, page, size, totalElements, totalPages }, erreurs RFC 7807, RBAC par permission),
> et le fichier features/ du module concerné.

---

## F0 — Audit de l'existant (NE PAS coder, juste analyser)

```
Le projet frontend est déjà initialisé (Next.js App Router + Mantine + Tailwind)
et quelques écrans métier sont déjà commencés. NE MODIFIE RIEN pour l'instant.

Explore /frontend et fais-moi un état des lieux :
- versions (Next, React, Mantine, Tailwind) et configuration (app router ?
  structure des dossiers, alias d'import) ;
- comment Mantine et Tailwind coexistent aujourd'hui : ordre des imports CSS,
  preflight Tailwind vs styles Mantine, présence d'un PostcssProvider Mantine,
  conflits de reset CSS éventuels ;
- ce qui existe déjà côté écrans métier (quels modules, quel niveau d'avancement,
  conventions de code et de nommage utilisées) ;
- l'état de l'auth, du routage, et de tout client API / data fetching présent ;
- la gestion du thème (couleur primaire, dark mode), des formulaires, des
  notifications.

Rends-moi : (1) un résumé de l'existant, (2) les conventions que tu vas RÉUTILISER
pour rester cohérent, (3) les problèmes Mantine/Tailwind à régler, (4) ta
recommandation pour le data fetching (TanStack Query vs SWR vs fetch maison) avec
un choix par défaut. Ne code pas encore : attends ma validation du plan.
```

## F0bis — Fondations (sur l'existant, sans casser)

```
Après validation de l'audit F0, mets en place les fondations SANS réécrire les
écrans déjà commencés (intègre-les / refactore a minima pour cohérence) :
- Coexistence Mantine + Tailwind propre : configurer l'ordre des layers CSS pour
  que Tailwind preflight n'écrase pas Mantine (utiliser @layer / l'option
  Tailwind pour désactiver ou isoler preflight si nécessaire), importer le CSS
  Mantine avant les utilitaires Tailwind, et documenter la règle "Mantine pour
  les composants, Tailwind pour le layout/spacing utilitaire" pour éviter les
  doublons ;
- client API centralisé (lib/api) : Authorization Bearer sur chaque requête,
  refresh token auto (401 -> refresh -> retry une fois), base URL
  NEXT_PUBLIC_API_BASE_URL (/api/v1), parsing des erreurs RFC 7807 vers
  @mantine/notifications ;
- data fetching : installe et configure la solution validée en F0 (provider en
  racine, clés de cache, invalidation) ;
- AuthProvider (profil via /auth/me : rôles + permissions + tenant + branding),
  hook usePermissions() + composant <Can permission="..."> , gardes de route ;
- AppShell Mantine (navigation latérale) piloté par les permissions ;
- composant <DataTable> réutilisable aligné sur l'enveloppe de pagination du
  backend (tri, filtres en query params, pagination serveur) ;
- helpers de formatage GNF (BigDecimal string) et dates ISO -> locale ;
- thème (couleur primaire + logo injectés par tenant au login).
Commit "chore(frontend): fondations (api, auth, theme, datatable)". Attends ma
validation.
```

## F1 — Module iam : Auth & RBAC

```
Lis features/02-iam.md. D'abord regarde ce qui existe déjà pour ce module et COMPLÈTE/refactore plutôt que de recréer ; garde les conventions du projet. Implémente le frontend d'authentification et RBAC :
- pages : login (avec branding du tenant : logo + couleur), mot de passe oublié,
  réinitialisation via token, changement de mot de passe (utilisateur connecté) ;
- récupération du profil via /auth/me (rôles + permissions + tenant + branding),
  stocké dans un AuthProvider ; déconnexion (invalidation refresh) ;
- hook usePermissions() + composant <Can permission="eleve:create"> pour
  conditionner l'affichage ; garde de route par rôle/permission ;
- écrans admin : utilisateurs (DataTable paginée + filtres rôle/actif/recherche,
  création/édition, activer/désactiver), rôles (CRUD + affectation de
  permissions), catalogue de permissions (lecture), affectation de rôles à un
  utilisateur ;
- verrouillage après échecs de login affiché proprement ; toutes les
  mutations invalident le cache TanStack Query concerné.
Commit "feat(frontend): iam (auth + RBAC)". Attends ma validation.
```

## F2 — Module academics : calendrier, classes, organisation

```
Lis features/05-academics.md. D'abord regarde ce qui existe déjà pour ce module et COMPLÈTE/refactore plutôt que de recréer ; garde les conventions du projet. Implémente l'UI academics :
- années scolaires : liste, création/édition, changement de statut
  (PLANIFIEE->EN_COURS->CLOTUREE->ARCHIVEE), "définir comme courante" (une seule
  courante : reflète l'exclusivité dans l'UI) ; une année ARCHIVEE s'affiche en
  lecture seule ;
- périodes : CRUD sous une année, action "clôturer" (avertir que ça fige les
  notes et génère les bulletins) ;
- niveaux (ordre), salles (CRUD + disponibilité) ;
- classes : CRUD (niveau, année, prof principal, salle, capacité), affichage
  effectif courant vs capacité, liste des élèves de la classe ;
- matières (coefficient) + affectation matière/enseignant à une classe ;
- emploi du temps : vue calendrier hebdomadaire (Mantine) par classe et par
  enseignant ; à la création/édition d'un créneau, afficher clairement les
  conflits renvoyés par l'API en 409 (salle/enseignant/classe).
Respecte les permissions et l'interdiction d'édition sur année close/archivée.
Commit "feat(frontend): academics". Attends ma validation.
```

## F3 — Module students : élèves & parents

```
Lis features/03-students.md. D'abord regarde ce qui existe déjà pour ce module et COMPLÈTE/refactore plutôt que de recréer ; garde les conventions du projet. Implémente l'UI students :
- élèves : DataTable (filtres classe, statut inscription, sexe, recherche
  nom/matricule), fiche complète (infos, photo uploadable, documents, parents,
  historique d'inscriptions), création/édition ; afficher un message clair si la
  création est refusée pour quota dépassé (403) ;
- documents de l'élève : upload (type acte de naissance/photo/certificat/
  historique), liste, suppression, avec validation type MIME/taille côté UI ;
- inscriptions/réinscriptions : créer une inscription (classe + année), valider/
  annuler, refus si classe pleine ; bouton de réinscription (nouvelle année) ;
- parents : CRUD, liaison à un/plusieurs élèves (lien PERE/MERE/TUTEUR), vue des
  enfants rattachés.
Commit "feat(frontend): students". Attends ma validation.
```

## F4 — Module staff : enseignants

```
Lis features/04-staff.md. D'abord regarde ce qui existe déjà pour ce module et COMPLÈTE/refactore plutôt que de recréer ; garde les conventions du projet. Implémente l'UI staff :
- enseignants : DataTable (recherche, spécialité), fiche (infos, matricule,
  date d'embauche), création/édition ;
- vues liées : classes dont il est responsable, ses affectations matières
  (ClasseMatiere), son emploi du temps consolidé ;
- la suppression d'un enseignant lié à des notes/évaluations est interdite :
  proposer la désactivation à la place et expliquer le refus de l'API.
Commit "feat(frontend): staff". Attends ma validation.
```

## F5 — Module attendance : présences

```
Lis features/06-attendance.md. D'abord regarde ce qui existe déjà pour ce module et COMPLÈTE/refactore plutôt que de recréer ; garde les conventions du projet. Implémente l'UI attendance :
- appel de classe : écran de saisie en lot pour une classe + date (liste des
  élèves, statut PRESENT/ABSENT/RETARD, motif), enregistrement groupé ;
- saisie/correction unitaire ; historique filtrable (élève, classe, date,
  statut) ;
- statistiques d'assiduité par élève et par période (taux, graphes Mantine) ;
- présences enseignants : écran séparé (saisie + stats), bien distinct des
  présences élèves.
Indiquer quand un parent a été notifié (notifie_parent).
Commit "feat(frontend): attendance". Attends ma validation.
```

## F6 — Module grading : notes, examens, bulletins

```
Lis features/07-grading.md. D'abord regarde ce qui existe déjà pour ce module et COMPLÈTE/refactore plutôt que de recréer ; garde les conventions du projet. Implémente l'UI grading :
- évaluations : CRUD (classe, matière, période, type, note_sur, coefficient) ;
  bloquer/avertir si la période est clôturée ; restreindre aux classes/matières
  de l'enseignant connecté ;
- saisie des notes : grille classe x élèves pour une évaluation, saisie en lot,
  validation (0 <= note <= note_sur) avec erreurs inline ; action "valider" qui
  verrouille ; correction soumise aux règles (période ouverte) ;
- examens : planification (classe, période, type, date), changement de statut
  (PLANIFIE->EN_COURS->TERMINE), lien vers la convocation PDF ;
- bulletins : génération en lot (classe + période), liste/détail (en-tête +
  lignes par matière : moyenne, coefficient, moyenne pondérée, rang matière,
  appréciation ; moyenne générale, rang, mention, décision, stats de classe),
  bouton "publier", lien PDF ; vue élève/parent en lecture seule des bulletins
  publiés.
Commit "feat(frontend): grading". Attends ma validation.
```

## F7 — Module billing : frais & factures

```
Lis features/08-billing.md. D'abord regarde ce qui existe déjà pour ce module et COMPLÈTE/refactore plutôt que de recréer ; garde les conventions du projet. Implémente l'UI billing :
- frais scolaires : CRUD (classe, type INSCRIPTION/MENSUALITE/TRANSPORT/...,
  montant, année) ;
- échéances : CRUD sous un frais, avertir si la somme des échéances ne
  correspond pas au montant total (422) ;
- factures : émission unitaire et génération en lot (par classe), DataTable
  (filtres élève/classe/statut/période), détail avec montant total/payé/solde et
  statut (IMPAYEE/PARTIELLE/PAYEE), lien PDF ;
- situation financière par élève (toutes ses factures + soldes).
Le paiement d'une facture est traité dans le prompt F9 (parcours Djomy).
Commit "feat(frontend): billing". Attends ma validation.
```

## F8 — Module communication : SMS / Email / WhatsApp

```
Lis features/11-communication.md. D'abord regarde ce qui existe déjà pour ce module et COMPLÈTE/refactore plutôt que de recréer ; garde les conventions du projet. Implémente l'UI communication :
- composer un message : choix du canal (SMS/EMAIL/WHATSAPP), du type de
  destinataire (parent/élève/enseignant/groupe), sujet, contenu, motif ;
- diffusion de masse : cibler une classe, un niveau ou un groupe ;
- historique : DataTable (filtres canal/motif/date/expéditeur), détail avec
  statut de livraison par destinataire (EN_ATTENTE/LIVRE/ECHOUE, lu) ;
- si la fonctionnalité Templates (bonus) est implémentée côté backend, ajouter
  un écran de gestion des modèles de message avec variables.
Commit "feat(frontend): communication". Attends ma validation.
```

## F9 — Parcours de paiement Djomy (scolaire + abonnement)

```
Lis features/09-payments.md et features/10-webhooks.md. Implémente le parcours
de paiement (frais scolaires ET factures d'abonnement). Le frontend NE parle PAS
à Djomy directement.

Flux gateway Djomy :
1. depuis une facture (ou facture d'abonnement) à régler : sélection, saisie du
   numéro de téléphone du payeur (OBLIGATOIRE), montant (rappel sandbox :
   <= 10 000 GNF pour Soutra/PayCard) et choix du fournisseur DJOMY ;
2. POST backend (/api/v1/paiements ou /api/v1/abonnements/paiements) -> reçoit
   un redirectUrl ;
3. redirection immédiate du payeur vers redirectUrl (portail Djomy) ;
4. page de retour (returnUrl) "vérification en cours" : NE PAS se fier au retour ;
   poll le statut du paiement côté backend jusqu'à REUSSI/ECHOUE, puis afficher
   le résultat + lien reçu PDF ;
5. composant de statut clair pour INITIE/EN_ATTENTE/REUSSI/ECHOUE/REMBOURSE ;
   action de remboursement réservée au rôle COMPTABLE.
Écran historique des paiements (filtres canal/statut/date) + accès aux reçus et
aux transactions (rôle comptable/admin).
Commit "feat(frontend): parcours paiement Djomy". Attends ma validation.
```

## F10 — Module webhooks : intégrations (UI)

```
Lis features/10-webhooks.md. D'abord regarde ce qui existe déjà pour ce module et COMPLÈTE/refactore plutôt que de recréer ; garde les conventions du projet. Implémente l'UI de gestion des intégrations :
- webhook entrant Djomy : écran d'information affichant l'URL de callback à
  enregistrer dans l'espace développeur Djomy, avec rappel explicite "HTTPS
  obligatoire ; en local, utiliser un tunnel type ngrok ; sans enregistrement
  de l'URL, aucun événement n'est reçu" ;
- webhooks sortants : CRUD des WebhookEndpoint (url, secret, types d'événements
  abonnés, actif/inactif) ; journal des WebhookEvent (filtres statut/endpoint,
  nb_tentatives, code réponse) avec action "rejouer".
Réservé aux rôles admin. Commit "feat(frontend): intégrations webhooks".
Attends ma validation.
```

## F11 — Espace plateforme (super-admin)

```
Lis features/01-platform.md. D'abord regarde ce qui existe déjà pour ce module et COMPLÈTE/refactore plutôt que de recréer ; garde les conventions du projet. Implémente l'espace plateforme (rôle SUPER_ADMIN),
séparé de l'espace école (app/(platform)) :
- tenants : onboarding/création, DataTable (filtres statut/ville/plan), détail
  avec quota consommé vs limite du plan, actions activer/suspendre/archiver
  (la suspension bloque l'accès du tenant) ;
- plans : CRUD (Starter/Standard/Premium, limites, prix mensuel/annuel) +
  archivage ;
- abonnements : liste (filtres statut/plan/échéance), changement de plan
  (upgrade/downgrade avec affichage du prorata), suspension/annulation, toggle
  auto-renouvellement ;
- factures d'abonnement (liste, PDF, relance) et paiements d'abonnement
  (parcours F9) + reçus.
Commit "feat(frontend): espace plateforme". Attends ma validation.
```

## F12 — Dashboards par rôle

```
Lis features/12-transverse.md (§12). Implémente les dashboards, chacun filtré
par rôle/tenant/portée, avec cartes de stats et graphes Mantine :
- plateforme (super-admin) : tenants actifs/suspendus, MRR/ARR, répartition par
  plan, churn, abonnements à échéance ;
- école (admin/directeur) : effectifs par niveau/classe, taux d'assiduité, taux
  de recouvrement (payé vs attendu), bulletins publiés, alertes (impayés, quota
  proche, échéances) ;
- comptable : encaissements par canal/période, soldes restants, top débiteurs ;
- enseignant : mes classes, prochains cours, évaluations à corriger, taux de
  saisie ;
- parent/élève : enfants, dernières notes, absences, factures impayées,
  prochains examens.
Commit "feat(frontend): dashboards". Attends ma validation.
```

## F13 — Transverse : recherche, paramètres, imports/exports, i18n

```
Lis features/12-transverse.md (§14, §15). Implémente :
- recherche globale (/api/v1/recherche) avec résultats groupés par type
  (élèves, parents, enseignants, classes, factures), filtrée par permissions ;
- paramètres du tenant : barème de mentions, format de matricule, devise,
  fuseau horaire, délai de grâce abonnement, horaires d'ouverture, politique de
  mot de passe ;
- imports en masse (élèves, notes, enseignants) : upload CSV/Excel, écran de
  validation (dry-run) affichant les lignes invalides avant commit, suivi du
  traitement asynchrone + rapport téléchargeable ;
- exports (élèves, notes, paiements, présences, archive ZIP des bulletins) :
  boutons de téléchargement avec filtres ;
- i18n : FR par défaut, EN optionnel, via Accept-Language ; sélecteur de langue.
Commit "feat(frontend): recherche + paramètres + imports/exports + i18n".
Attends ma validation.
```

---

## Ordre conseillé

F0 -> F1 -> F2 -> F3 -> F4 -> F5 -> F6 -> F7 -> F8 -> F9 -> F10 -> F11 -> F12 -> F13.

Le backend du module correspondant doit être prêt (et validé) avant son écran
frontend. Pour le paiement (F9), il faut que les prompts backend Djomy A1 et A2
soient validés et que l'URL du webhook soit enregistrée dans l'espace
développeur Djomy.

## Coexistence Mantine + Tailwind (règle du projet)
- Mantine = composants (inputs, modals, tables, notifications, AppShell).
- Tailwind = layout, spacing, petits utilitaires. Éviter de styliser deux fois
  le même élément. Importer le CSS Mantine AVANT les utilitaires Tailwind et
  isoler le preflight Tailwind pour ne pas casser les composants Mantine.

## Conventions à rappeler à chaque module
- Toutes les listes utilisent la pagination serveur { content, page, size,
  totalElements, totalPages } et des filtres en query params.
- Les erreurs API sont au format RFC 7807 -> notifications Mantine lisibles.
- L'affichage est conditionné par les permissions (<Can> / usePermissions()).
- Montants BigDecimal (string) formatés en GNF ; dates ISO formatées en locale.
- Mutations TanStack Query -> invalidation du cache concerné.
- Si un point est ambigu, demande avant de coder. Ne saute aucun écran.
