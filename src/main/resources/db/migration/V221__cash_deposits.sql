-- Dépôt de fonds comptable → fondateur, avec validation explicite et SMS bidirectionnel (§5 cahier des
-- charges). Jamais comptabilisé avant la validation du fondateur (status PENDING jusqu'à décision).
CREATE TABLE cash_deposits (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    accountant_id BIGINT NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'GNF',
    method VARCHAR(20) NOT NULL,
    destination_account TEXT,
    deposited_at TIMESTAMP NOT NULL,
    transaction_reference TEXT,
    comment TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    validated_by BIGINT,
    validated_at TIMESTAMP,
    rejection_reason TEXT,
    idempotency_key VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_cash_deposits_tenant ON cash_deposits (tenant_id);
CREATE INDEX idx_cash_deposits_status ON cash_deposits (tenant_id, status);
-- Empêche les doubles soumissions (§5.1) : rejouer avec la même clé renvoie le dépôt déjà créé.
CREATE UNIQUE INDEX uq_cash_deposits_idempotency ON cash_deposits (tenant_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

INSERT INTO permissions (name, code) VALUES
    ('Enregistrer un dépôt de fonds au fondateur', 'depot:create'),
    ('Valider ou rejeter un dépôt de fonds', 'depot:valider'),
    ('Consulter les dépôts de fonds', 'depot:read')
ON CONFLICT (code) DO NOTHING;

-- SUPER_ADMIN : rattrapage (convention déjà en usage, cf. V190).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SUPER_ADMIN' AND p.code IN ('depot:create', 'depot:valider', 'depot:read')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- SCHOOL_ADMIN (fondateur) : valide/consulte ; peut aussi créer (établissement à comptable unique).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SCHOOL_ADMIN' AND p.code IN ('depot:create', 'depot:valider', 'depot:read')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- DIRECTOR : même rôle de validation que SCHOOL_ADMIN dans ce module (cf. convention établie V190).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'DIRECTOR' AND p.code IN ('depot:valider', 'depot:read')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ACCOUNTANT : crée et consulte ses dépôts, ne valide jamais (séparation des rôles, §5.3).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ACCOUNTANT' AND p.code IN ('depot:create', 'depot:read')
ON CONFLICT (role_id, permission_id) DO NOTHING;
