-- Module IAM: users, roles, permissions

CREATE TABLE users (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT,
    first_name       VARCHAR(100) NOT NULL,
    last_name        VARCHAR(100) NOT NULL,
    email            VARCHAR(255) NOT NULL UNIQUE,
    password_hash    VARCHAR(255) NOT NULL,
    phone            VARCHAR(30),
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at    TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_tenant ON users (tenant_id);
CREATE INDEX idx_users_email  ON users (email);

CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE permissions (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE user_roles (
    id      BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id),
    role_id BIGINT NOT NULL REFERENCES roles (id),
    UNIQUE (user_id, role_id)
);

CREATE TABLE role_permissions (
    id            BIGSERIAL PRIMARY KEY,
    role_id       BIGINT NOT NULL REFERENCES roles (id),
    permission_id BIGINT NOT NULL REFERENCES permissions (id),
    UNIQUE (role_id, permission_id)
);

-- Seed: 8 reference roles
INSERT INTO roles (name, code, description) VALUES
    ('Super Administrator', 'SUPER_ADMIN',   'Platform management'),
    ('School Administrator', 'SCHOOL_ADMIN', 'Full school management'),
    ('Director',             'DIRECTOR',     'Pedagogical direction'),
    ('Accountant',           'ACCOUNTANT',   'Financial management'),
    ('Secretary',            'SECRETARY',    'Secretariat and enrollments'),
    ('Teacher',              'TEACHER',      'Grade and attendance entry'),
    ('Parent',               'PARENT',       'View student information'),
    ('Student',              'STUDENT',      'Access personal space');
