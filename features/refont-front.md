# Mission — Reproduction fidèle du template Vankine

Je veux reproduire fidèlement dans mon projet frontend le design et les pages du template Vankine Next.js disponible ici :

### Pages de référence

**Home 5**
https://vankine-nextjs.vercel.app/home-5

**Service Details**
https://vankine-nextjs.vercel.app/service-details

**Blog Details — Right Sidebar**
https://vankine-nextjs.vercel.app/blog-details-right-sidebar

Le template Vankine est déjà présent dans le projet frontend que je t'ai fourni.

---

# OBJECTIF

Je veux que tu utilises le template existant comme base et que tu reproduises fidèlement les trois pages de référence :

1. `/home-5`
2. `/service-details`
3. `/blog-details-right-sidebar`

Le rendu doit être **très proche visuellement du site de référence**, et pas simplement inspiré de celui-ci.

---

# IMPORTANT — IDENTITÉ VISUELLE

J'aime particulièrement l'identité visuelle du template Vankine.

Il faut donc accorder une attention particulière à :

* la police utilisée ;
* les familles de fonts ;
* les weights ;
* les tailles de texte ;
* les line-height ;
* le letter-spacing ;
* les titres ;
* les sous-titres ;
* les paragraphes ;
* les couleurs ;
* les boutons ;
* les bordures ;
* les border-radius ;
* les ombres ;
* les espacements ;
* les paddings ;
* les marges ;
* les largeurs maximales ;
* les containers ;
* les backgrounds ;
* les éléments décoratifs ;
* les icônes ;
* les animations ;
* les transitions.

**Ne remplace pas la police du template par une police générique.**

Identifie précisément les fonts utilisées par le template existant et réutilise-les.

Si les fonts sont déjà présentes dans le projet, utilise les fichiers existants.

Si elles sont importées via Google Fonts ou une autre méthode dans le template, conserve cette configuration lorsqu'elle est compatible avec le projet.

---

# ÉTAPE 1 — ANALYSER LE PROJET EXISTANT

Avant toute modification :

Inspecte complètement le frontend.

Analyse :

* `package.json`
* structure `src`
* `app` / `pages`
* `components`
* `layouts`
* `styles`
* fichiers CSS/SCSS
* Tailwind s'il est utilisé
* fonts
* images
* SVG
* icônes
* animations
* composants Header
* composants Footer
* composants Button
* composants Card
* composants Breadcrumb
* composants Sidebar
* composants Blog
* composants Service.

Identifie les composants du template Vankine qui peuvent être réutilisés.

**Ne reconstruis pas ce qui existe déjà.**

---

# ÉTAPE 2 — ANALYSER LES 3 PAGES DE RÉFÉRENCE

Analyse séparément :

## PAGE 1 — HOME 5

https://vankine-nextjs.vercel.app/home-5

Reproduis :

* Header
* navigation
* hero
* toutes les sections
* cartes
* statistiques
* services
* contenus
* CTA
* footer
* animations
* éléments décoratifs.

Respecte exactement l'ordre des sections.

**Aucune section ne doit être supprimée ou simplifiée.**

---

## PAGE 2 — SERVICE DETAILS

https://vankine-nextjs.vercel.app/service-details

Reproduis intégralement :

* Header
* breadcrumb / page title
* contenu principal
* image(s)
* titre(s)
* paragraphes
* listes
* boutons
* éléments graphiques
* sidebar éventuelle
* autres services / navigation éventuelle
* CTA
* footer.

Le contenu doit respecter la structure et la hiérarchie visuelle de la référence.

---

## PAGE 3 — BLOG DETAILS RIGHT SIDEBAR

https://vankine-nextjs.vercel.app/blog-details-right-sidebar

Reproduis intégralement :

* Header
* breadcrumb
* titre de l'article
* informations de l'article
* image principale
* contenu
* titres H2/H3
* paragraphes
* listes
* citations éventuelles
* images éventuelles
* tags
* partage social
* navigation article
* commentaires s'ils existent
* sidebar droite
* recherche
* catégories
* articles récents
* CTA éventuel
* footer.

La **sidebar droite doit être reproduite fidèlement**, avec les mêmes proportions, espacements et hiérarchie visuelle.

---

# ÉTAPE 3 — DESIGN SYSTEM GLOBAL

Les trois pages doivent utiliser **le même design system**.

Ne crée pas trois designs indépendants.

Identifie et centralise si possible :

### Typography

* font-family principale
* font-family secondaire si présente
* font-weight
* font-size
* line-height
* letter-spacing.

Définis une hiérarchie cohérente pour :

* H1
* H2
* H3
* H4
* body
* small text
* labels
* buttons
* navigation.

### Couleurs

Identifie les couleurs principales du template :

* primary
* secondary
* accent
* texte
* texte secondaire
* background
* border
* hover.

Utilise les mêmes valeurs sur les trois pages.

### Layout

Respecte :

* largeur du container ;
* max-width ;
* gutters ;
* grid ;
* colonnes ;
* gaps ;
* responsive breakpoints.

---

# ÉTAPE 4 — COMPOSANTS RÉUTILISABLES

Crée ou réutilise des composants communs lorsque cela est pertinent.

Par exemple :

* Header
* MobileMenu
* Footer
* PageBanner
* Breadcrumb
* SectionTitle
* Button
* ServiceCard
* BlogCard
* SidebarWidget
* SocialLinks
* CTA.

Mais **ne crée pas artificiellement des composants pour chaque petit élément**.

Respecte l'architecture déjà présente dans le template.

---

# ÉTAPE 5 — ASSETS

Utilise en priorité les assets déjà présents dans le projet.

Recherche dans tous les répertoires pertinents :

* `/public`
* `/assets`
* `/src/assets`
* `/images`
* etc.

Associe les images du template aux bons fichiers.

**Ne remplace pas les images existantes par des placeholders.**

Ne mets pas :

* `placeholder.jpg`
* images aléatoires
* Lorem ipsum
* icônes approximatives

lorsque les ressources correspondantes sont disponibles dans le template.

---

# ÉTAPE 6 — RESPONSIVE

Les trois pages doivent être parfaitement exploitables sur :

### Desktop

1440px
1280px
1024px

### Tablet

768px

### Mobile

414px
390px
375px

Vérifie notamment :

* navigation ;
* menu mobile ;
* hero ;
* titres ;
* images ;
* cartes ;
* colonnes ;
* sidebar ;
* boutons ;
* paddings ;
* marges ;
* font-size ;
* line-height ;
* overflow.

Sur mobile, la sidebar de la page blog doit être adaptée correctement au responsive du template.

Aucun scroll horizontal involontaire.

---

# ÉTAPE 7 — NE PAS CASSER LE PROJET

Ne change pas inutilement :

* framework ;
* architecture ;
* configuration ;
* dépendances ;
* routes existantes.

Ne supprime pas les fonctionnalités existantes.

Ne casse pas les autres pages.

Avant de modifier un composant partagé, vérifie quelles pages l'utilisent.

---

# ÉTAPE 8 — VÉRIFICATION VISUELLE

Après avoir développé les pages, lance le projet.

Vérifie chaque page contre sa référence :

HOME :
https://vankine-nextjs.vercel.app/home-5

SERVICE :
https://vankine-nextjs.vercel.app/service-details

BLOG :
https://vankine-nextjs.vercel.app/blog-details-right-sidebar

La vérification doit être visuelle.

Compare notamment :

* position des éléments ;
* largeur des containers ;
* hauteur des sections ;
* typographie ;
* police ;
* poids des fonts ;
* tailles ;
* couleurs ;
* espacements ;
* images ;
* boutons ;
* icônes ;
* sidebar ;
* responsive.

Si une différence importante est détectée, corrige-la.

Ne considère pas le travail terminé simplement parce que le projet compile.

---

# ÉTAPE 9 — PIXEL PERFECT

Je privilégie la fidélité visuelle plutôt que la rapidité.

Pour chaque page :

1. implémente ;
2. lance le projet ;
3. prends/inspecte le rendu ;
4. compare avec la référence ;
5. identifie les différences ;
6. corrige ;
7. vérifie à nouveau.

Accorde une attention particulière à la typographie.

**La police, les font weights, les tailles, les line-heights et les espacements doivent donner exactement la même sensation visuelle que Vankine.**

---

# RÈGLES ABSOLUES

* Ne pars pas de zéro.
* Le template est déjà dans le projet.
* Réutilise ses composants.
* Réutilise ses assets.
* Réutilise ses fonts.
* Ne simplifie aucune page.
* Ne supprime aucune section.
* Ne mets aucun placeholder inutile.
* Ne mets pas de Lorem ipsum.
* Ne crée pas trois systèmes de design différents.
* Les trois pages doivent partager le même design system.
* Ne casse pas les autres pages.
* Ne change pas la stack sans nécessité.
* Vérifie réellement le rendu avant de considérer la tâche terminée.

---

# ORDRE D'EXÉCUTION

Travaille dans cet ordre :

### 1.

Analyser le projet existant.

### 2.

Identifier les composants et assets Vankine déjà disponibles.

### 3.

Identifier précisément les fonts et le système typographique.

### 4.

Identifier le design system global.

### 5.

Implémenter `home-5`.

### 6.

Implémenter `service-details`.

### 7.

Implémenter `blog-details-right-sidebar`.

### 8.

Factoriser les composants communs.

### 9.

Tester desktop.

### 10.

Tester mobile.

### 11.

Comparer visuellement aux trois références.

### 12.

Corriger les différences.

### 13.

Vérifier qu'aucune autre page du projet n'a été cassée.

---

# LIVRABLE FINAL

À la fin, donne-moi uniquement un résumé clair :

* pages implémentées ;
* composants créés/réutilisés ;
* fonts utilisées ;
* assets réutilisés ;
* fichiers principaux modifiés ;
* tests effectués ;
* problèmes éventuels.


# PAGES SUPPLÉMENTAIRES À REPRODUIRE

Le projet doit également reproduire fidèlement les pages suivantes du template Vankine :

### PAGE 4 — ABOUT US

Référence :
https://vankine-nextjs.vercel.app/about-us

Reproduis intégralement la page, notamment :

* Header
* Hero / Page Banner
* Breadcrumb
* Section de présentation / About
* Images
* Titres et sous-titres
* Paragraphes
* Statistiques
* Services ou fonctionnalités présentés
* Éléments graphiques
* CTA
* Footer
* Toutes les sections présentes sur la page de référence.

Respecte exactement la hiérarchie visuelle, les espacements, la typographie, les images, les couleurs et la structure du template.

---

### PAGE 5 — PRICING

Référence :
https://vankine-nextjs.vercel.app/pricing

Reproduis intégralement la page Pricing.

Respecte notamment :

* Header
* Page Banner / Breadcrumb
* Titre de la page
* Introduction
* Cartes de pricing
* Noms des plans
* Prix
* Périodicité
* Liste des fonctionnalités
* Boutons
* Plan mis en avant
* Badges éventuels
* États hover
* Alignements
* Espacements
* CTA
* Footer.

Les cartes de pricing doivent conserver exactement les proportions, bordures, rayons, ombres, typographies et espacements du template.

Si la page contient un système de sélection ou de bascule (par exemple mensuel/annuel), reproduis également son comportement.

---

### PAGE 6 — FAQS

Référence :
https://vankine-nextjs.vercel.app/faqs

Reproduis intégralement la page FAQ.

Respecte notamment :

* Header
* Page Banner
* Breadcrumb
* Titre
* Introduction
* Liste des questions
* Accordéon FAQ
* États ouvert/fermé
* Icônes
* Animations
* Sections supplémentaires
* CTA
* Footer.

Le comportement de l'accordéon doit être fonctionnel.

Lorsqu'une question est ouverte :

* l'icône doit changer si le template le prévoit ;
* la réponse doit apparaître avec l'animation appropriée ;
* les espacements doivent correspondre au template.

---

# COHÉRENCE ENTRE LES 6 PAGES

Ces six pages appartiennent au même site et doivent donc partager exactement la même identité visuelle.

Pages concernées :

1. `/home-5`
2. `/service-details`
3. `/blog-details-right-sidebar`
4. `/about-us`
5. `/pricing`
6. `/faqs`

## DESIGN SYSTEM GLOBAL

Ne traite pas chaque page comme un projet indépendant.

Crée/réutilise un système commun pour :

### Typography

* font-family
* font-weight
* font-size
* line-height
* letter-spacing
* titres
* paragraphes
* navigation
* boutons
* labels.

### Layout

* container
* max-width
* gutters
* grids
* columns
* gaps
* sections
* paddings
* margins.

### UI

* boutons
* cartes
* bordures
* border-radius
* shadows
* badges
* icônes
* hover states
* transitions.

### Colors

Utilise les mêmes couleurs et variables sur toutes les pages.

---

# COMPOSANTS COMMUNS

Identifie les éléments récurrents entre les six pages et réutilise-les.

Par exemple :

* Header
* Navigation
* Mobile menu
* Footer
* Page Banner
* Breadcrumb
* Section Title
* Button
* CTA
* Cards
* Blog Card
* Service Card
* Pricing Card
* FAQ Accordion
* Sidebar
* Social Links.

Ne duplique pas le code lorsqu'un composant existant peut être réutilisé.

---

# IMPORTANT — FIDÉLITÉ AU TEMPLATE

Pour chacune des six pages, ne te contente pas de reproduire le contenu général.

Je veux reproduire **la sensation visuelle du template Vankine**.

Une attention particulière doit être portée à :

* la police ;
* la taille des titres ;
* le poids des titres ;
* le rendu des paragraphes ;
* les line-height ;
* les espacements entre sections ;
* les largeurs de contenu ;
* les images ;
* les boutons ;
* les couleurs ;
* les animations ;
* les détails graphiques.

**Ne remplace pas les fonts par Arial, Inter ou une autre police générique si ce n'est pas celle du template.**

---

# PRIORITÉ

Si tu dois choisir entre :

1. écrire rapidement beaucoup de code ;
2. obtenir un rendu visuellement fidèle ;

**privilégie toujours la fidélité visuelle.**

Le résultat final doit donner l'impression que les six pages appartiennent au même site Vankine.

---

# VÉRIFICATION FINALE

Après implémentation, vérifie chacune des six URLs de référence :

HOME 5
https://vankine-nextjs.vercel.app/home-5

SERVICE
https://vankine-nextjs.vercel.app/service-details

BLOG
https://vankine-nextjs.vercel.app/blog-details-right-sidebar

ABOUT
https://vankine-nextjs.vercel.app/about-us

PRICING
https://vankine-nextjs.vercel.app/pricing

FAQ
https://vankine-nextjs.vercel.app/faqs

Pour chaque page :

* vérifier toutes les sections ;
* vérifier la typographie ;
* vérifier les images ;
* vérifier les espacements ;
* vérifier les couleurs ;
* vérifier les boutons ;
* vérifier les interactions ;
* vérifier le responsive ;
* vérifier le mobile ;
* vérifier qu'aucune section n'est oubliée.

Ne considère pas le travail terminé simplement parce que `npm run build` ou `npm run dev` fonctionne.

**Le rendu visuel est le principal critère de réussite.**


**Commence par analyser le projet et le template existant. Ne modifie aucun fichier avant cette analyse.**
