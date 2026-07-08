-- Module platform: SaaS subscription tables (editor billing, NOT school fees)

CREATE TABLE tenant (
    id                   BIGSERIAL PRIMARY KEY,
    school_name          VARCHAR(255) NOT NULL,
    logo                 VARCHAR(500),
    brand_color          VARCHAR(50),
    address              VARCHAR(500),
    city                 VARCHAR(255),
    country              VARCHAR(255),
    phone                VARCHAR(50),
    email                VARCHAR(255),
    website              VARCHAR(500),
    director             VARCHAR(255),
    current_academic_year VARCHAR(50),
    status               VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE plan (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    code           VARCHAR(50)  NOT NULL UNIQUE,
    student_limit  INTEGER,
    monthly_price  NUMERIC(12, 2),
    annual_price   NUMERIC(12, 2),
    description    TEXT
);

CREATE TABLE subscription (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT        NOT NULL REFERENCES tenant(id),
    plan_id      BIGINT        NOT NULL REFERENCES plan(id),
    start_date   DATE          NOT NULL,
    end_date     DATE          NOT NULL,
    status       VARCHAR(50)   NOT NULL DEFAULT 'PENDING',
    auto_renewal BOOLEAN       NOT NULL DEFAULT FALSE,
    amount       NUMERIC(12, 2) NOT NULL,
    created_at   TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE subscription_invoice (
    id                BIGSERIAL PRIMARY KEY,
    subscription_id   BIGINT        NOT NULL REFERENCES subscription(id),
    reference         VARCHAR(100)  NOT NULL UNIQUE,
    billing_period    VARCHAR(50)   NOT NULL,
    period_start      DATE          NOT NULL,
    period_end        DATE          NOT NULL,
    net_amount        NUMERIC(12, 2) NOT NULL,
    tax_amount        NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_amount      NUMERIC(12, 2) NOT NULL,
    paid_amount       NUMERIC(12, 2) NOT NULL DEFAULT 0,
    remaining_balance NUMERIC(12, 2) NOT NULL,
    status            VARCHAR(50)   NOT NULL DEFAULT 'DRAFT',
    issued_at         DATE          NOT NULL,
    due_date          DATE          NOT NULL,
    pdf_file          VARCHAR(500)
);

CREATE TABLE subscription_payment (
    id                     BIGSERIAL PRIMARY KEY,
    invoice_id             BIGINT        NOT NULL REFERENCES subscription_invoice(id),
    reference              VARCHAR(100)  NOT NULL UNIQUE,
    amount                 NUMERIC(12, 2) NOT NULL,
    channel                VARCHAR(50)   NOT NULL,
    payment_type           VARCHAR(50)   NOT NULL,
    status                 VARCHAR(50)   NOT NULL DEFAULT 'INITIATED',
    external_transaction_id VARCHAR(255),
    paid_at                TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE subscription_receipt (
    id          BIGSERIAL PRIMARY KEY,
    payment_id  BIGINT        NOT NULL UNIQUE REFERENCES subscription_payment(id),
    reference   VARCHAR(100)  NOT NULL UNIQUE,
    amount      NUMERIC(12, 2) NOT NULL,
    pdf_file    VARCHAR(500),
    generated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE plan_change (
    id               BIGSERIAL PRIMARY KEY,
    subscription_id  BIGINT        NOT NULL REFERENCES subscription(id),
    old_plan_id      BIGINT        NOT NULL REFERENCES plan(id),
    new_plan_id      BIGINT        NOT NULL REFERENCES plan(id),
    type             VARCHAR(50)   NOT NULL,
    prorated_amount  NUMERIC(12, 2),
    effective_date   DATE          NOT NULL,
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- No default plans are seeded: the SUPER_ADMIN creates the plans from the platform UI.
