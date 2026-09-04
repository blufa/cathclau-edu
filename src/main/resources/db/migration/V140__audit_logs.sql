-- Journal d'audit transverse (DPE_ARCHITECTURE_ANALYSIS.md §16/§18) : trace les écritures sensibles
-- déjà identifiées par les événements applicatifs existants (connexion, rôles/permissions, cycle de
-- vie des écoles, paiements). Append-only — aucun endpoint de modification/suppression.
CREATE TABLE audit_logs (
    id             BIGSERIAL PRIMARY KEY,
    -- Nul pour les actions plateforme (ex. création d'un tenant, vue depuis le référentiel SUPER_ADMIN).
    tenant_id      BIGINT,
    utilisateur_id BIGINT,
    action         VARCHAR(60)  NOT NULL,
    ressource      VARCHAR(60)  NOT NULL,
    ressource_id   BIGINT,
    details        TEXT,
    -- Capture différée (nécessite de propager la requête HTTP jusqu'au listener asynchrone) :
    -- colonnes prêtes, non encore renseignées par cette première tranche.
    adresse_ip     VARCHAR(64),
    user_agent     VARCHAR(255),
    created_at     TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_logs_tenant ON audit_logs (tenant_id, created_at DESC);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
