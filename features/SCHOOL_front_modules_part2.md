# SCHOOL SaaS — Prompts FRONT-END par module — Partie 2/2

> Suite de la partie 1. Même socle (Next.js + Tailwind + Mantine, client OpenAPI typé, TanStack Query,
> zod, erreurs en toasts, i18n FR, AppShell, DataTable, garde de rôle, respect du statut école).

---

## MODULE `grading` — Notes, examens, bulletins + ADMIN des barèmes paramétrables

```
Construis le front du module "grading". C'est le module le plus riche : il doit gérer la NOTATION
PARAMÉTRABLE (primaire par compétences, collège/lycée par moyennes coefficientées, supérieur par crédits)
et laisser l'admin configurer barèmes, coefficients, mentions et décisions.
Accès : ENSEIGNANT (évaluations/notes de ses classes), ADMIN_ECOLE/DIRECTEUR (barèmes, bulletins).

=== A) ADMINISTRATION DES BARÈMES (ADMIN_ECOLE/DIRECTEUR) — /grading/baremes ===
- Liste des barèmes (GET /api/v1/grading-schemes) : libellé, type_etablissement
  (PRIMAIRE|COLLEGE|LYCEE|SUPERIEUR|MIXTE|PERSO), mode_evaluation (NOTE_CHIFFREE|COMPETENCE|CREDITS|MIXTE),
  échelle_max, seuil_réussite, badge "par défaut".
- À la création d'école, des MODÈLES PRÉ-DÉFINIS sont seedés selon le type ; l'écran permet de TOUT
  modifier. Bouton "Créer un barème" + "Dupliquer un modèle".
- Éditeur de barème (formulaire) :
  * Général : libellé, type, mode_evaluation, échelle_max (20/10/100), seuil_réussite, arrondi
    (ENTIER|DECIMAL_1|DECIMAL_2), définir par défaut (PATCH /{id}/definir-defaut).
  * Mentions (POST /{id}/mentions) : table éditable libellé + borne_min/borne_max + ordre
    (ex. 16-20 Très bien, 14-16 Bien...). Entièrement paramétrable, rien en dur.
  * Décisions (POST /{id}/decisions) : ADMIS / REDOUBLE / ADMIS_CONDITION... + seuil_min.
  * Échelle de compétences (POST /{id}/competence-scale) pour le primaire : codes (A/B/C/D ou
    Acquis/En cours/Non acquis) + libellé + valeur numérique (mapping optionnel) + couleur + ordre.
  * Affectation aux niveaux (POST /{id}/niveaux) : à quels niveaux s'applique ce barème (sinon défaut
    tenant). Pour MIXTE : un barème différent par niveau.
- Coefficients : rappel/lien vers academics (PUT /classes/{classeId}/matieres/{id}/coefficient) pour
  ajuster le coefficient d'une matière par classe. Afficher un récap des coefficients par classe.

=== B) ÉVALUATIONS (ENSEIGNANT) — /grading/evaluations ===
- Liste (GET /api/v1/evaluations) filtres classe, matière, période, type, enseignant.
- Créer/éditer (POST/PUT) : classe, matière (limitées à celles que l'enseignant enseigne -> 403 sinon),
  période, type (INTERROGATION|DEVOIR|COMPOSITION|EXAMEN), note_sur, coefficient. Interdit si période
  clôturée (désactiver + message).

=== C) SAISIE DES NOTES — /grading/evaluations/{id}/notes ===
- Grille de saisie en masse pour toute la classe (POST /evaluations/{id}/notes/lot) : liste élèves +
  champ note. Validation 0 <= valeur <= note_sur (sinon 422 mis en évidence sur la cellule).
- MODE selon le barème : si COMPETENCE (primaire), saisir un niveau de compétence (select A/B/C/D) au lieu
  d'une note chiffrée. Si CREDITS, gérer les UE. L'UI s'adapte au mode_evaluation du barème de la classe.
- Correction unitaire (PUT /notes/{id}) si non validée. Valider en bloc (PATCH /evaluations/{id}/notes/
  valider) -> verrouille (cadenas), correction nécessite déverrouillage par rôle autorisé.
- Saisie interdite hors période ouverte d'une année EN_COURS (désactiver + message).

=== D) EXAMENS — /grading/examens ===
- Liste (GET) filtres classe, période, statut. Planifier (POST) : classe, période, type
  (BLANC|SEMESTRIEL|ANNUEL|NATIONAL), date. Changer statut (PATCH /statut : PLANIFIE->EN_COURS->TERMINE).
- Convocation PDF (GET /examens/{id}/convocation) — génération asynchrone, bouton télécharger.

=== E) BULLETINS — /grading/bulletins ===
- Générer pour une classe + période (POST /api/v1/bulletins/generer) — uniquement après clôture de
  période ; sinon message. Génération asynchrone (suivi de progression).
- Liste (GET /api/v1/bulletins) filtres élève, classe, période, statut (BROUILLON|GENERE|PUBLIE).
- Détail bulletin (GET /{id}) : en-tête (élève, classe, période, moyennes) + lignes par matière. L'AFFICHAGE
  S'ADAPTE AU BARÈME : notes chiffrées + moyennes + rang + mentions (secondaire) ; compétences + appréciations
  (primaire) ; crédits/UE validées (supérieur). Statistiques de classe (moyenne classe, plus forte/faible,
  effectif), mention et décision (issues du barème, pas en dur).
- Publier (PATCH /{id}/publier) BROUILLON/GENERE -> PUBLIE (rend visible parent/élève) ; PDF (GET /{id}/pdf).
- Vue élève/parent : ses bulletins publiés (GET /api/v1/eleves/{id}/bulletins) en lecture seule + PDF.

RÈGLES FRONT
- L'UI de saisie ET d'affichage du bulletin DOIVENT lire le mode_evaluation du barème applicable à la
  classe/niveau et s'adapter (chiffres vs compétences vs crédits).
- À la clôture d'une période (academics), les bulletins se génèrent automatiquement (événement) -> rafraîchir.
- BulletinPublie -> notification parent (communication). Gérer 422 (hors période), 403 (classe non
  enseignée par l'enseignant).
DÉFINITION DE TERMINÉ : admin barèmes (mentions/décisions/compétences/affectation niveaux + par défaut) ;
coefficients par classe ; évaluations ; saisie notes en masse adaptée au mode + validation/verrou ;
examens + convocation ; bulletins (génération, affichage adapté au barème, publication, PDF) ; vue
parent/élève ; i18n.
```

---

## MODULE `billing` — Frais scolaires & facturation (école ↔ parents)

```
Construis le front du module "billing" (frais scolaires — distinct de l'abonnement plateforme).
Accès ADMIN_ECOLE/DIRECTEUR/COMPTABLE (gestion), PARENT (consultation + payer).

ÉCRANS
1. Paramétrage des frais — /scolarite/frais (COMPTABLE/ADMIN) :
   - DataTable (GET /api/v1/frais-scolaires) filtres classe, type, année. CRUD.
   - Formulaire : classe, type (INSCRIPTION|REINSCRIPTION|MENSUALITE|TRANSPORT|CANTINE|EXAMEN), montant,
     année. 
2. Échéances d'un frais — /scolarite/frais/{id}/echeances :
   - Échelonner (POST) : libellé, montant, date limite. La SOMME des échéances doit = montant total du
     frais (contrôle live côté front + 422 du back). Éditer/supprimer.
3. Factures — /scolarite/factures (COMPTABLE/ADMIN) :
   - DataTable (GET /api/v1/factures) filtres élève, classe, statut (IMPAYEE|PARTIELLE|PAYEE), période.
     Colonnes : référence, élève, montant total, payé, solde restant, statut coloré.
   - Émettre une facture (POST) pour un élève/frais ; GÉNÉRER EN LOT pour toute une classe
     (POST /api/v1/factures/generer-lot) avec aperçu du nombre de factures.
   - Détail facture (GET /{id}) + PDF (GET /{id}/pdf, asynchrone). Bouton "Encaisser un paiement" -> module
     payments.
   - Génération auto sur InscriptionValidee (frais d'inscription) -> visible automatiquement.
4. Situation financière d'un élève — /eleves/{id}/factures (réutilisable dans la fiche élève) :
   - GET /api/v1/eleves/{id}/factures : toutes les factures + solde global, échéances à venir/dépassées.
5. Portail parent — /portail/factures :
   - Le parent voit les factures de ses enfants, soldes, échéances, et peut PAYER en ligne (-> payments).
   - Échéance dépassée mise en évidence (relance auto côté back via communication).

RÈGLES FRONT : solde_restant = total - payé (affiché partout). Statuts dérivés. Référence unique par école.
Ne JAMAIS mélanger avec l'abonnement plateforme (sémantique et écrans distincts).
DÉFINITION DE TERMINÉ : frais + échéances (contrôle somme) ; factures (CRUD, lot, PDF) ; situation élève ;
portail parent (consultation + payer) ; i18n ; états vides/erreurs.
```

---

## MODULE `payments` — Paiements, encaissements & reçus (+ reçu multicanal)

```
Construis le front du module "payments" (paiement des frais scolaires par les parents + encaissements).
Accès PARENT (payer en ligne), COMPTABLE/ADMIN (encaissements, remboursements, reçus).

ÉCRANS
1. Payer une facture (PARENT ou guichet) — /scolarite/factures/{id}/payer :
   - Récap (élève, frais, montant dû, solde restant). Choix du type : COMPLET ou PARTIEL (montant saisi).
   - Choix du canal : ORANGE_MONEY, MTN_MONEY, WAVE, CARTE_BANCAIRE (selon le pays). Pour mobile money :
     saisie du téléphone payeur.
   - POST /api/v1/paiements -> initie ; rendre la nextAction (redirection PSP, push USSD, QR, secret).
   - Écran de statut : polling GET /api/v1/paiements/{id} jusqu'à l'état terminal
     (INITIE->EN_ATTENTE->REUSSI|ECHOUE|REMBOURSE). REUSSI -> reçu disponible + facture mise à jour
     (PARTIELLE/PAYEE). ECHOUE -> message + réessayer.
2. REÇU MULTICANAL (après paiement réussi) :
   - À REUSSI, afficher le reçu (GET /api/v1/paiements/{id}/recu/pdf) + confirmation que le reçu a été
     ENVOYÉ AU PARENT par WhatsApp / email / SMS de confirmation (selon ses préférences).
   - Bouton "Renvoyer le reçu" (POST /api/v1/paiements/{id}/recu/renvoyer) avec choix du/des canal(aux)
     (WhatsApp, email, SMS) — utile au guichet si le parent n'a rien reçu.
3. Suivi des paiements (COMPTABLE/ADMIN) — /scolarite/paiements :
   - DataTable (GET /api/v1/paiements) filtres élève, facture, canal, statut, date.
   - Détail paiement (GET /{id}) : statut + transaction fournisseur associée.
   - Rembourser (POST /{id}/rembourser, rôle COMPTABLE) avec confirmation -> facture ajustée + reçu d'avoir.
4. Transactions fournisseurs (COMPTABLE/ADMIN) — /scolarite/transactions :
   - DataTable (GET /api/v1/transactions-paiement) filtres fournisseur (ORANGE_MONEY|MTN_MONEY|WAVE|STRIPE),
     statut, date. Détail + payload_reponse (lecture technique, repliée).
5. Reçus — /scolarite/recus :
   - Liste (GET /api/v1/recus) filtres élève, date ; PDF (GET /{id}/pdf) ; renvoyer (multicanal).

RÈGLES FRONT : le succès est piloté par le STATUT SERVEUR (callback webhook), pas par le retour client.
Empêcher les doubles soumissions. Ne pas exposer de secret PSP. Messages PSP lisibles. Reçu généré
uniquement au REUSSI. Idempotence du renvoi (sauf demande explicite).
DÉFINITION DE TERMINÉ : payer (canal + partiel/complet + nextAction + polling) ; reçu + envoi multicanal +
renvoi ; suivi paiements + remboursement ; transactions fournisseurs ; reçus ; i18n.
```

---

## MODULE `webhooks` — Intégrations entrantes & sortantes

```
Construis le front du module "webhooks" (administration des intégrations). Accès ADMIN_ECOLE/DIRECTEUR.
(Les callbacks entrants de paiement sont des endpoints publics côté back, sans UI ; ici on gère la CONFIG
des webhooks SORTANTS et le journal.)

ÉCRANS
1. Endpoints sortants — /admin/webhooks :
   - DataTable (GET /api/v1/webhook-endpoints) : url, événements abonnés (badges), statut actif/inactif.
   - Créer/éditer (POST/PUT) : url, secret (généré/masqué, copiable une fois), sélection des
     type_evenement (PAIEMENT_RECU, BULLETIN_DISPONIBLE, ABSENCE, INSCRIPTION). Activer/désactiver
     (PATCH /activer), supprimer.
   - Afficher comment vérifier la signature HMAC sortante (doc inline + exemple).
2. Journal des envois — /admin/webhooks/journal :
   - DataTable (GET /api/v1/webhook-events) filtres statut (EN_ATTENTE|ENVOYE|ECHEC), endpoint, type, date.
     Colonnes : type, endpoint, statut, nb_tentatives, prochaine_tentative, code de réponse.
   - Détail d'un event : payload envoyé, réponse, historique des tentatives (retry exponentiel).
   - Rejouer manuellement (POST /api/v1/webhook-events/{id}/rejouer).

RÈGLES FRONT : le secret n'est montré qu'à la création (puis masqué). Statuts colorés. États vides.
DÉFINITION DE TERMINÉ : CRUD endpoints + secret + sélection événements + activation ; journal + détail +
rejouer ; i18n.
```

---

## MODULE `report-card` — Livret scolaire de l'élève (pluriannuel)

```
Construis le front du module "report-card" (livret scolaire / dossier de suivi pluriannuel).
Accès : staff (consultation + saisie limitée), PARENT/ELEVE (lecture seule de leur livret).

ÉCRANS
1. Livret de l'élève — /eleves/{id}/livret (onglet de la fiche élève + page dédiée) :
   - GET /api/v1/eleves/{id}/livret : vue chronologique PLURIANNUELLE.
   - En-tête : élève, parcours (niveaux/classes traversés), statut du livret (ACTIF/CLOTURE).
   - Par année (SchoolRecordYear) : classe, niveau, moyenne annuelle, rang annuel, décision de passage
     (ADMIS/REDOUBLE...), taux d'assiduité, appréciation du conseil, mention annuelle. Accordéon par année.
   - Sous chaque année : synthèse par période (moyenne, rang, appréciation).
   - Pour le PRIMAIRE : afficher les COMPÉTENCES acquises (SchoolRecordCompetence) avec l'échelle de
     compétences (couleurs) plutôt que des moyennes.
   - Faits marquants (SchoolRecordEvent) : distinctions, sanctions, santé, autres — timeline avec pièces
     jointes. Ajout (POST /api/v1/eleves/{id}/livret/evenements) réservé au staff.
2. Décision de passage — sur l'année du livret :
   - Saisir/valider la décision annuelle (POST /api/v1/livrets/{id}/annees/{anneeId}/decision) [staff].
3. Liste des livrets (staff) — /livrets : DataTable (filtres classe, année, niveau) pour accès rapide.
4. Export PDF — bouton "Télécharger le livret" (GET /api/v1/eleves/{id}/livret/pdf, asynchrone).
5. Clôture du livret — PATCH /api/v1/livrets/{id}/cloturer (fin de scolarité), confirmation.

RÈGLES FRONT
- Le livret est alimenté AUTOMATIQUEMENT (clôture période/année, bulletins publiés) -> essentiellement en
  lecture ; les champs éditables sont les appréciations, décisions, faits marquants.
- Années archivées en lecture seule. Parent/élève : lecture seule des années publiées.
- Distinguer clairement livret (cumul pluriannuel) et bulletin (une période) dans l'UI.
DÉFINITION DE TERMINÉ : livret chronologique multi-années ; affichage primaire (compétences) vs secondaire
(moyennes/rang) ; faits marquants ; décision de passage ; PDF ; clôture ; vue parent lecture seule ; i18n.
```

---

## MODULE `communication` — SMS / WhatsApp / Email centralisé

```
Construis le front du module "communication" (envoi sortant multicanal + modèles + préférences + historique).
Accès ADMIN_ECOLE/DIRECTEUR/SECRETAIRE (envoi, modèles), PARENT (ses préférences).

ÉCRANS
1. Composer un message — /communication/envoyer :
   - Choix du canal (SMS|WHATSAPP|EMAIL), des destinataires (parents/élèves d'une classe, sélection
     individuelle, ou par segment), choix d'un modèle (template) ou message libre, variables prévisualisées.
   - Aperçu du rendu (avec substitution des variables {{prenom}}, {{montant}}...). Envoi
     (POST /api/v1/communication/send). Pour email : sujet. Compteur de caractères/segments SMS.
   - Indiquer le coût/quota si applicable.
2. Modèles de messages — /communication/templates (ADMIN) :
   - DataTable (GET /api/v1/communication/templates) : code (RECU_PAIEMENT, ABSENCE, BULLETIN_DISPO,
     ECHEANCE_RELANCE, INSCRIPTION_VALIDEE...), canal, langue, statut. CRUD.
   - Éditeur : libellé, canal, sujet (email), contenu avec variables (insertion assistée), langue.
     Prévisualisation. Multilingue (un modèle par langue).
3. Historique des envois — /communication/messages :
   - DataTable (GET /api/v1/communication/messages) filtres canal, statut (QUEUED|SENT|DELIVERED|READ|
     FAILED), type (PAIEMENT|ABSENCE|BULLETIN...), date, élève/parent. Détail (GET /{id}) : contenu final,
     destinataire, statut de livraison (DLR), external_message_id, événement source.
4. Préférences de communication — /parents/{id}/preferences (et portail parent) :
   - GET|PUT /api/v1/parents/{id}/preferences-communication : canal préféré (WhatsApp/SMS/Email), opt-in/
     opt-out par type de message, langue préférée.

RÈGLES FRONT
- Beaucoup d'envois sont AUTOMATIQUES (consumers d'événements : PaiementReussi -> reçu, AbsenceEnregistree,
  BulletinPublie, EcheanceDepassee, InscriptionValidee) : l'historique les montre. L'écran "composer" sert
  aux envois manuels/ad hoc.
- Respect des préférences et de l'opt-out (afficher quand un destinataire est désinscrit).
- Statuts de livraison colorés ; retry visible. Rendu multilingue.
DÉFINITION DE TERMINÉ : composer (canal/destinataires/template/aperçu) ; CRUD templates multilingues ;
historique + DLR ; préférences parent (opt-in/opt-out) ; i18n ; états vides/erreurs.
```

---

## MODULE `expenses` — Dépenses, fournisseurs, budget, validation, rapports, trésorerie

```
Construis le front du module "expenses" (gestion des dépenses de l'école).
Accès COMPTABLE/ADMIN (saisie/paiement), DIRECTEUR (approbation), ADMIN (rapports/budget).

ÉCRANS
1. Dépenses — /depenses :
   - DataTable (GET /api/v1/expenses) filtres catégorie, fournisseur, statut (BROUILLON|SOUMISE|APPROUVEE|
     REJETEE|PAYEE), période, montant. Colonnes : libellé, catégorie, fournisseur, montant, date, statut.
   - Créer/éditer (POST/PUT, en BROUILLON) : catégorie (arbre), fournisseur (optionnel), libellé, montant,
     devise, date, mode_paiement (ESPECES|VIREMENT|MOBILE_MONEY|CHEQUE), référence pièce, justificatif
     (upload). 
   - WORKFLOW (boutons selon statut + rôle) : Soumettre (PATCH /soumettre) ; Approuver (PATCH /approuver,
     DIRECTEUR) ; Rejeter (PATCH /rejeter + motif) ; Payer (PATCH /payer, COMPTABLE). Afficher l'historique
     d'approbation (ExpenseApproval) en timeline. Transitions illégales -> 422.
   - Au-delà d'un seuil paramétrable -> approbation DIRECTEUR requise (indiqué dans l'UI).
   - Justificatif requis avant PAYEE (selon config) -> bloquer + message.
2. Catégories de dépenses — /depenses/categories (ADMIN) :
   - Arbre (réflexive parent_id) CRUD, budget annuel optionnel par catégorie.
3. Fournisseurs — /depenses/fournisseurs (ADMIN/COMPTABLE) :
   - DataTable CRUD : nom, type, téléphone, email, adresse, RIB/compte.
4. Budgets — /depenses/budgets (ADMIN) :
   - Par année + catégorie (POST/GET/PUT) : montant alloué. 
   - Suivi (GET /api/v1/budgets/suivi) : consommé vs alloué par catégorie (barres de progression), alerte
     si dépassement (BudgetDepasse) -> 409 ou warning à l'approbation selon config.
5. Rapports — /depenses/rapports :
   - Par catégorie (GET /rapports/par-categorie?annee) : total + % du budget (camembert/barres).
   - Par mois (GET /rapports/par-mois?annee) : évolution (courbe).
   - Par fournisseur (GET /rapports/par-fournisseur?annee).
   - Filtres par année scolaire, export.
6. Synthèse trésorerie — /tresorerie :
   - GET /api/v1/tresorerie/synthese?annee : ENTRÉES (frais scolaires encaissés) vs SORTIES (dépenses
     payées) -> solde de trésorerie. Graphiques entrées/sorties dans le temps, KPI solde courant.

RÈGLES FRONT
- Workflow strict (boutons conditionnés au statut + rôle) ; afficher le motif de rejet. Contrôle budgétaire
  visible à l'approbation. Catégories en arbre. Montants/devises formatés. Soft delete (BROUILLON).
DÉFINITION DE TERMINÉ : CRUD dépenses + workflow complet (soumettre/approuver/rejeter/payer) + justificatif ;
catégories (arbre) ; fournisseurs ; budgets + suivi (dépassement) ; rapports (catégorie/mois/fournisseur) ;
synthèse trésorerie (entrées vs sorties) ; i18n.
```

---

## Récapitulatif — un front par module

| Module back | Prompt front | Accès principal |
|-------------|--------------|-----------------|
| platform + billing-subscription | (livré séparément : home publique + abonnement + dashboard + admin éditeur) | SUPER_ADMIN / ADMIN_ECOLE |
| iam | Partie 1 | ADMIN_ECOLE |
| students | Partie 1 | SECRETAIRE / ADMIN |
| staff | Partie 1 | DIRECTEUR / ADMIN |
| academics | Partie 1 | DIRECTEUR / SECRETAIRE |
| attendance | Partie 1 | ENSEIGNANT / PARENT |
| grading | Partie 2 | ENSEIGNANT / ADMIN |
| billing | Partie 2 | COMPTABLE / PARENT |
| payments | Partie 2 | PARENT / COMPTABLE |
| webhooks | Partie 2 | ADMIN |
| report-card | Partie 2 | staff / PARENT |
| communication | Partie 2 | ADMIN / SECRETAIRE |
| expenses | Partie 2 | COMPTABLE / DIRECTEUR |

> Chaque module se construit comme un sous-ensemble cohérent de l'app (routes, écrans, hooks de données),
> branché à l'AppShell commun et au client OpenAPI typé. Livrer module par module, en respectant les gardes
> de rôle, le statut de l'école, et la séparation stricte abonnement plateforme / frais scolaires.
