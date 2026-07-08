# Déploiement — VPS Contabo (cathclau-edu.com) — DEV + PROD

Deux environnements sur **un seul VPS**, un reverse-proxy **Caddy** partagé (HTTPS auto), images sur
**GHCR** (privées). `develop` → **dev**, `main` → **prod**.

```
Internet ─▶ Caddy :443 ─┬─ cathclau-edu.com / www   ─▶ laca-prod-frontend:3000
   (réseau « web »)     ├─ api.cathclau-edu.com      ─▶ laca-prod-backend:8284 ─▶ db prod
                        ├─ dev.cathclau-edu.com      ─▶ laca-dev-frontend:3000
                        └─ api-dev.cathclau-edu.com  ─▶ laca-dev-backend:8284  ─▶ db dev
```

## Pourquoi Caddy (et pas Nginx statique) ?
Le front est **Next.js (SSR)**, pas des fichiers statiques : il tourne dans un conteneur Node (`node server.js`).
Caddy fait le **reverse-proxy + HTTPS automatique** (Let's Encrypt) sans configuration de certificats.
Nginx marcherait aussi mais demanderait certbot + renouvellement manuel : Caddy est plus simple ici.

## 1. À installer sur le VPS (une seule fois)
```bash
apt update && apt install -y ca-certificates curl
curl -fsSL https://get.docker.com | sh          # Docker Engine + plugin compose
ufw allow 22 && ufw allow 80 && ufw allow 443 && ufw enable
docker network create web                        # réseau partagé proxy ↔ stacks
```
C'est tout : PostgreSQL, backend, front et Caddy tournent en conteneurs (rien d'autre à installer).

## 2. DNS (registrar)
Enregistrements **A** → IP du VPS : `@`, `www`, `api`, `dev`, `api-dev`.

## 3. Fichiers sur le VPS
```
/opt/lacaeducation/
  docker-compose.yml          # (depuis deploy/)
  .env.prod                   # depuis .env.prod.example, rempli — NON commité
  .env.dev                    # depuis .env.dev.example, rempli — NON commité
  proxy/
    docker-compose.yml        # (depuis deploy/proxy/)
    Caddyfile                 # (depuis deploy/proxy/)
```
```bash
mkdir -p /opt/lacaeducation/proxy && cd /opt/lacaeducation
# scp deploy/docker-compose.yml, deploy/proxy/* et les .env.*.example depuis votre poste
cp .env.prod.example .env.prod && nano .env.prod   # remplir les secrets PROD
cp .env.dev.example  .env.dev  && nano .env.dev    # remplir les secrets DEV
# Connexion GHCR (images privées) — PAT GitHub avec scope read:packages
echo "GHCR_PAT" | docker login ghcr.io -u VOTRE_USER_GITHUB --password-stdin
```

## 4. Secrets GitHub (dans les DEUX dépôts → Settings ▸ Secrets ▸ Actions)
| Secret        | Description                          |
|---------------|--------------------------------------|
| `VPS_HOST`    | IP du VPS                            |
| `VPS_USER`    | utilisateur SSH                     |
| `VPS_SSH_KEY` | clé privée SSH autorisée sur le VPS |

GHCR utilise `GITHUB_TOKEN` automatiquement pour **pousser** ; le VPS utilise le PAT ci-dessus pour **tirer**.

## 5. Premier démarrage
```bash
cd /opt/lacaeducation/proxy && docker compose -p proxy up -d          # Caddy (une fois)
cd /opt/lacaeducation
docker compose --env-file .env.prod -p laca-prod up -d                # stack PROD
docker compose --env-file .env.dev  -p laca-dev  up -d                # stack DEV
docker compose --env-file .env.prod -p laca-prod logs -f backend      # suivre Flyway/bootstrap
```
Le **SUPER_ADMIN** est créé au 1er démarrage à partir de `SUPER_ADMIN_EMAIL` / `SUPER_ADMIN_PASSWORD`.

## 6. CI/CD (automatique ensuite)
- **PR / autres branches** → `CI` (tests backend / lint+types front).
- **push `develop`** → build image `:dev` → `docker compose -p laca-dev pull/up`.
- **push `main`** → build image `:prod` → `docker compose -p laca-prod pull/up`.

## Différences dev / prod
| | dev (`develop`) | prod (`main`) |
|---|---|---|
| Profil Spring | (défaut) | `prod` |
| Schéma | `ddl-auto=update` | `ddl-auto=validate` (Flyway seul) |
| Seeds de test | oui (`db/seed`, super@admin.gn) | non |
| Djomy | sandbox | production |
| Swagger | activé | désactivé |

## Exploitation
```bash
docker compose --env-file .env.prod -p laca-prod ps
docker compose --env-file .env.prod -p laca-prod logs -f backend
# Sauvegarde base prod :
docker compose --env-file .env.prod -p laca-prod exec db \
  pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" > backup_$(date +%F).sql
```

## Mailing (prod) — checklist
Les emails (vérification, identifiants admin, notifications) partent via **SMTP réel**.
1. Choisir un fournisseur SMTP : **Brevo**, **Mailgun**, **SendGrid**, ou le SMTP de l'hébergeur.
2. Renseigner `MAIL_HOST` / `MAIL_PORT` (587) / `MAIL_USERNAME` / `MAIL_PASSWORD` dans `.env.prod`.
3. `MAIL_FROM` = expéditeur affiché — **doit** être une adresse/domaine **autorisé** par le fournisseur
   (ex. `no-reply@cathclau-edu.com`), sinon les mails sont rejetés.
4. **Délivrabilité (DNS)** : ajouter les enregistrements **SPF** et **DKIM** fournis par le prestataire
   au domaine `cathclau-edu.com` (sinon les mails tombent en spam ou sont refusés).
5. Tester : créer une école → l'admin doit recevoir l'email d'identifiants.

## Paiement des frais scolaires (Djomy) — checklist prod
1. `DJOMY_BASE_URL=https://api.djomy.africa`, `DJOMY_CLIENT_ID/SECRET` **de production**.
2. Dans l'espace Djomy, configurer l'URL de webhook :
   `https://api.cathclau-edu.com/api/v1/webhooks/paiement/djomy` (déjà passée en env `DJOMY_WEBHOOK_URL`).
3. `DJOMY_WEBHOOK_SECRET` = clé de signature Djomy (sinon le `client-secret` est utilisé).
4. L'endpoint webhook est **public** (hors JWT) et vérifie la **signature** — ne pas le protéger derrière une auth.
5. Tester un paiement de frais bout-en-bout : initiation → redirection Djomy → retour → webhook → reçu.
