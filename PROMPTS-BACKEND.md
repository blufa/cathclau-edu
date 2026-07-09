# Prompts Backend — Plateforme SaaS scolaire (Spring Modulith)

> À donner à Claude Code dans l'ordre. Rythme : un prompt = un module = un commit = ta validation.
> Réfs : CLAUDE.md (modèle de données + architecture Spring Modulith / hexagonale),
> features/00-conventions.md (format API, pagination, RFC 7807, RBAC), et le fichier
> features/ du module concerné. Le contrat d'événements inter-modules est en
> features/13-bonus-checklist-events.md §18.
>
> ⚠️ Djomy (agrégateur de paiement) est intégré DIRECTEMENT dans les prompts P9 (payments)
> et P10 (webhooks) ci-dessous — pas de fichier séparé.
>
> ⚠️ Sécurité : les identifiants Djomy vivent UNIQUEMENT dans un .env non commité
> (ajouté à .gitignore). Jamais en clair dans le code ni dans Git.
> ```
> DJOMY_CLIENT_ID=...
> DJOMY_CLIENT_SECRET=...
> DJOMY_BASE_URL=https://sandbox-api.djomy.africa
> DJOMY_WEBHOOK_URL=https://<tunnel-ou-domaine>/api/v1/webhooks/paiement/djomy
> ```

---

## P0 — Bootstrap du projet (une seule fois)

```
Lis CLAUDE.md, features/README.md et features/00-conventions.md.
Confirme que tu as compris l'architecture (Spring Modulith + hexagonale :
domaine pur, ports in/out, adapters in/web + out/persistence + out/gateway,
package-info @ApplicationModule), le multi-tenant (TenantContext + filtre
Hibernate sur tenant_id) et les conventions API (préfixe /api/v1, pagination,
RFC 7807, RBAC par permission).
Mets en place le squelette : Spring Boot 4 (Java 21), dépendances (web,
data-jpa, security, validation, spring-modulith, PostgreSQL, Flyway,
Testcontainers, MapStruct, Lombok), classe Application, test ModularityTests
(ApplicationModules.verify() + Documenter), config Flyway/PostgreSQL.
Ne crée AUCUN module métier. Commit "chore: bootstrap projet Spring Modulith",
puis attends ma validation.
```

## P1 — Module iam (socle multi-tenant + RBAC)

```
Lis CLAUDE.md, features/00-conventions.md et features/02-iam.md.
Implémente la TOTALITÉ de features/02-iam.md : tous les endpoints, règles
métier et événements (cf. §18 dans features/13-bonus-checklist-events.md).
Implémente ici le socle multi-tenant : TenantContext (ThreadLocal) + filtre qui
lit le tenant depuis le JWT + filtre Hibernate global sur tenant_id. Auth
JWT/refresh/reset, utilisateurs, rôles, permissions, RBAC.
Architecture du CLAUDE.md (domaine pur + ports + adapters + package-info
@ApplicationModule). Migration Flyway. Tests : domaine (purs), application
(ports mockés), adapters (@DataJpaTest, @WebMvcTest, Testcontainers).
À la fin : ApplicationModules.of(Application.class).verify(), compile + tests,
commit "feat(iam): implémentation complète", récap + checklist, puis ATTENDS ma
validation. Si ambigu, demande. Ne saute rien.
```

## P2 — Module platform (tenants, plans, abonnements)

```
Lis CLAUDE.md, features/00-conventions.md et features/01-platform.md.
Implémente la TOTALITÉ de features/01-platform.md : tenants, plans, abonnements,
FactureAbonnement, PaiementAbonnement, RecuAbonnement, ChangementPlan,
QuotaService, dashboard plateforme — endpoints, règles, événements (§18).
Respecte les machines à états de l'abonnement (EN_ATTENTE → ACTIF →
SUSPENDU/EXPIRE/ANNULE) et le calcul de prorata. Ne mélange JAMAIS ce flux
(abonnement SaaS) avec les frais scolaires.
Architecture du CLAUDE.md, multi-tenant, Flyway, tests (domaine/application/
adapters + Testcontainers).
À la fin : verify(), compile + tests, commit "feat(platform): implémentation
complète", récap + checklist, puis ATTENDS ma validation. Si ambigu, demande.
```

## P3 — Module academics (calendrier, classes, organisation)

```
Lis CLAUDE.md, features/00-conventions.md et features/05-academics.md.
Implémente la TOTALITÉ de features/05-academics.md : années scolaires, périodes,
niveaux, salles, classes, matières, ClasseMatiere, emploi du temps — endpoints,
règles, événements (§18).
Points critiques : une seule AnneeScolaire courante par tenant ; la clôture
d'une période émet PeriodeClôturee ; détection des conflits salle/enseignant/
classe sur l'emploi du temps (409) ; effectif de classe recalculé ; archivage =
lecture seule.
Architecture du CLAUDE.md, multi-tenant, Flyway, tests.
À la fin : verify(), compile + tests, commit "feat(academics): implémentation
complète", récap + checklist, puis ATTENDS ma validation. Si ambigu, demande.
```

## P4 — Module students (élèves & parents)

```
Lis CLAUDE.md, features/00-conventions.md et features/03-students.md.
Implémente la TOTALITÉ de features/03-students.md : élèves, inscriptions/
réinscriptions, documents, parents, liens de parenté — endpoints, règles,
événements (§18).
Points critiques : appeler QuotaService (module platform) avant toute création
d'élève (403 si quota dépassé) ; matricule unique par tenant ; InscriptionValidee
déclenche la facture d'inscription (billing) et la maj d'effectif (academics).
Architecture du CLAUDE.md, multi-tenant, Flyway, tests.
À la fin : verify(), compile + tests, commit "feat(students): implémentation
complète", récap + checklist, puis ATTENDS ma validation. Si ambigu, demande.
```

## P5 — Module staff (enseignants)

```
Lis CLAUDE.md, features/00-conventions.md et features/04-staff.md.
Implémente la TOTALITÉ de features/04-staff.md : enseignants + vues classes/
matières/emploi du temps — endpoints, règles, événements (§18).
Matricule unique par tenant ; suppression interdite si lié à des notes/
évaluations (désactivation à la place).
Architecture du CLAUDE.md, multi-tenant, Flyway, tests.
À la fin : verify(), compile + tests, commit "feat(staff): implémentation
complète", récap + checklist, puis ATTENDS ma validation. Si ambigu, demande.
```

## P6 — Module attendance (présences)

```
Lis CLAUDE.md, features/00-conventions.md et features/06-attendance.md.
Implémente la TOTALITÉ de features/06-attendance.md : présences élèves (unitaire
+ lot/appel de classe + stats) et présences enseignants (table SÉPARÉE) —
endpoints, règles, événements (§18).
AbsenceEnregistree notifie le(s) parent(s) via communication et positionne
notifie_parent. Unicité présence par élève/classe/date.
Architecture du CLAUDE.md, multi-tenant, Flyway, tests.
À la fin : verify(), compile + tests, commit "feat(attendance): implémentation
complète", récap + checklist, puis ATTENDS ma validation. Si ambigu, demande.
```

## P7 — Module grading (notes, examens, bulletins)

```
Lis CLAUDE.md, features/00-conventions.md et features/07-grading.md.
Implémente la TOTALITÉ de features/07-grading.md : évaluations, saisie/validation
des notes, examens, bulletins, LigneBulletin — endpoints, règles, événements (§18).
Points critiques : saisie de notes seulement sur période non clôturée d'une année
EN_COURS ; calcul exact des moyennes (pondération par coefficient d'évaluation
puis par coefficient de matière), rang par classe et par période, mention/décision
dérivées ; PeriodeClôturee (reçu d'academics) déclenche la génération des
bulletins ; BulletinPublie notifie le parent ; PDF asynchrone.
Architecture du CLAUDE.md, multi-tenant, Flyway, tests.
À la fin : verify(), compile + tests, commit "feat(grading): implémentation
complète", récap + checklist, puis ATTENDS ma validation. Si ambigu, demande.
```

## P8 — Module billing (frais & factures)

```
Lis CLAUDE.md, features/00-conventions.md et features/08-billing.md.
Implémente la TOTALITÉ de features/08-billing.md : frais scolaires, échéances,
factures — endpoints, règles, événements (§18).
Points critiques : flux financier école↔parents, distinct de l'abonnement SaaS ;
somme des échéances = montant du frais ; statut facture dérivé (IMPAYEE/PARTIELLE/
PAYEE), solde recalculé ; génération auto sur InscriptionValidee ; EcheanceDepassee
(job) relance le parent.
Architecture du CLAUDE.md, multi-tenant, Flyway, tests.
À la fin : verify(), compile + tests, commit "feat(billing): implémentation
complète", récap + checklist, puis ATTENDS ma validation. Si ambigu, demande.
```

## P9 — Module payments (paiements, reçus, + FOURNISSEUR DJOMY)

```
Lis CLAUDE.md, features/00-conventions.md et features/09-payments.md.
Implémente la TOTALITÉ de features/09-payments.md : paiements, transactions
fournisseurs, reçus, remboursements — endpoints, règles, événements (§18).
Points critiques : machine à états INITIE → EN_ATTENTE → REUSSI/ECHOUE/REMBOURSE ;
adaptateurs hexagonaux distincts derrière le port FournisseurPaiementGateway ;
PaiementReussi génère le Recu, met à jour la Facture (billing) et notifie
(communication). TransactionPaiement trace chaque appel sortant.

>>> INTÈGRE ICI LE FOURNISSEUR DJOMY (agrégateur), à AJOUTER à côté des autres
    (Orange/MTN/Wave/Stripe) sans en retirer aucun. Il sert pour les DEUX flux :
    paiements scolaires (ce module) ET abonnements SaaS (module platform,
    PaiementAbonnement).
- Crée un DjomyAdapter dans payments/adapters/out/gateway implémentant le port
  FournisseurPaiementGateway. Ajoute la valeur DJOMY aux enums fournisseur
  (TransactionPaiement.fournisseur et, côté platform, au canal de
  PaiementAbonnement si nécessaire — signale-moi si une migration côté platform
  est requise en plus de celle de payments). Migration Flyway pour les enums.
- API Djomy (base sandbox https://sandbox-api.djomy.africa, lis les identifiants
  depuis l'env : DJOMY_CLIENT_ID, DJOMY_CLIENT_SECRET, DJOMY_BASE_URL — jamais
  en dur) :
  * Auth en 2 étapes : (1) HMAC-SHA256(clientId, clientSecret) en hex ;
    (2) POST /v1/auth avec header "X-API-KEY: {clientId}:{hexSignature}" pour
    récupérer un Bearer token (mets-le en cache jusqu'à expiration).
  * TOUTE requête authentifiée exige LES DEUX headers à la fois :
    "Authorization: Bearer {token}" ET "X-API-KEY: {clientId}:{HMAC}" recalculé
    à chaque requête. Sans X-API-KEY => 401 même avec un Bearer valide.
  * Méthode par DÉFAUT : create_payment_gateway (délègue moyen de paiement + OTP
    au portail Djomy). payerNumber OBLIGATOIRE (sinon 400) : collecte le numéro
    du payeur avant l'appel. Récupère redirectUrl dans la réponse et renvoie-le
    au frontend pour redirection.
  * verify_payment (GET) : à appeler côté serveur pour confirmer status==='SUCCESS'
    AVANT de valider quoi que ce soit. Mappe les devises non-ISO renvoyées
    ('GNF','FCFA','Dollar','Leones') vers ISO (GNF, XOF, USD, SLE). Utilise
    receivedAmount (net après frais) pour la compta, pas paidAmount.
- Flux : Paiement (INITIE) -> DjomyAdapter.create_payment_gateway ->
  TransactionPaiement tracée (transactionId Djomy dans transaction_externe_id) ->
  renvoyer redirectUrl. La confirmation viendra par le webhook (P10) + verify_payment.
- Tests Djomy : unitaires sur le calcul HMAC et le mapping devises ; intégration
  de l'adaptateur avec un serveur HTTP mocké (MockWebServer/WireMock) pour
  /v1/auth, create_payment_gateway et verify_payment. N'appelle JAMAIS la vraie
  sandbox dans les tests automatisés.

Architecture du CLAUDE.md, multi-tenant, Flyway, tests.
À la fin : verify(), compile + tests, commit "feat(payments): implémentation
complète + fournisseur Djomy", récap + checklist, puis ATTENDS ma validation.
Si ambigu, demande.
```

## P10 — Module webhooks (entrants + sortants, + CALLBACK DJOMY)

```
Lis CLAUDE.md, features/00-conventions.md et features/10-webhooks.md.
Implémente la TOTALITÉ de features/10-webhooks.md : webhooks entrants (callbacks
paiement) et sortants (notifications tiers) — endpoints, règles, événements (§18).
Points généraux : vérification de signature + idempotence sur les entrants ;
retry exponentiel (nb_tentatives, prochaine_tentative) + signature HMAC sortante
+ souscription par type sur les sortants ; job planifié pour dépiler.

>>> INTÈGRE ICI LE CALLBACK DJOMY :
    POST /api/v1/webhooks/paiement/djomy (hors authentification JWT).
- Vérifier la signature AVANT tout traitement. Djomy envoie l'en-tête
  "X-Webhook-Signature" au format "v1:<hex>". Extraire la partie hex après les
  deux-points, calculer HMAC-SHA256(rawBody, DJOMY_CLIENT_SECRET) en hex, et
  comparer en time-constant. Rejeter (400) si non identiques ou si le préfixe
  n'est pas "v1". Lis le corps BRUT (raw body), pas le JSON re-sérialisé.
- Idempotence : un même événement n'est traité qu'une fois (PaymentWebhook.traite
  = true) ; stocke payload_brut, signature, verifie.
- Types reçus : payment.created, payment.redirected, payment.cancelled,
  payment.pending, payment.success, payment.failed. N'agis (fulfillment) QUE sur
  payment.success.
- Ne JAMAIS valider sur la seule foi du webhook : sur payment.success, appelle
  verify_payment (DjomyAdapter) et confirme status==='SUCCESS' côté serveur avant
  d'émettre l'événement métier.
- Retourne HTTP 200 immédiatement (même si traitement async) ; un non-200 est un
  échec de livraison pour Djomy.
- Après confirmation : retrouve la TransactionPaiement via le transactionId, mets
  à jour le Paiement (REUSSI/ECHOUE) et émets PaiementReussi/PaiementEchoue (ou
  les équivalents abonnement pour platform) — qui déclenchent reçu + maj facture
  + notification (déjà câblés).

⚠️ À me RAPPELER explicitement dans ta réponse :
- L'URL du webhook DOIT être enregistrée dans l'espace développeur Djomy, sinon
  aucun événement n'est envoyé.
- Djomy n'appelle que des URLs HTTPS. En local, utiliser un tunnel
  (ex. `ngrok http 8080`) et configurer l'URL ngrok dans Djomy avant de tester.

Tests : signature valide/invalide, préfixe non v1, idempotence (double envoi),
chaque type d'événement, et le cas où verify_payment contredit le webhook.
Architecture du CLAUDE.md, multi-tenant, Flyway, tests.
À la fin : verify(), compile + tests, commit "feat(webhooks): implémentation
complète + callback Djomy", récap + checklist, puis ATTENDS ma validation.
Si ambigu, demande.
```

## P11 — Module communication (SMS / Email / WhatsApp)

```
Lis CLAUDE.md, features/00-conventions.md et features/11-communication.md.
Implémente la TOTALITÉ de features/11-communication.md : envoi SMS/Email/WhatsApp,
diffusion de masse, statuts de livraison, et l'écoute des événements métier qui
déclenchent les notifications automatiques — endpoints, règles, événements (§18).
Adaptateurs hexagonaux par canal derrière un port NotificationGateway ; chaque
envoi crée N MessageDestinataire avec statut de livraison ; envoi asynchrone.
Consomme AbsenceEnregistree, BulletinPublie, PaiementReussi, EcheanceDepassee,
AbonnementBientotExpire.
Templates (§11.2) = BONUS : signale-moi avant de créer la table TemplateMessage.
Architecture du CLAUDE.md, multi-tenant, Flyway, tests.
À la fin : verify(), compile + tests, commit "feat(communication): implémentation
complète", récap + checklist, puis ATTENDS ma validation. Si ambigu, demande.
```

## P12 — Transverse (dashboards, audit, exports, paramètres)

```
Lis CLAUDE.md, features/00-conventions.md et features/12-transverse.md.
Implémente la TOTALITÉ de features/12-transverse.md : dashboards par rôle (§12),
audit/journalisation (§13), imports-exports + génération PDF (§14), recherche/
paramètres/i18n (§15).
ATTENTION : tables additionnelles (AuditLog, ParametreTenant, et les bonus
TemplateMessage, Notification). AVANT de les créer, liste-les-moi avec leur schéma
proposé et attends ma validation.
Génération PDF asynchrone (bulletins, reçus, factures, convocations) avec branding
du tenant. Dashboards filtrés par rôle/tenant/portée.
Architecture du CLAUDE.md, multi-tenant, Flyway, tests.
À la fin : verify(), compile + tests, commit "feat(transverse): dashboards,
audit, exports, paramètres", récap + checklist, puis ATTENDS ma validation.
Si ambigu, demande.
```

## P13 — Vérification finale

```
Lis features/13-bonus-checklist-events.md.
Parcours toute la checklist §17 et le contrat d'événements §18 : vérifie que
chaque module est réellement implémenté (endpoints, règles, événements émis ET
consommés) et que tous les événements du tableau §18 ont un émetteur et un
consommateur câblés. Vérifie en particulier la chaîne Djomy :
create_payment_gateway -> redirectUrl -> webhook signé -> verify_payment ->
PaiementReussi -> reçu + maj facture + notification.
Lance ApplicationModules.of(Application.class).verify() + génère la doc Modulith
(Documenter). Lance toute la suite de tests.
Donne-moi un rapport final : ce qui est complet, les écarts restants, et les
bonus §16 non implémentés. Ne corrige rien sans me demander d'abord.
```

---

## Comptes de test Djomy (sandbox) — pour tests MANUELS uniquement

> Ne jamais mettre ces valeurs dans des tests automatisés appelant la vraie sandbox.

Cartes (date d'expiration future, CVV = 3 chiffres) :
- 2303779999000275  Frictionless (transStatus != C)  => Succès
- 2303779999000408  Challenge   (transStatus == C)   => Succès
- 2303779999000291  Frictionless (transStatus == N)  => Échec (Not Authenticated)
- 2303779999000317  Frictionless (transStatus == U)  => Échec (Unavailable)
- 2303779999000424  Challenge   (transStatus == N)   => Échec (Not Authenticated)
- 2303779999000432  Challenge   (transStatus == U)   => Échec (Unavailable)

PayCard : compte 537417414 ; OTP de confirmation 0000.
Soutra   : compte 622356781 ; PIN 1111.
Orange Money (OM) : tous les paiements ÉCHOUENT en sandbox (normal).
MoMo (MTN) : NE PAS utiliser actuellement (problèmes sandbox côté Djomy).
⚠️ Montants <= 10 000 GNF pour Soutra et PayCard.

---

## Ordre conseillé
P0 → P1 (iam, socle) → P2 (platform) → P3 (academics) → P4 (students) →
P5 (staff) → P6 (attendance) → P7 (grading) → P8 (billing) → P9 (payments+Djomy)
→ P10 (webhooks+Djomy) → P11 (communication) → P12 (transverse) → P13 (vérif).

## Rappels Djomy (P9/P10)
- Auth = HMAC-SHA256(clientId, clientSecret) hex -> POST /v1/auth (X-API-KEY:
  clientId:hex) -> Bearer. Les DEUX headers sur CHAQUE requête.
- create_payment_gateway par défaut (OTP géré par le portail). payerNumber requis.
- Toujours verify_payment côté serveur avant de valider une commande/paiement.
- Webhook : signature "v1:<hex>" = HMAC-SHA256(rawBody, clientSecret) ; 200
  immédiat ; n'agir que sur payment.success ; URL à enregistrer côté Djomy ;
  HTTPS only (ngrok en local).
- Identifiants en .env non commité. Régénère la clé si elle a transité par un chat.
