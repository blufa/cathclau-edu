# SCHOOL SaaS — Prompt FRONT-END complet (Next.js + Tailwind + Mantine)

> **Périmètre de ce prompt** : tout le front des modules **`platform`** (inscription école, cycle de vie, structures, quota élèves) et **`billing-subscription`** (plans, abonnement, factures, paiements, reçus), **plus** la **home publique** (vitrine + grille tarifaire) et le **dashboard** post-connexion.
> Back-end de référence : voir prompts `platform` et `billing-subscription`. IAM externe (auth/rôles). Ne jamais mélanger l'abonnement plateforme (l'école paie l'éditeur) avec les frais scolaires (hors de ce périmètre).
>
> À coller dans Claude Code après le module shared/bootstrap front. Implémente **TOUS** les écrans décrits, branchés à l'OpenAPI.

---

## 0. Stack & conventions front (rappel)

```
Crée/complète l'app Next.js (App Router, TypeScript) "school-web" avec :
- Tailwind CSS (layout, utilitaires) + Mantine (@mantine/core, hooks, form, dates, notifications,
  charts, modals, nprogress, spotlight). MantineProvider + ColorSchemeScript dans le layout racine,
  thème de marque (couleurs primaires école, rayon md, police Inter).
- TanStack Query pour toute la donnée serveur (cache, invalidation, optimistic updates).
- Client API typé GÉNÉRÉ depuis l'OpenAPI du back (openapi-typescript ou orval) ; script npm "gen:api".
  Toujours passer par ce client typé, jamais de fetch nu.
- Auth OIDC contre l'IAM externe (login/refresh silencieux, Bearer sur tous les appels).
- zod pour la validation des formulaires (@mantine/form + zodResolver).
- Gestion centralisée des erreurs HTTP -> toasts clairs : 400 (validation), 401 (déconnexion),
  403 (droits/quota), 404, 409 (conflit), 422 (transition illégale), 429 (rate limit).
- i18n (next-intl ou équivalent) : FR par défaut, structure prête pour EN/PT/ES/AR/ZH (la note de
  cadrage exige le multilingue). Tous les libellés via clés de traduction.
- Layout responsive (mobile-first), accessibilité (focus, aria), états loading/empty/error partout.

ARBORESCENCE (App Router) :
  app/(public)/                 -> site vitrine + tarifs + signup + verify-email (sans auth)
  app/(auth)/login              -> redirection IAM
  app/(app)/                    -> espace connecté (AppShell), garde d'auth
  app/(platform-admin)/         -> espace SUPER_ADMIN éditeur (garde de rôle)
  components/  hooks/  lib/  api/  i18n/  styles/
```

---

## 1. HOME PUBLIQUE (vitrine SaaS + grille tarifaire) — `app/(public)`

```
Construis le site public (sans authentification), responsive, soigné, orienté conversion.

PAGES & SECTIONS :
1. Landing `/` :
   - Header public : logo, nav (Fonctionnalités, Tarifs, Connexion, "Essai gratuit"), sélecteur de langue.
   - Hero : titre accrocheur (gestion scolaire SaaS), sous-titre, 2 CTA ("Démarrer l'essai gratuit",
     "Voir les tarifs"), visuel/illustration.
   - Section bénéfices (cards Mantine) : gestion des élèves, scolarité, communication parents,
     facturation des frais, multi-établissements.
   - Section "Comment ça marche" (3 étapes : s'inscrire -> vérifier l'email -> gérer son école).
   - Bandeau confiance (logos, témoignages factices placeholder).
   - Footer (liens légaux, contact, langues).

2. Page Tarifs `/tarifs` (CRUCIAL — grille des plans d'abonnement) :
   - Récupère les plans via GET /api/v1/platform/plans (endpoint public).
   - Affiche les plans STARTER / STANDARD / PREMIUM en cards comparatives :
     prix + périodicité (toggle MENSUEL / ANNUEL / TRIMESTRE_SCOLAIRE avec recalcul du prix affiché),
     limite d'élèves (≤500 / ≤2000 / illimité), liste des fonctionnalités incluses, badge "Populaire"
     sur STANDARD, bouton "Choisir ce plan" -> redirige vers /signup?plan=CODE.
   - Tableau comparatif détaillé (lignes de fonctionnalités, colonnes = plans, ✓/✗).
   - FAQ tarifs (accordéon Mantine) : essai gratuit, changement de plan, moyens de paiement.
   - Mentionne l'essai gratuit de 30 jours sans carte bancaire.

3. Page Fonctionnalités `/fonctionnalites` : détail des modules (scolarité, communication, facturation).

RÈGLES : pages statiques/SSR pour le SEO (metadata, OpenGraph). Les plans sont chargés dynamiquement.
Aucune donnée sensible. Le sélecteur de périodicité recalcule le prix affiché (mensuel vs annuel remisé).
```

---

## 2. INSCRIPTION & VÉRIFICATION EMAIL (parcours self-service) — `app/(public)`

```
Construis le parcours d'inscription d'une école (self-service), branché sur le module platform.

ÉCRANS :
1. `/signup` (peut recevoir ?plan=CODE depuis la page tarifs) :
   - Formulaire (Stepper Mantine 2 étapes ou page unique) :
     Étape "École" : nom_ecole, slug_souhaite (auto-généré depuis le nom, éditable, vérif dispo en live
       via debounce -> indicateur disponible/pris), ville, pays (select), type_etablissement
       (PRIMAIRE|COLLEGE|LYCEE|SUPERIEUR|MIXTE).
     Étape "Directeur" : email_directeur, mot de passe (règles de complexité affichées), confirmation,
       acceptation CGU.
     Récapitulatif du plan choisi (depuis ?plan=) avec possibilité de changer.
   - Validation zod stricte (email, force du mot de passe, slug format).
   - Submit -> POST /api/v1/public/signup -> 202 -> écran de confirmation "Vérifiez votre email"
     (avec l'email masqué + bouton "Renvoyer l'email" appelant /public/resend-verification, throttlé).
   - Gérer 409 (slug ou email déjà pris) avec message précis sur le champ concerné.

2. `/verify-email?token=...` :
   - Au montage : POST /api/v1/public/verify-email { token } (idempotent).
   - États : chargement -> succès ("Votre école est active, vous pouvez vous connecter" + CTA Connexion)
     ; erreur (token expiré -> bouton "Renvoyer un lien") ; déjà vérifié (message neutre).

3. `/resend-verification` : petit formulaire email -> /public/resend-verification.

4. Page de login brandée par école `/{slug}/login` (optionnel) :
   - Charge le branding via GET /api/v1/public/ecoles/{slug}/branding (logo + couleurs) sans auth,
     applique dynamiquement le thème, puis redirige vers l'IAM pour l'authentification.

RÈGLES : tant que l'école est PENDING_VERIFICATION, aucun accès à l'espace connecté. Messages clairs,
pas de jargon. Respecter le throttle du back sur signup/resend (désactiver le bouton + compte à rebours).
```

---

## 3. ESPACE CONNECTÉ — AppShell & navigation — `app/(app)`

```
Construis l'AppShell de l'espace école (après connexion IAM), avec garde d'authentification.

LAYOUT (Mantine AppShell) :
- Navbar latérale : Tableau de bord, Abonnement, Factures, (placeholders désactivés : Élèves, Classes,
  Scolarité, Communication — hors périmètre mais visibles en "à venir").
- Header : nom + logo de l'école, badge de STATUT de l'école (TRIAL avec compte à rebours de fin d'essai,
  ACTIVE, SUSPENDED, EXPIRED), cloche notifications, menu utilisateur (profil, déconnexion), sélecteur
  de langue.
- Bandeau global contextuel selon le statut :
  * TRIAL -> "Essai gratuit : J-{n} restants. Choisissez un plan pour continuer." + CTA "Voir les plans".
  * SUSPENDED -> bandeau rouge "Accès suspendu (paiement en attente)" + CTA "Régulariser".
  * EXPIRED -> bandeau "Essai/abonnement expiré" + CTA "Souscrire".
- Garde de rôle : l'ADMIN_ECOLE voit son école ; les sections /platform-admin sont masquées.

RÈGLES : récupérer le contexte (école + abonnement courant) au chargement via
GET /api/v1/abonnements/courant et un endpoint école. Garder ce contexte en cache (TanStack Query) et le
rafraîchir après toute action d'abonnement/paiement.
```

---

## 4. DASHBOARD (tableau de bord école) — `app/(app)/dashboard`

```
Construis le tableau de bord post-connexion (page d'atterrissage).

CONTENU :
- Cartes KPI : statut de l'abonnement (plan + état + date de fin), élèves consommés vs limite du plan
  (jauge/progress avec alerte si proche de la limite), jours d'essai restants (si TRIAL), prochaine
  échéance de facture, montant dû éventuel.
- Carte "Mon abonnement" : plan courant, périodicité, auto-renouvellement (toggle), bouton "Changer de
  plan" et "Gérer l'abonnement".
- Carte "Dernières factures" : 3-5 dernières (numéro, statut coloré, montant, échéance, lien PDF).
- Widget quota élèves : barre de progression consommé/limite (via abonnement courant) + message si
  dépassement bloqué (403) à prévoir côté création d'élève.
- Raccourcis (cards cliquables) vers Abonnement, Factures, (placeholders modules scolaires).
- Flux de notifications récentes.
- États vides clairs (ex. aucune facture encore émise en période d'essai).

RÈGLES : tout est filtré sur l'école courante. Données via abonnements/courant + factures. Rafraîchir
après paiement. Graphiques avec @mantine/charts si pertinent (évolution des paiements, consommation).
```

---

## 5. ABONNEMENT (ADMIN_ECOLE) — `app/(app)/abonnement`

```
Construis l'écran de gestion de l'abonnement de l'école (vue + actions déléguées à billing-subscription).

ÉCRANS / SECTIONS :
1. Vue d'ensemble (GET /api/v1/abonnements/courant) :
   - Plan courant, statut (badge), date début/fin, auto-renouvellement (toggle ->
     PATCH /api/v1/abonnements/{id}/auto-renouvellement), élèves consommés vs limite.
   - Si TRIAL/EXPIRED : encart "Souscrire à un plan" mis en avant.
   - Si ACTIVE : boutons "Changer de plan", "Annuler l'abonnement" (avec confirmation : fin de période
     vs immédiat).

2. Souscription / changement de plan :
   - Sélecteur de plans (réutilise les cards de la page tarifs, mais en mode connecté) chargés via
     GET /api/v1/platform/plans, avec le plan actuel mis en évidence.
   - UPGRADE : aperçu du prorata (montant_proratise) avant confirmation ; effet immédiat.
   - DOWNGRADE : information "effet au prochain renouvellement" ; si l'école dépasse la nouvelle
     limite_eleves -> afficher l'erreur 409 clairement ("Vous avez X élèves, le plan visé en autorise Y ;
     réduisez les effectifs ou choisissez un plan supérieur").
   - Souscrire : POST /api/v1/platform/abonnements -> crée l'abonnement EN_ATTENTE + facture -> redirige
     vers le paiement (section 7). Changer : POST /api/v1/platform/abonnements/{id}/changer-plan.
   - Historique des changements de plan (PlanChange : type, date, prorata).

3. Annulation : PATCH /api/v1/platform/abonnements/{id}/annuler (option fin de période / immédiat avec
   remboursement prorata) -> confirmation + conséquences expliquées.

RÈGLES : refléter les règles back (activation conditionnée au paiement REUSSI, délai de grâce, etc.).
Après toute action, invalider le cache "abonnement courant" + bandeau de statut. Gérer 422 (transition
illégale) et 409 (downgrade hors-limite) avec messages explicites.
```

---

## 6. FACTURES D'ABONNEMENT — `app/(app)/factures`

```
Construis la gestion des factures d'abonnement de l'école.

ÉCRANS :
1. Liste (GET /api/v1/abonnements/factures) : DataTable (numéro, période, montant total, statut coloré
   [BROUILLON|EMISE|PAYEE|PARTIELLE|IMPAYEE|ANNULEE], date d'échéance, solde restant), filtres (statut,
   période), tri, pagination. Ligne en retard (IMPAYEE + échéance dépassée) mise en évidence (rouge).
2. Détail facture : récap montants (HT, TVA, total, payé, solde restant), période, plan facturé,
   bouton "Télécharger le PDF" (GET /api/v1/factures-abonnement/{id}/pdf), bouton "Payer maintenant"
   si IMPAYEE/PARTIELLE -> section paiement.
3. Pour une facture impayée : appel à l'action de paiement (section 7).

RÈGLES : le PDF est généré en asynchrone côté back -> si non prêt, afficher "génération en cours" +
réessayer. Montants formatés selon la devise. Statuts traduits + colorés. États vides ("aucune facture
en période d'essai").
```

---

## 7. PAIEMENT DE L'ABONNEMENT & REÇUS — `app/(app)/abonnement/paiement`

```
Construis le parcours de paiement d'un abonnement/facture (passerelles via le module payment).

FLUX :
1. Écran de checkout : récap (plan/facture, montant, devise), choix du moyen de paiement disponible
   (Orange Money, MTN MoMo, Wave, Visa/MasterCard…) selon le pays de l'école. Saisie du téléphone payeur
   pour le mobile money.
2. POST /api/v1/abonnements/paiements -> initie le paiement -> rendre la "nextAction" renvoyée :
   redirection (carte/PSP), affichage d'un code/push USSD (mobile money), QR, ou client secret.
3. Écran de statut : polling de l'état du paiement (GET /api/v1/abonnements/paiements/{id}) jusqu'à l'état
   terminal : REUSSI (-> abonnement ACTIVE, facture PAYEE, affichage du reçu) / ECHOUE (message + réessayer)
   / EN_ATTENTE (continuer le polling, avec timeout).
4. Au succès : proposer le téléchargement du reçu (GET /api/v1/abonnements/recus/{id}/pdf) + retour au
   dashboard, et rafraîchir le statut de l'école (bandeau passe de TRIAL/EXPIRED à ACTIVE).

RÈGLES : idempotence visuelle (empêcher les double-clics, désactiver le bouton pendant l'init). Gérer le
paiement partiel (PARTIELLE -> solde restant affiché). Ne jamais exposer de secret PSP. Messages d'erreur
PSP lisibles. Le succès est piloté par le statut serveur (callback), pas par le retour client.
```

---

## 8. ESPACE SUPER_ADMIN ÉDITEUR — `app/(platform-admin)` (garde de rôle SUPER_ADMIN)

```
Construis l'espace d'administration de l'éditeur (Global ITEC), réservé au SUPER_ADMIN.

ÉCRANS :
1. Liste des écoles (GET /api/v1/platform/tenants) : DataTable (nom, ville, type, plan, statut, élèves
   consommés, date de création), filtres (statut, ville, plan, type), recherche, pagination.
2. Détail école (GET /api/v1/platform/tenants/{id}) : infos, abonnement courant, quota élèves consommé,
   actions : éditer (PUT), changer le statut (PATCH /statut : activer/suspendre/archiver via
   confirmations), supprimer (soft, DELETE).
3. Création manuelle d'une école (POST /api/v1/platform/tenants) : formulaire avec initial_state
   TRIAL ou ACTIVE (si ACTIVE : choisir un plan -> abonnement actif + facture PAYEE type MANUAL).
4. Catalogue des plans (CRUD) : GET/POST/PUT /api/v1/platform/plans + PATCH /{id}/archiver. Formulaire
   plan : nom, code, limite_eleves, prix, devise, périodicité, type, fonctionnalités. Plan archivé =
   non souscriptible mais conservé pour les abonnements en cours.
5. Supervision abonnements (GET /api/v1/platform/abonnements) : tous les abonnements, filtres (statut,
   plan, échéance), actions suspendre/annuler, relancer une facture
   (POST /api/v1/platform/factures-abonnement/{id}/relancer).
6. Factures globales (GET /api/v1/platform/factures-abonnement) : vue consolidée multi-écoles.

RÈGLES : garde de rôle stricte (rediriger si non SUPER_ADMIN). Toutes les transitions de statut passent
par les confirmations et gèrent 422. Les montants/devises formatés. Audit visible si exposé par le back.
```

---

## 9. Composants transverses & qualité

```
- Composants partagés : PlanCard (carte plan réutilisée vitrine + connecté), StatusBadge (mapping
  statut -> couleur/libellé traduit), MoneyText (format devise), DataTable générique (tri/pagination/
  filtres serveur), ConfirmModal, FormDrawer, EmptyState, ErrorState, PageLoader, CountdownTrial,
  QuotaProgress (jauge élèves), PaymentMethodPicker, InvoiceStatusPill.
- États systématiques : loading (skeletons Mantine), empty (illustration + CTA), error (message + retry).
- Accessibilité : labels, focus management dans modals/drawers, contrastes.
- Responsive : navbar repliable en burger sur mobile, tables -> cartes empilées sur petit écran.
- Performance : TanStack Query (staleTime raisonnable), pagination serveur, lazy-load des écrans lourds.
- i18n : toutes les chaînes externalisées (FR par défaut) ; format dates/nombres/devises localisé.
- Tests : quelques tests de composants clés (PlanCard, parcours signup) + e2e léger (signup -> verify ->
  dashboard) si l'outillage est en place.

DÉFINITION DE TERMINÉ :
- [ ] Home publique + page tarifs branchée sur /platform/plans, responsive et SEO.
- [ ] Parcours signup -> verify-email -> login fonctionnel (gère 409, throttle resend).
- [ ] AppShell avec bandeau de statut (TRIAL/SUSPENDED/EXPIRED) et garde d'auth.
- [ ] Dashboard avec KPI abonnement + quota élèves + dernières factures.
- [ ] Abonnement : souscrire / changer de plan (prorata upgrade, downgrade hors-limite 409) / annuler /
      auto-renouvellement.
- [ ] Factures : liste + détail + PDF + payer.
- [ ] Paiement : checkout + nextAction + polling statut + reçu, rafraîchit le statut école.
- [ ] Espace SUPER_ADMIN : écoles (CRUD + statut), plans (CRUD), supervision abonnements + factures.
- [ ] Tous les appels via client OpenAPI typé ; erreurs 400/401/403/409/422/429 gérées en toasts.
- [ ] i18n FR opérationnel, structure prête pour les autres langues.
```

---

## 10. Rappel — NE PAS inclure dans ce front

```
- Les FRAIS SCOLAIRES (parents -> école) : c'est le module billing-school, HORS de ce périmètre.
- La gestion des élèves/classes/notes/scolarité : modules séparés (placeholders "à venir" seulement).
- L'authentification elle-même (gérée par l'IAM externe ; ce front consomme les tokens).
Ne jamais mélanger l'abonnement plateforme (école -> éditeur) avec les frais scolaires (parents -> école).
```
