-- Workflow de validation académique des bulletins (§10) : un workflow par (classe, période),
-- avec l'historique horodaté et signé de chaque étape franchie.
CREATE TABLE validation_workflows (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT      NOT NULL,
    class_id      BIGINT      NOT NULL,
    period_id     BIGINT      NOT NULL,
    current_stage VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP,
    CONSTRAINT uq_validation_workflow_class_period UNIQUE (tenant_id, class_id, period_id)
);

CREATE TABLE validation_steps (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT      NOT NULL,
    workflow_id BIGINT      NOT NULL REFERENCES validation_workflows(id),
    stage       VARCHAR(30) NOT NULL,
    action      VARCHAR(20) NOT NULL,
    actor_id    BIGINT,
    comment     VARCHAR(500),
    acted_at    TIMESTAMP   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_validation_steps_workflow ON validation_steps (workflow_id);
