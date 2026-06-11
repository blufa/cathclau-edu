CREATE TABLE students (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL,
    user_id     BIGINT,
    student_number VARCHAR(50),
    photo       VARCHAR(500),
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    gender      VARCHAR(10),
    birth_date  DATE,
    birth_place VARCHAR(200),
    address     VARCHAR(500),
    phone       VARCHAR(50),
    email       VARCHAR(200),
    nationality VARCHAR(100),
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE parents (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL,
    user_id     BIGINT,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    phone       VARCHAR(50),
    email       VARCHAR(200),
    occupation  VARCHAR(200),
    address     VARCHAR(500)
);

CREATE TABLE student_parents (
    id           BIGSERIAL PRIMARY KEY,
    student_id   BIGINT NOT NULL,
    parent_id    BIGINT NOT NULL,
    relationship VARCHAR(20) NOT NULL
);

CREATE TABLE enrollments (
    id               BIGSERIAL PRIMARY KEY,
    student_id       BIGINT NOT NULL,
    class_id         BIGINT NOT NULL,
    academic_year_id BIGINT NOT NULL,
    type             VARCHAR(20) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    enrolled_at      DATE DEFAULT CURRENT_DATE
);

CREATE TABLE student_documents (
    id            BIGSERIAL PRIMARY KEY,
    student_id    BIGINT NOT NULL,
    document_type VARCHAR(50),
    file_url      VARCHAR(500),
    added_at      TIMESTAMP DEFAULT NOW()
);
