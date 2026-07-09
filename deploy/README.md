# Déploiement pas-à-pas — cathclau-edu.com (Hostinger + VPS Contabo)

Ce guide part de zéro. Domaine acheté sur **Hostinger**, serveur **VPS Contabo**. Objectif :
deux environnements (dev + prod), HTTPS automatique, déploiement automatique à chaque `git push`.

```
Internet ─▶ Caddy :443 (HTTPS auto) ─┬─ cathclau-edu.com / www   ─▶ front PROD
   (sur le VPS)                       ├─ api.cathclau-edu.com      ─▶ backend PROD ─▶ PostgreSQL
                                      ├─ dev.cathclau-edu.com      ─▶ front DEV
                                      └─ api-dev.cathclau-edu.com  ─▶ backend DEV  ─▶ PostgreSQL
```  
Tout tourne en conteneurs Docker sur le VPS. Le domaine (Hostinger) pointe simplement vers l'IP du VPS.

---

## ÉTAPE 1 — Configurer le domaine sur Hostinger (DNS)

But : faire pointer `cathclau-edu.com` (et ses sous-domaines) vers **l'adresse IP de votre VPS Contabo**.

1. Récupérez **l'IP publique** de votre VPS dans le panneau Contabo (ex. `194.x.x.x`).
2. Connectez-vous à **hPanel Hostinger** → **Domaines** → cliquez sur `cathclau-edu.com`.
3. Ouvrez **DNS / Nameservers** → onglet **Enregistrements DNS** (Zone DNS).
   - Vérifiez d'abord que les **nameservers** sont ceux de Hostinger (`ns1.dns-parking.com` / `ns2...`).
     Si oui, vous gérez le DNS **ici même** (ne changez pas les nameservers).
4. **Supprimez** les enregistrements A par défaut sur `@` et `www` qui pointent vers le parking Hostinger.
5. **Ajoutez** ces 5 enregistrements **A** (Type = A, TTL = 3600 ou par défaut) :

   | Type | Nom (Host) | Pointe vers (Points to) |
   |------|------------|-------------------------|
   | A    | `@`        | IP_DU_VPS               |
   | A    | `www`      | IP_DU_VPS               |
   | A    | `api`      | IP_DU_VPS               |
   | A    | `dev`      | IP_DU_VPS               |
   | A    | `api-dev`  | IP_DU_VPS               |

6. Enregistrez. La propagation DNS prend de quelques minutes à quelques heures.
   Vérifier : `ping api.cathclau-edu.com` doit renvoyer l'IP du VPS.

> ⚠️ N'activez PAS d'« hébergement web » Hostinger pour ce domaine : le site est servi par le VPS,
> Hostinger ne fait **que le DNS**.
>
> 📧 Plus tard (mailing) : c'est aussi ici que vous ajouterez les enregistrements **SPF** et **DKIM**
> donnés par votre fournisseur d'email (voir la checklist Mailing plus bas).

---

## ÉTAPE 2 — Préparer le VPS Contabo

Depuis votre terminal, connectez-vous en SSH (l'IP et le mot de passe root sont dans l'email Contabo) :
```bash
ssh root@IP_DU_VPS
```

Installez Docker et ouvrez le pare-feu (copier-coller tel quel) :
```bash
apt update && apt install -y ca-certificates curl
curl -fsSL https://get.docker.com | sh            # installe Docker + docker compose
ufw allow 22 && ufw allow 80 && ufw allow 443 && ufw --force enable
docker network create web                          # réseau partagé proxy <-> applications
mkdir -p /opt/lacaeducation/proxy
```
Vérifier : `docker --version` et `docker compose version` doivent répondre.
**Rien d'autre à installer** (pas de Java, Node, PostgreSQL ni Nginx : tout est conteneurisé).

---

## ÉTAPE 3 — Déposer les fichiers de configuration sur le VPS

Ces fichiers sont dans le dépôt **backend** (dossier `deploy/`). Depuis **votre PC** (pas le VPS),
dans le dossier du dépôt backend :
```bash
cd chemin/vers/lacaeducation
scp deploy/docker-compose.yml            root@IP_DU_VPS:/opt/lacaeducation/
scp deploy/.env.prod.example deploy/.env.dev.example root@IP_DU_VPS:/opt/lacaeducation/
scp deploy/proxy/docker-compose.yml deploy/proxy/Caddyfile root@IP_DU_VPS:/opt/lacaeducation/proxy/
```

### Fichiers de variables d'environnement à créer (sur le VPS)
De retour sur le VPS, créez les vrais fichiers de secrets à partir des exemples :
```bash
ssh root@IP_DU_VPS
cd /opt/lacaeducation
cp .env.prod.example .env.prod
cp .env.dev.example  .env.dev
nano .env.prod        # remplir, puis Ctrl+O, Entrée, Ctrl+X
nano .env.dev
```
À remplir dans **`.env.prod`** (le plus important) :
- `GHCR_OWNER` = votre nom d'utilisateur/organisation GitHub **en minuscules** (ex. `blufa`).
- `POSTGRES_PASSWORD` = un mot de passe fort.
- `JWT_SECRET` (≥ 32 caractères), `WEBHOOK_SECRET`, `APP_QR_SECRET` = valeurs aléatoires.
- `SUPER_ADMIN_EMAIL` / `SUPER_ADMIN_PASSWORD` = votre 1er compte super-admin (créé au démarrage).
- `MAIL_*` + `MAIL_FROM` = votre SMTP réel (voir checklist Mailing).
- `CLOUDINARY_*` = vos identifiants Cloudinary (stockage des photos/PDF).
- `DJOMY_*` = identifiants **production** Djomy (`DJOMY_BASE_URL=https://api.djomy.africa`).

> Ces fichiers `.env.prod` / `.env.dev` **restent sur le VPS** et ne sont jamais commités (déjà dans `.gitignore`).

### Connexion à GHCR (images Docker privées)
Le VPS doit pouvoir télécharger les images depuis GitHub Container Registry :
```bash
# PAT = jeton GitHub avec le scope « read:packages » (créé sur github.com/settings/tokens)
echo "VOTRE_PAT_GITHUB" | docker login ghcr.io -u VOTRE_USER_GITHUB --password-stdin
```

---

## ÉTAPE 4 — Configurer GitHub Actions (dans les DEUX dépôts)

### a) Secrets (obligatoire)
Dans chaque dépôt (backend **et** front) : **Settings ▸ Secrets and variables ▸ Actions ▸ New repository secret**.
Créez ces 3 secrets :

| Secret        | Valeur                                                        |
|---------------|--------------------------------------------------------------|
| `VPS_HOST`    | l'IP du VPS                                                   |
| `VPS_USER`    | `root` (ou l'utilisateur SSH de déploiement)                 |
| `VPS_SSH_KEY` | la **clé privée SSH** autorisée sur le VPS (voir ci-dessous) |

Générer la clé SSH de déploiement (sur votre PC) :
```bash
ssh-keygen -t ed25519 -C "github-deploy" -f ~/.ssh/laca_deploy -N ""
ssh-copy-id -i ~/.ssh/laca_deploy.pub root@IP_DU_VPS    # autorise la clé sur le VPS
cat ~/.ssh/laca_deploy                                   # -> à coller dans le secret VPS_SSH_KEY
```

### b) Autoriser GitHub Actions à publier les images
Rien à faire : le workflow utilise le `GITHUB_TOKEN` intégré (permission `packages: write` déjà déclarée).
Après le 1er build, allez rendre les packages **privés** si besoin (Profil/Org ▸ Packages ▸ Package settings).

### c) Comment marche le pipeline
- **Pull request / autre branche** → workflow **CI** : tests backend / lint+types+build front.
- **push sur `develop`** → workflow **Deploy** : tests → build image `:dev` → push GHCR → SSH sur le VPS
  → `docker compose ... -p laca-dev up -d` → **environnement dev** (`dev.cathclau-edu.com`).
- **push sur `main`** → idem en `:prod` → **environnement prod** (`cathclau-edu.com`).

Vous n'avez donc **rien à lancer à la main** pour déployer : il suffit de merger dans `develop` puis `main`.

---

## ÉTAPE 5 — Premier démarrage

1. Démarrer le reverse-proxy **une fois** (sur le VPS) :
   ```bash
   cd /opt/lacaeducation/proxy && docker compose -p proxy up -d
   ```
2. Fusionner `chore/deploy-cicd` → **`develop`** dans les 2 dépôts (via Pull Request GitHub).
   → GitHub Actions construit et déploie automatiquement l'environnement **dev**.
3. Suivre le démarrage :
   ```bash
   cd /opt/lacaeducation
   docker compose --env-file .env.dev -p laca-dev logs -f backend
   ```
   Caddy obtient les certificats HTTPS tout seul au 1er accès. Le **SUPER_ADMIN** est créé au démarrage.
4. Quand le dev est validé → fusionner `develop` → **`main`** = mise en **production**.

---

## Différences dev / prod (automatique selon la branche)
| | dev (`develop`) | prod (`main`) |
|---|---|---|
| Domaines | dev. / api-dev.cathclau-edu.com | cathclau-edu.com / api. |
| Profil Spring | défaut | `prod` |
| Schéma base | `ddl-auto=update` | `ddl-auto=validate` (Flyway seul) |
| Données de test | oui (super@admin.gn) | non (super-admin via env) |
| Djomy | sandbox | production |
| Swagger | activé | désactivé |

---

## Exploitation courante
```bash
cd /opt/lacaeducation
docker compose --env-file .env.prod -p laca-prod ps            # état des conteneurs
docker compose --env-file .env.prod -p laca-prod logs -f backend
# Sauvegarde de la base de prod :
docker compose --env-file .env.prod -p laca-prod exec db \
  pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" > backup_$(date +%F).sql
```

## Checklist Mailing (prod)
1. Fournisseur SMTP : **Brevo**, **Mailgun**, **SendGrid** ou le SMTP de votre hébergeur.
2. Remplir `MAIL_HOST` / `MAIL_PORT` (587) / `MAIL_USERNAME` / `MAIL_PASSWORD` dans `.env.prod`.
3. `MAIL_FROM` = expéditeur **autorisé** par le fournisseur (ex. `no-reply@cathclau-edu.com`).
4. **SPF + DKIM** : ajouter sur Hostinger (Zone DNS) les enregistrements fournis par le prestataire,
   sinon les mails partent en spam ou sont refusés.
5. Tester : créer une école → l'admin doit recevoir l'email d'identifiants.

## Checklist Paiement des frais scolaires (Djomy, prod)
1. `DJOMY_BASE_URL=https://api.djomy.africa` + `DJOMY_CLIENT_ID/SECRET` **de production**.
2. Dans le tableau de bord Djomy, déclarer le webhook :
   `https://api.cathclau-edu.com/api/v1/webhooks/paiement/djomy` (orthographe FR « paiement »).
3. `DJOMY_WEBHOOK_SECRET` = clé de signature Djomy.
4. L'endpoint webhook est **public** (hors JWT) et vérifie la **signature** — ne pas le protéger.
5. Tester un paiement de frais bout-en-bout : initiation → redirection → retour → webhook → reçu.
