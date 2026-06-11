-- Module academics

CREATE TABLE academic_years (
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  BIGINT       NOT NULL,
    label      VARCHAR(50)  NOT NULL,
    start_date DATE,
    end_date   DATE,
    status     VARCHAR(20)  NOT NULL DEFAULT 'PLANNED',
    is_current BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, label)
);

CREATE INDEX idx_academic_years_tenant ON academic_years (tenant_id);

CREATE TABLE periods (
    id               BIGSERIAL PRIMARY KEY,
    academic_year_id BIGINT      NOT NULL REFERENCES academic_years (id),
    label            VARCHAR(50) NOT NULL,
    type             VARCHAR(20) NOT NULL,
    sort_order       INTEGER,
    start_date       DATE,
    end_date         DATE,
    closed           BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE TABLE rooms (
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  BIGINT       NOT NULL,
    label      VARCHAR(100) NOT NULL,
    building   VARCHAR(100),
    capacity   INTEGER,
    type       VARCHAR(30),
    available  BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE levels (
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  BIGINT       NOT NULL,
    label      VARCHAR(100) NOT NULL,
    sort_order INTEGER
);

CREATE TABLE school_classes (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL,
    level_id         BIGINT       REFERENCES levels (id),
    academic_year_id BIGINT       NOT NULL REFERENCES academic_years (id),
    head_teacher_id  BIGINT,
    room_id          BIGINT       REFERENCES rooms (id),
    label            VARCHAR(100) NOT NULL,
    headcount        INTEGER      NOT NULL DEFAULT 0,
    max_capacity     INTEGER
);

CREATE INDEX idx_school_classes_academic_year ON school_classes (academic_year_id);
CREATE INDEX idx_school_classes_tenant        ON school_classes (tenant_id);

CREATE TABLE subjects (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    label       VARCHAR(100) NOT NULL,
    coefficient NUMERIC(5,2),
    syllabus    TEXT
);

CREATE TABLE class_subjects (
    id         BIGSERIAL PRIMARY KEY,
    class_id   BIGINT NOT NULL REFERENCES school_classes (id),
    subject_id BIGINT NOT NULL REFERENCES subjects (id),
    teacher_id BIGINT,
    UNIQUE (class_id, subject_id)
);

CREATE TABLE schedules (
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  BIGINT      NOT NULL,
    class_id   BIGINT      NOT NULL REFERENCES school_classes (id),
    subject_id BIGINT      REFERENCES subjects (id),
    teacher_id BIGINT,
    room_id    BIGINT      REFERENCES rooms (id),
    day        VARCHAR(15) NOT NULL,
    start_time TIME,
    end_time   TIME
);

CREATE INDEX idx_schedules_class ON schedules (class_id);
