# Intégration avec la plateforme officielle du ministère — comparaison des options

> Document produit conformément à `features/dpe.md` §25. **Aucune API officielle du ministère n'est
> connue, documentée ou accessible à ce jour.** Rien dans le code actuel n'indique une tentative
> d'intégration passée (confirmé par audit du module `grading` — calcul 100% interne, aucun client
> HTTP externe, aucune configuration pointant vers un système tiers). Tout point marqué
> **À CONFIRMER AVEC LE MINISTÈRE** est une hypothèse de travail, pas un fait établi.

## Option 1 — Intégration API officielle

| Critère | Évaluation |
|---|---|
| **Avantages** | Données toujours à jour sans action manuelle de l'école ; source unique de vérité garantie ; élimine les erreurs de saisie/mapping d'un import manuel ; expérience utilisateur supérieure à terme. |
| **Inconvénients** | Dépend entièrement d'un système que nous ne contrôlons pas ; aucune garantie de disponibilité, de format stable ou de délai de mise à disposition ; nécessite une convention/autorisation officielle avec le ministère avant tout développement. |
| **Sécurité** | Nécessite un mécanisme d'authentification inter-systèmes (clé API, OAuth2 client-credentials, etc. — **À CONFIRMER** lequel), un chiffrement en transit (TLS, déjà standard), et une gestion rigoureuse des secrets (pattern déjà en place dans le projet pour d'autres intégrations, ex. fournisseurs de paiement — réutilisable). |
| **Complexité** | Élevée dès le premier jour : contrat d'interface inconnu, gestion des versions d'API, gestion des pannes/timeouts, mapping vers notre modèle interne quel que soit le format. Complexité qui ne peut être réduite avant que le contrat soit connu. |
| **Maintenance** | Dépendante du ministère : toute évolution de son API (changement de champ, dépréciation) impacte notre code sans préavis garanti. |
| **Fiabilité** | Inconnue — dépend de la disponibilité et de la stabilité du système ministériel, hors de notre contrôle. |
| **Synchronisation** | Peut être automatisée (planifiée, ex. nocturne) une fois le contrat connu — voir modèle de statut de synchronisation dans `DPE_ARCHITECTURE_ANALYSIS.md` §5 (`import_batches`, réutilisable pour une synchronisation API en plus d'un import fichier). |
| **Gestion des erreurs** | Nécessite un plan de reprise (retry, alerte en cas d'échec prolongé) — le projet a déjà un pattern de retry exponentiel pour les webhooks sortants (`webhooks` module), directement réutilisable comme modèle. |
| **Coût** | Développement initial potentiellement significatif (contrat inconnu = estimation impossible avant confirmation) ; coût d'exploitation faible une fois stable (pas de saisie manuelle). |
| **Dépendance au ministère** | **Totale.** Aucune capacité de démarrer le pilote sans cette dépendance si cette option est retenue seule. |

## Option 2 — Import Excel/CSV

| Critère | Évaluation |
|---|---|
| **Avantages** | Ne dépend d'aucune coopération technique préalable du ministère — l'école exporte simplement ce qu'elle obtient déjà de la plateforme officielle ; permet de démarrer le pilote immédiatement ; contrôle total de notre côté sur le pipeline de validation. |
| **Inconvénients** | Processus manuel côté école (risque d'oubli, de retard) ; qualité des données dépendante de la rigueur de l'utilisateur qui exporte/importe ; pas de mise à jour temps réel. |
| **Sécurité** | Fichier téléversé par un humain : nécessite validation stricte du contenu (voir pipeline §10 de `DPE_ARCHITECTURE_ANALYSIS.md`), stockage temporaire sécurisé, purge après traitement — risques maîtrisables et déjà couverts par le modèle de données proposé (`import_batches`). |
| **Complexité** | Modérée : le principal risque est l'hétérogénéité des formats d'export selon les écoles/versions du système ministériel (colonnes, encodage, format de date) — d'où l'exigence d'un mapping de colonnes correctif par l'administrateur plutôt qu'un mapping figé. |
| **Maintenance** | Faible et sous notre contrôle total : toute évolution de format se corrige côté mapping, sans dépendre d'un tiers. |
| **Fiabilité** | Élevée une fois le pipeline de validation en place (aperçu avant confirmation, détection de doublons/erreurs) — le risque principal est humain (mauvais fichier importé), couvert par l'aperçu obligatoire. |
| **Synchronisation** | Manuelle par nature (l'école déclenche l'import quand elle a exporté depuis le système officiel) — affichage explicite de la fraîcheur des données (« Dernier import : JJ/MM/AAAA ») pour ne jamais laisser croire à du temps réel. |
| **Gestion des erreurs** | Rapport d'erreurs ligne par ligne, correction puis ré-import — déjà partiellement modélisé par `StudentImportService` existant (à étendre, voir gaps identifiés dans `DPE_ARCHITECTURE_ANALYSIS.md` §3). |
| **Coût** | Développement initial maîtrisé (bibliothèque Apache POI à ajouter, pipeline additif sur un modèle déjà ébauché) ; coût d'exploitation = temps humain côté école pour l'export/import régulier. |
| **Dépendance au ministère** | **Aucune dépendance technique** — seulement une dépendance sur le fait que l'école ait accès à un export de ses propres données depuis la plateforme officielle, ce qui est un usage normal d'un système dont l'école est déjà utilisatrice. |

## Recommandation pour le pilote

**Démarrer exclusivement avec l'Option 2 (Import Excel/CSV).**

Justification :
- Le pilote (DPE de Coyah) ne peut pas être bloqué par une négociation d'accès API dont l'existence
  même n'est pas confirmée (cahier des charges §26 : ne jamais supposer qu'une API existe).
- L'Option 2 est réalisable avec les compétences et l'infrastructure déjà présentes dans le projet
  (PDF/documents déjà gérés côté stockage, pattern d'import déjà ébauché côté CSV élèves, pattern de
  retry déjà en place côté webhooks pour inspirer la gestion d'erreurs).
- L'architecture proposée (`OfficialDataAdapter`, voir `DPE_ARCHITECTURE_ANALYSIS.md` §11) garantit
  que l'Option 1 pourra être ajoutée plus tard **sans réécrire** le pipeline de normalisation, la base
  de données ni les vues consommatrices (école, parent, DPE) — seul un nouvel adaptateur serait à
  développer le jour où une API officielle serait confirmée et documentée.
- Le principe fondamental du cahier des charges (§22 : « le ministère calcule et produit les résultats
  officiels, notre plateforme collecte/centralise/communique/analyse/supervise ») est respecté par les
  deux options ; l'Option 2 permet de le démontrer concrètement dès le pilote sans attendre une
  intégration technique tierce.

**Action de suivi explicite** : lors de toute discussion avec la DPE ou le ministère, poser la
question de l'existence d'une API officielle et de ses conditions d'accès. Tant qu'aucune réponse
n'est obtenue, considérer ce point comme **À CONFIRMER AVEC LE MINISTÈRE** et continuer sur l'Option 2.
