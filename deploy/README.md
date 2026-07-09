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

> **Ce guide couvre les DEUX applications** (backend **et** frontend). Rôle de chaque dépôt :
> - **Dépôt backend** (`lacaeducation`) : contient la **stack de déploiement** (`deploy/` : docker-compose,
>   proxy Caddy, .env, ce guide) + son `Dockerfile` + son workflow. C'est **la source unique** du déploiement.
> - **Dépôt frontend** (`lacaeducation-ui`) : contient uniquement son `Dockerfile` + son workflow CI/CD.
>   Il pousse son image sur GHCR ; le `docker-compose.yml` (côté backend) l'utilise.
>
> Sur le VPS, une seule commande `docker compose up` démarre backend + frontend + base + proxy.

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

   | Type  | Nom (Host) | Pointe vers (Points to) |
   |-------|------------|-------------------------|
   | A     | `@`        | IP_DU_VPS               |
   | CNAME | `www`      | `cathclau-edu.com`      |
   | A     | `api`      | IP_DU_VPS               |
   | A     | `dev`      | IP_DU_VPS               |
   | A     | `api-dev`  | IP_DU_VPS               |

   > ⚠️ **`www`** : Hostinger crée souvent déjà un **CNAME `www → cathclau-edu.com`**. Gardez-le tel
   > quel (il suit l'apex `@`). On ne peut PAS avoir un CNAME **et** un A sur le même nom : n'ajoutez
   > donc pas d'enregistrement A pour `www` (sinon « conflit »). Alternative : supprimer le CNAME puis
   > ajouter un A `www → IP_DU_VPS`.

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

> ⚠️ **DNS des conteneurs (piège fréquent Contabo / `systemd-resolved`)** : sans ça, les conteneurs
> ne résolvent aucun domaine (`127.0.0.53:53 connection refused`) → **Caddy ne peut pas obtenir de
> certificat HTTPS** et le backend ne joint pas Cloudinary/Djomy/SMTP. Fixez le DNS Docker :
> ```bash
> mkdir -p /etc/docker
> cat > /etc/docker/daemon.json <<'EOF'
> { "dns": ["1.1.1.1", "8.8.8.8"] }
> EOF
> systemctl restart docker
> ```
Vérifier : `docker --version` et `docker compose version` doivent répondre.
**Rien d'autre à installer** (pas de Java, Node, PostgreSQL ni Nginx : tout est conteneurisé).

---

## ÉTAPE 3 — Déposer les fichiers de configuration sur le VPS

Deux méthodes. **La méthode B (créer les fichiers directement sur le VPS) est la plus fiable** — à
utiliser si le `scp` ne fonctionne pas ou si des fichiers n'arrivent pas au bon endroit.

### Méthode A — copie depuis votre PC (`scp`)
Depuis **votre PC** (pas le VPS), dans le dossier du dépôt backend :
```bash
cd chemin/vers/lacaeducation
scp deploy/docker-compose.yml            root@IP_DU_VPS:/opt/lacaeducation/
scp deploy/.env.prod.example deploy/.env.dev.example root@IP_DU_VPS:/opt/lacaeducation/
scp deploy/proxy/docker-compose.yml deploy/proxy/Caddyfile root@IP_DU_VPS:/opt/lacaeducation/proxy/
```

### Méthode B — créer les fichiers du proxy directement sur le VPS (recommandé)
> ⚠️ **Piège fréquent** : si `/opt/lacaeducation/proxy/docker-compose.yml` est absent, la commande
> `docker compose` **remonte au dossier parent** et lance par erreur le compose de l'application
> (d'où des dizaines de « variable not set » et `ghcr.io//...:`). Créez donc bien les fichiers du proxy.

Sur le **VPS**, copiez-collez ce bloc (crée `proxy/docker-compose.yml` + `proxy/Caddyfile`) :
```bash
mkdir -p /opt/lacaeducation/proxy
cat > /opt/lacaeducation/proxy/docker-compose.yml <<'EOF'
services:
  caddy:
    image: caddy:2-alpine
    container_name: proxy-caddy
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile:ro
      - caddy_data:/data
      - caddy_config:/config
    networks: [web]
volumes:
  caddy_data:
  caddy_config:
networks:
  web:
    external: true
    name: web
EOF

cat > /opt/lacaeducation/proxy/Caddyfile <<'EOF'
cathclau-edu.com, www.cathclau-edu.com {
	encode zstd gzip
	reverse_proxy laca-prod-frontend:3000
}
api.cathclau-edu.com {
	encode zstd gzip
	request_body {
		max_size 20MB
	}
	reverse_proxy laca-prod-backend:8284
}
dev.cathclau-edu.com {
	encode zstd gzip
	reverse_proxy laca-dev-frontend:3000
}
api-dev.cathclau-edu.com {
	encode zstd gzip
	request_body {
		max_size 20MB
	}
	reverse_proxy laca-dev-backend:8284
}
EOF
```
Puis créez le **`docker-compose.yml` de l'application** (racine `/opt/lacaeducation/`) — copiez-collez
ce bloc entier sur le VPS :
```bash
cat > /opt/lacaeducation/docker-compose.yml <<'COMPOSE_EOF'
services:
  db:
    image: postgres:16-alpine
    container_name: ${STACK}-db
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    ports:
      - "${DB_HOST_PORT:-5432}:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks: [internal]

  backend:
    image: ghcr.io/${GHCR_OWNER}/lacaeducation-backend:${BACKEND_TAG}
    container_name: ${STACK}-backend
    restart: unless-stopped
    depends_on:
      db:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILE}
      SERVER_PORT: "8284"
      DB_URL: jdbc:postgresql://db:5432/${POSTGRES_DB}
      DB_USER: ${POSTGRES_USER}
      DB_PASSWORD: ${POSTGRES_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      WEBHOOK_SECRET: ${WEBHOOK_SECRET}
      APP_QR_SECRET: ${APP_QR_SECRET}
      BOOTSTRAP_SUPERADMIN_EMAIL: ${SUPER_ADMIN_EMAIL}
      BOOTSTRAP_SUPERADMIN_PASSWORD: ${SUPER_ADMIN_PASSWORD}
      APP_CORS_ALLOWED_ORIGINS: ${FRONT_URL}
      VERIFICATION_URL: ${FRONT_URL}/verify-email
      APP_PLATFORM_VERIFICATION_URL: ${FRONT_URL}/verify-email
      APP_PLATFORM_LOGIN_URL: ${FRONT_URL}/login
      MAIL_HOST: ${MAIL_HOST}
      MAIL_PORT: ${MAIL_PORT}
      MAIL_USERNAME: ${MAIL_USERNAME}
      MAIL_PASSWORD: ${MAIL_PASSWORD}
      APP_MAIL_FROM: ${MAIL_FROM}
      CLOUDINARY_CLOUD_NAME: ${CLOUDINARY_CLOUD_NAME}
      CLOUDINARY_API_KEY: ${CLOUDINARY_API_KEY}
      CLOUDINARY_API_SECRET: ${CLOUDINARY_API_SECRET}
      DJOMY_CLIENT_ID: ${DJOMY_CLIENT_ID}
      DJOMY_CLIENT_SECRET: ${DJOMY_CLIENT_SECRET}
      DJOMY_BASE_URL: ${DJOMY_BASE_URL}
      DJOMY_WEBHOOK_SECRET: ${DJOMY_WEBHOOK_SECRET}
      DJOMY_RETURN_URL: ${FRONT_URL}/subscription/payment
      DJOMY_CANCEL_URL: ${FRONT_URL}/subscription/payment
    expose:
      - "8284"
    networks: [internal, web]

  frontend:
    image: ghcr.io/${GHCR_OWNER}/lacaeducation-frontend:${FRONTEND_TAG}
    container_name: ${STACK}-frontend
    restart: unless-stopped
    depends_on:
      - backend
    expose:
      - "3000"
    networks: [web]

volumes:
  pgdata:

networks:
  internal:
  web:
    external: true
    name: web
COMPOSE_EOF
```
Les fichiers `.env.prod` / `.env.dev` se créent à la section suivante.

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
**GHCR** = GitHub Container Registry (`ghcr.io`), le magasin d'images Docker de GitHub. La CI y publie
vos images en **privé** ; le VPS doit s'y **authentifier** pour les télécharger (`docker compose pull`).

On n'utilise PAS le mot de passe GitHub mais un **PAT** (jeton) avec la seule permission `read:packages` :
1. github.com → votre photo → **Settings** → **Developer settings** (tout en bas)
2. **Personal access tokens** → **Tokens (classic)** → **Generate new token (classic)**
3. Nom `vps-ghcr`, cochez **`read:packages`**, **Generate**, puis **copiez** le jeton (`ghp_…`).
4. Sur le VPS (remplacez le jeton et votre user GitHub, ex. `blufa`) :
```bash
echo "ghp_votreJeton" | docker login ghcr.io -u blufa --password-stdin
# -> doit afficher « Login Succeeded »
```
> Alternative : rendre les packages **publics** (Profil ▸ Packages ▸ Package settings ▸ Change visibility)
> — pas de `docker login` nécessaire, mais l'image backend devient téléchargeable par tous. Recommandé : privé + PAT.

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

1. Créer le réseau partagé (si pas déjà fait) et démarrer le reverse-proxy **une fois** (sur le VPS).
   On précise le fichier avec `-f` pour éviter que Compose remonte au dossier parent :
   ```bash
   docker network create web 2>/dev/null || true
   docker compose -f /opt/lacaeducation/proxy/docker-compose.yml -p proxy up -d
   docker compose -f /opt/lacaeducation/proxy/docker-compose.yml -p proxy ps   # doit lister « proxy-caddy »
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

### Dépannage — le proxy ne démarre pas (« port 80 already in use »)
Un autre serveur web (souvent **Apache** ou **nginx** pré-installé sur le VPS) occupe déjà le port 80/443.
```bash
ss -tlnp | grep -E ':80 |:443 '                 # identifier le service
systemctl stop apache2 nginx 2>/dev/null; systemctl disable apache2 nginx 2>/dev/null
docker compose -f /opt/lacaeducation/proxy/docker-compose.yml -p proxy up -d   # relancer
```

### Dépannage — « variable not set » / `ghcr.io//...:`
`docker compose` lancé sans trouver de fichier dans le dossier courant **remonte au parent** et prend le
mauvais compose. Toujours préciser le fichier avec `-f`, ou lancer l'app avec `--env-file` :
```bash
docker compose -f /opt/lacaeducation/proxy/docker-compose.yml -p proxy up -d
docker compose --env-file .env.prod -p laca-prod up -d
```

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

## Accès à la base depuis pgAdmin — connexion directe par l'IP
La base est exposée sur l'IP du VPS (prod = 5432, dev = 5433). Vous ajoutez simplement le serveur
dans pgAdmin avec l'IP + la base + les identifiants.

1. **Ouvrir le port** dans le pare-feu du VPS :
   ```bash
   # Option SÛRE (recommandée) : n'autoriser QUE votre IP (trouvez-la sur https://ifconfig.me)
   ufw allow from VOTRE_IP_PUBLIQUE to any port 5432 proto tcp
   ufw allow from VOTRE_IP_PUBLIQUE to any port 5433 proto tcp
   # Option simple (moins sûre : ouvert à tous) :
   # ufw allow 5432/tcp && ufw allow 5433/tcp
   ```
2. Dans **pgAdmin** → clic droit *Servers* → *Register → Server* :
   | Champ | Prod | Dev |
   |---|---|---|
   | Host | **IP_DU_VPS** | **IP_DU_VPS** |
   | Port | `5432` | `5433` |
   | Maintenance DB | `cathclau` | `cathclau_dev` |
   | Username | `cathclau` | `cathclau_dev` |
   | Password | (`POSTGRES_PASSWORD` du `.env.prod`) | (celui du `.env.dev`) |

> ⚠️ **SÉCURITÉ** : un port Postgres ouvert sur Internet est une cible. **Impératif** :
> mot de passe `POSTGRES_PASSWORD` **fort et unique**, et de préférence **restreindre à votre IP**
> (option sûre ci-dessus). Alternative 100 % privée : tunnel SSH
> `ssh -N -L 5432:127.0.0.1:5432 root@IP_DU_VPS` puis pgAdmin sur `127.0.0.1:5432`.
>
> Nécessite que la stack soit **déployée** (conteneur `db` en marche). Après mise à jour du compose,
> relancez : `docker compose --env-file .env.dev -p laca-dev up -d`.

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

---

## ✅ Checklist finale de vérification (à cocher)

Parcourez cette liste : chaque case a **la commande pour vérifier** que c'est bien fait.

### A. DNS (Hostinger)
- [ ] A `@` → IP du VPS · A `api` · A `dev` · A `api-dev` · CNAME `www` → `cathclau-edu.com`
  ```bash
  # depuis votre PC (remplacez par votre IP VPS) :
  dig +short cathclau-edu.com api.cathclau-edu.com dev.cathclau-edu.com api-dev.cathclau-edu.com
  ```

### B. VPS préparé
- [ ] Docker installé — `docker --version && docker compose version`
- [ ] Réseau partagé — `docker network ls | grep web`
- [ ] Aucun service sur 80/443 à part Caddy — `ss -tlnp | grep -E ':80 |:443 '` (ne doit montrer que `docker-proxy`)
- [ ] nginx/apache désactivés — `systemctl is-enabled nginx apache2 2>/dev/null` (doit dire `disabled`/absent)

### C. Fichiers présents sur le VPS
- [ ] `ls -la /opt/lacaeducation/` → **docker-compose.yml**, **.env.prod**, **.env.dev**
- [ ] `ls -la /opt/lacaeducation/proxy/` → **docker-compose.yml**, **Caddyfile**
- [ ] `.env.prod` et `.env.dev` remplis (pas de « CHANGER ») — `grep -c CHANGER /opt/lacaeducation/.env.prod` doit renvoyer `0`
- [ ] Le `docker-compose.yml` du VPS est **à jour** (contient `DB_HOST_PORT`) — `grep DB_HOST_PORT /opt/lacaeducation/docker-compose.yml`

### D. Accès aux images (GHCR)
- [ ] Connecté à GHCR — `cat ~/.docker/config.json | grep ghcr.io` (doit exister)

### E. GitHub (dans les 2 dépôts)
- [ ] Secrets `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY` créés (Settings ▸ Secrets ▸ Actions)

### F. Proxy démarré (une fois)
- [ ] `docker compose -f /opt/lacaeducation/proxy/docker-compose.yml -p proxy ps` → `proxy-caddy` **Up**

### G. Déploiement de l'app
- [ ] Merge `chore/deploy-cicd` → **`develop`** effectué (onglet **Actions** GitHub : workflow vert)
- [ ] `docker ps` sur le VPS montre `laca-dev-backend`, `laca-dev-frontend`, `laca-dev-db`
- [ ] Sites qui répondent : https://dev.cathclau-edu.com et https://api-dev.cathclau-edu.com
- [ ] (prod) Merge `develop` → **`main`** → `laca-prod-*` + https://cathclau-edu.com

### H. Base dans pgAdmin (optionnel)
- [ ] Tunnel : `ssh -N -L 5433:127.0.0.1:5433 root@IP_DU_VPS` puis pgAdmin `127.0.0.1:5433` (dev)

> Si une case ne passe pas, la section « Dépannage » (Étape 5) couvre les cas fréquents
> (port 80 occupé, « variable not set », dossier proxy vide).
