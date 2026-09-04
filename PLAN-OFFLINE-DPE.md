# Plan — Fonctionnement hors-ligne (offline-first) pour la présentation DPE

> Document de présentation/décision, pas une spécification d'implémentation détaillée. Objectif :
> montrer à la DPE que la plateforme peut continuer à fonctionner dans une école sans connexion internet
> stable (cas très fréquent en préfecture : coupures réseau, écoles rurales, salles sans Wi-Fi fiable),
> sur les 5 fonctionnalités jugées critiques sur le terrain : encaissement des frais, inscription,
> stockage des photos/documents, génération de badge, scan de badge.

---

## 1. Pourquoi hors-ligne — l'argument pour la DPE

Aujourd'hui, LacaEducation est une application web classique : toute action (encaisser un paiement,
inscrire un élève, imprimer un badge) nécessite une requête réseau immédiate vers le serveur. Si la
connexion de l'école tombe pendant une opération — un cas courant en préfecture, en particulier en zone
rurale ou lors des pics de rentrée — l'agent ne peut plus rien faire tant que le réseau n'est pas revenu.

L'objectif du mode hors-ligne : l'agent (secrétaire, comptable) continue à travailler normalement même
sans réseau, et tout se synchronise automatiquement dès que la connexion revient — sans double saisie,
sans perte de données, sans doublon.

**Message clé pour la présentation** : *« l'école n'est jamais bloquée par une coupure réseau »*.

---

## 2. Choix technique : pourquoi le navigateur suffit (pas besoin d'une appli native)

Le principe retenu est le **PWA offline-first** (Progressive Web App) : on reste dans le navigateur,
pas besoin d'installer une application séparée par appareil ni de gérer deux bases de code. Trois briques
du navigateur, déjà standard et gratuites, fournissent la mémoire et les mécanismes nécessaires :

| Brique | Rôle | Capacité mémoire |
|---|---|---|
| **Service Worker** | Intercepte les requêtes réseau ; sert l'application (HTML/JS/CSS) même sans réseau ; réveille la synchronisation au retour du réseau. | — (mécanisme, pas de stockage) |
| **IndexedDB** (via une librairie comme *Dexie.js*) | Base de données structurée **côté navigateur** : élèves, factures, paiements en attente, files de badges à imprimer. | Plusieurs centaines de Mo à quelques Go selon l'appareil (le navigateur alloue un quota proportionnel à l'espace disque libre, extensible via l'API *Persistent Storage*). Largement suffisant pour une école (quelques milliers d'élèves + leurs mouvements financiers). |
| **Cache Storage API** | Stockage des fichiers binaires : photos d'élèves, badges déjà générés (PDF/PNG), pages de l'application. | Même quota que IndexedDB, partagé. |

Ces trois briques sont déjà pré-requises par le manifeste PWA ajouté récemment au site (cf. plan de test
§35) — ce plan en est la suite logique, pas un chantier séparé.

**Pourquoi pas une appli native (Android/iOS) ?** Coût de développement et de maintenance doublé (deux
bases de code), déploiement plus lent (stores), alors que le PWA offre la même capacité de stockage local
et fonctionne sur n'importe quel appareil (ordinateur du secrétariat, tablette, téléphone Android) sans
installation depuis un store. À réévaluer uniquement si un besoin d'accès matériel non disponible au
navigateur apparaît (ex. imprimante à badge USB non pilotable en Web).

---

## 3. Principe général de synchronisation

Toute action faite hors-ligne suit le même schéma en 3 temps :

1. **Écriture locale immédiate** : l'action (paiement, inscription, photo) est enregistrée dans IndexedDB
   avec un statut `EN_ATTENTE_SYNC` et un identifiant temporaire généré localement (UUID). L'agent voit
   son action prise en compte à l'écran instantanément — aucune attente réseau perçue.
2. **File d'attente de synchronisation** : chaque action en attente est ajoutée à une file locale
   (`SyncQueue`). Le Service Worker écoute l'événement *Background Sync* du navigateur (ou, à défaut sur
   les navigateurs qui ne le supportent pas encore tous, un intervalle de vérification en tâche de fond) :
   dès que le réseau revient, la file se rejoue automatiquement, dans l'ordre, vers les endpoints existants
   (`POST /paiements`, `POST /inscriptions`, etc. — **aucune nouvelle API n'est nécessaire**, ce sont les
   mêmes endpoints qu'aujourd'hui).
3. **Réconciliation** : la réponse du serveur remplace l'identifiant temporaire par l'identifiant réel et
   marque l'action `SYNCHRONISE`. En cas de conflit (ex. la classe est devenue complète entre-temps, un
   même paiement rejoué deux fois), un identifiant d'idempotence (déjà généré côté client à la création)
   permet au serveur de détecter un doublon et d'y répondre proprement plutôt que de créer une deuxième
   entrée — point à valider avec le backend au moment de l'implémentation (ajout d'une clé d'idempotence
   sur les endpoints concernés, si elle n'existe pas déjà).

Un indicateur visuel simple (bandeau discret « Hors-ligne — 3 actions en attente de synchronisation »)
tient l'agent informé sans bloquer son travail.

---

## 4. Détail par fonctionnalité

### 4.1 Encaissement des frais (priorité n°1 — impact financier direct)

- **Cas d'usage terrain** : un parent vient payer en espèces au secrétariat, le réseau est coupé au même
  moment — aujourd'hui, l'agent ne peut pas encaisser.
- **Fonctionnement hors-ligne** : le paiement (élève, facture, montant, canal = ESPECES) est enregistré
  localement, un **reçu provisoire** est imprimé/affiché immédiatement (marqué « en attente de
  synchronisation », avec le même numéro de reçu qui sera confirmé après coup) pour que le parent reparte
  avec une preuve de paiement séance tenante. La facture correspondante, déjà chargée en local (voir §4.6
  pré-chargement), passe visuellement en « payée localement » pour éviter un double encaissement par un
  autre agent avant la synchronisation.
- **Limite assumée et à annoncer clairement à la DPE** : les canaux qui dépendent d'un fournisseur externe
  (Orange Money, MTN Money, Wave, carte bancaire) nécessitent par nature une connexion au moment du
  paiement — le mode hors-ligne pour l'encaissement concerne donc les paiements en **espèces au
  secrétariat**, pas les paiements mobiles. C'est déjà le cas d'usage le plus fréquent en présentiel.

### 4.2 Inscription / réinscription

- **Cas d'usage terrain** : pic d'inscriptions à la rentrée, souvent en zone à réseau instable.
- **Fonctionnement hors-ligne** : le formulaire d'inscription (élève, parent, classe) fonctionne à
  l'identique, avec vérification des règles qui peuvent être validées localement à partir des données déjà
  synchronisées (capacité de la classe telle que connue lors du dernier téléchargement, matricule généré
  localement selon le même format que le serveur). Le contrôle de quota d'élèves du plan d'abonnement
  (`QuotaService`) est vérifié de façon optimiste en local (sur la base du dernier effectif connu) puis
  revalidé strictement par le serveur à la synchronisation — en cas de dépassement découvert seulement à
  ce moment-là, l'inscription reste en attente avec un message clair pour l'administrateur plutôt que
  d'être silencieusement perdue.

### 4.3 Stockage des images (photos élèves, documents administratifs)

- **Fonctionnement hors-ligne** : les photos capturées (webcam/tablette) sont compressées côté navigateur
  puis stockées dans le Cache Storage local, associées à l'élève en attente de synchronisation. Lors du
  retour réseau, elles sont téléversées vers le stockage objet du serveur comme aujourd'hui.
- **Point mémoire à souligner en présentation** : une photo compressée (format WebP, redimensionnée à la
  taille d'affichage réelle d'un badge/fiche élève) pèse quelques dizaines de Ko — même une école de
  2000 élèves représente quelques dizaines de Mo, très loin du quota disponible sur un navigateur moderne.

### 4.4 Génération de badge

- **Fonctionnement hors-ligne** : le badge (QR code + informations élève + photo) peut être **généré et
  imprimé localement sans réseau**, dès lors que les données de l'élève (y compris sa photo) sont déjà
  présentes en local — la génération de badge est un calcul/rendu côté client (bibliothèque de génération
  de QR code + mise en page), qui ne nécessite pas d'appel serveur pour produire le visuel. Le PDF/PNG
  généré est mis en cache localement (réimpression possible sans réseau) et une trace de génération est
  envoyée au serveur à la prochaine synchronisation (pour l'audit et l'historique).

### 4.5 Scan de badge (contrôle d'accès / présence)

- **Fonctionnement hors-ligne** : le scan (caméra du téléphone/tablette lisant le QR code du badge) est
  intrinsèquement une opération locale — décoder un QR code ne nécessite aucun réseau. Ce qui nécessite
  normalement le réseau, c'est la vérification de l'identité scannée contre la base des élèves : cette
  vérification se fait contre la copie locale des élèves de l'école (synchronisée périodiquement, voir
  §4.6), donc fonctionne aussi hors-ligne. Le résultat du scan (présence enregistrée, badge invalide,
  élève non trouvé) s'affiche immédiatement ; l'enregistrement de présence rejoint la file de
  synchronisation comme les autres actions.

### 4.6 Pré-chargement (condition commune aux 5 fonctionnalités)

Pour que l'inscription, l'encaissement, le badge et le scan fonctionnent hors-ligne, l'application doit
disposer d'une **copie locale à jour** des données de référence de l'école : liste des élèves actifs,
classes, factures en cours, frais et échéances. Cette copie se met à jour automatiquement dès que le
réseau est disponible (au chargement de l'app, puis en tâche de fond), sans action manuelle de l'agent —
un peu comme un email qui se synchronise dès qu'il y a du réseau, mais reste consultable hors-ligne
entre-temps.

---

## 5. Sécurité et confidentialité des données stockées localement

Point sensible à anticiper pour la présentation (la DPE pourra le demander) : les données d'élèves
(identité, photos, montants payés) résideront temporairement sur l'appareil du secrétariat.

- Le stockage local (IndexedDB / Cache Storage) est **isolé par domaine et par navigateur** — inaccessible
  aux autres sites web, mais accessible à quiconque a un accès physique à l'appareil déverrouillé.
- Recommandation à intégrer : chiffrement des données sensibles au repos dans IndexedDB (clé dérivée de la
  session de l'agent connecté), et purge automatique des données locales à la déconnexion ou après un
  délai d'inactivité configurable.
- Aucune donnée hors-ligne ne doit persister sur un appareil partagé au-delà de la session — point à
  traiter avant la mise en production, pas uniquement une amélioration de confort.

---

## 6. Ce que le hors-ligne ne couvre pas (limites à annoncer honnêtement)

- Les paiements par canal mobile/carte (Orange Money, MTN, Wave, Visa/Mastercard) nécessitent une
  connexion au moment du paiement — non contournable, ce sont des appels à des fournisseurs externes.
- Les tableaux de bord et statistiques agrégées (comparaisons inter-écoles pour la DPE, par exemple)
  restent des calculs serveur et ne sont pas disponibles hors-ligne.
- L'envoi de notifications (SMS/email/WhatsApp) aux parents ne peut évidemment se faire que lorsque le
  serveur a reçu l'information (donc après synchronisation).
- Un appareil qui n'a **jamais** été connecté à l'école ne peut pas fonctionner hors-ligne dès le premier
  jour (il n'a pas encore de copie locale des données) — la première connexion doit se faire avec réseau.

---

## 7. Feuille de route proposée (phasage)

| Phase | Contenu | Pourquoi cet ordre |
|---|---|---|
| **1. Fondations** | Service Worker + IndexedDB (Dexie.js) + file de synchronisation générique + indicateur visuel d'état réseau. | Brique commune à toutes les fonctionnalités suivantes — rien d'autre n'est possible sans elle. |
| **2. Encaissement des frais hors-ligne** | Paiement espèces + reçu provisoire + verrou anti-double-encaissement local. | Priorité annoncée par l'utilisateur, impact financier direct et visible pour la DPE. |
| **3. Inscription hors-ligne** | Formulaire d'inscription + validations optimistes + revalidation serveur. | Cas d'usage à fort enjeu (pics de rentrée). |
| **4. Photos + badges hors-ligne** | Capture/compression photo, génération et impression de badge en local. | Dépend des fondations + bénéficie d'avoir déjà la logique de fichiers binaires posée pour les reçus (phase 2). |
| **5. Scan de badge hors-ligne** | Lecture QR + vérification contre la copie locale + présence en file de sync. | Dépend de la copie locale des élèves déjà en place depuis la phase 1, et des badges (phase 4). |
| **6. Durcissement** | Chiffrement local, purge de session, tests de coupure réseau simulée, gestion des conflits de synchronisation. | À faire avant mise en production réelle, une fois les 5 fonctionnalités validées sur le terrain. |

---

## 8. Ce qu'il faut retenir pour la présentation DPE (résumé en une diapositive)

1. **Le problème** : coupure réseau = école bloquée aujourd'hui.
2. **La solution** : les 5 opérations du quotidien (encaisser, inscrire, stocker les photos, générer et
   scanner les badges) continuent de fonctionner sans réseau, avec synchronisation automatique au retour.
3. **La technologie** : standards du navigateur (Service Worker, IndexedDB, Cache Storage) — pas
   d'application séparée à installer, pas de matériel supplémentaire.
4. **La limite honnête** : les paiements mobiles/carte restent en ligne uniquement (dépendance
   fournisseur externe), le reste fonctionne hors-ligne.
5. **Le calendrier** : fondations d'abord, puis encaissement, puis inscription, puis photos/badges, puis
   scan, puis durcissement sécurité avant généralisation.
