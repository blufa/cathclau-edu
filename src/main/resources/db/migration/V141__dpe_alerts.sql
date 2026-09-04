-- Alertes DPE/DCE (features/dpe.md §13, DPE_ARCHITECTURE_ANALYSIS.md §15). Une ligne évolue dans le
-- temps plutôt que d'être recréée à chaque évaluation : le statut et resolved_at portent l'historique
-- de la résolution ; la condition qui a déclenché l'alerte est réévaluée périodiquement (job) et peut
-- rouvrir une alerte déjà résolue si elle réapparaît.
CREATE TABLE dpe_alerts (
    id                   BIGSERIAL PRIMARY KEY,
    prefecture_id        BIGINT       NOT NULL REFERENCES prefectures(id),
    tenant_id            BIGINT       NOT NULL,
    type                 VARCHAR(40)  NOT NULL,
    priorite             VARCHAR(10)  NOT NULL,
    description          VARCHAR(500) NOT NULL,
    statut               VARCHAR(15)  NOT NULL DEFAULT 'NOUVELLE',
    created_at           TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP,
    resolved_at          TIMESTAMP,
    resolved_by_user_id  BIGINT,
    UNIQUE (prefecture_id, tenant_id, type)
);
CREATE INDEX idx_dpe_alerts_prefecture_statut ON dpe_alerts (prefecture_id, statut);
