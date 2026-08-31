-- Fondation du module de partenariat DPE/DCE (voir DPE_ARCHITECTURE_ANALYSIS.md §5-7) : périmètre
-- géographique de supervision, rattachement des écoles, affectation des comptes DPE_ADMIN.
CREATE TABLE prefectures (
    id          BIGSERIAL PRIMARY KEY,
    nom         VARCHAR(150) NOT NULL,
    -- DPE (Direction Préfectorale) ou DCE (Direction Communale, ex. communes de Conakry) : même
    -- modèle de données et même mécanique de périmètre, seul le libellé affiché change selon le type.
    type        VARCHAR(10)  NOT NULL DEFAULT 'DPE',
    region      VARCHAR(150),
    pays        VARCHAR(100) NOT NULL DEFAULT 'Guinée',
    latitude    NUMERIC(9,6),
    longitude   NUMERIC(9,6),
    statut      VARCHAR(15)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

-- Rattachement d'une école à une préfecture/commune (nullable : une école hors périmètre DPE n'a pas
-- de valeur). N'affecte jamais l'isolation tenant classique ni l'abonnement SaaS.
ALTER TABLE tenant ADD COLUMN prefecture_id BIGINT REFERENCES prefectures(id);
CREATE INDEX idx_tenant_prefecture ON tenant (prefecture_id);

-- Affectation d'un compte DPE_ADMIN à une ou plusieurs préfectures/communes (évolutif vers un
-- utilisateur couvrant plusieurs périmètres — DPE_ARCHITECTURE_ANALYSIS.md §7).
CREATE TABLE dpe_user_prefectures (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    prefecture_id BIGINT NOT NULL REFERENCES prefectures(id),
    UNIQUE (user_id, prefecture_id)
);
CREATE INDEX idx_dpe_user_prefectures_user ON dpe_user_prefectures (user_id);
