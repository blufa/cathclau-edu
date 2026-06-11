CREATE TABLE school_fees (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    class_id BIGINT,
    type VARCHAR(30) NOT NULL CHECK (type IN ('ENROLLMENT','RE_ENROLLMENT','MONTHLY','TRANSPORT','CANTEEN','EXAM')),
    amount NUMERIC(15,2) NOT NULL,
    academic_year VARCHAR(20) NOT NULL
);

CREATE TABLE installments (
    id BIGSERIAL PRIMARY KEY,
    fee_id BIGINT NOT NULL REFERENCES school_fees(id),
    label VARCHAR(255) NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    due_date DATE NOT NULL
);

CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    fee_id BIGINT REFERENCES school_fees(id),
    reference VARCHAR(100) NOT NULL UNIQUE,
    total_amount NUMERIC(15,2) NOT NULL,
    paid_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    remaining_balance NUMERIC(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('UNPAID','PARTIAL','PAID')),
    issue_date DATE NOT NULL
);

CREATE INDEX idx_school_fees_tenant ON school_fees(tenant_id);
CREATE INDEX idx_installments_fee ON installments(fee_id);
CREATE INDEX idx_invoices_student ON invoices(student_id);
CREATE INDEX idx_invoices_tenant_status ON invoices(tenant_id, status);
