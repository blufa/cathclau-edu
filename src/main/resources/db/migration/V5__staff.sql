CREATE TABLE teachers (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      BIGINT NOT NULL,
    user_id        BIGINT,
    staff_number   VARCHAR(50),
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    phone          VARCHAR(50),
    email          VARCHAR(200),
    specialization VARCHAR(200),
    hire_date      DATE
);
