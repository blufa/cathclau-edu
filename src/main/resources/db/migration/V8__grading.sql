CREATE TABLE assessments (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    period_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL CHECK (type IN ('QUIZ','HOMEWORK','EXAM','FINAL')),
    label VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    coefficient NUMERIC(5,2) NOT NULL,
    max_score NUMERIC(5,2) NOT NULL
);

CREATE TABLE grades (
    id BIGSERIAL PRIMARY KEY,
    assessment_id BIGINT NOT NULL REFERENCES assessments(id),
    student_id BIGINT NOT NULL,
    value NUMERIC(5,2) NOT NULL,
    validated BOOLEAN NOT NULL DEFAULT FALSE,
    entered_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE exams (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    period_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('MOCK','SEMESTER','ANNUAL','NATIONAL')),
    label VARCHAR(255) NOT NULL,
    scheduled_date DATE,
    convocation_url VARCHAR(500),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PLANNED','ONGOING','COMPLETED'))
);

CREATE TABLE report_cards (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    period_id BIGINT NOT NULL,
    overall_average NUMERIC(5,2),
    class_average NUMERIC(5,2),
    highest_average NUMERIC(5,2),
    lowest_average NUMERIC(5,2),
    rank INTEGER,
    class_size INTEGER,
    honor VARCHAR(50),
    decision VARCHAR(100),
    comment TEXT,
    pdf_file VARCHAR(500),
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT','GENERATED','PUBLISHED')),
    generated_at TIMESTAMP
);

CREATE TABLE report_card_lines (
    id BIGSERIAL PRIMARY KEY,
    report_card_id BIGINT NOT NULL REFERENCES report_cards(id),
    subject_id BIGINT NOT NULL,
    subject_average NUMERIC(5,2),
    coefficient NUMERIC(5,2) NOT NULL,
    weighted_average NUMERIC(5,2),
    subject_rank INTEGER,
    comment TEXT,
    teacher_name VARCHAR(200)
);

CREATE INDEX idx_grades_assessment ON grades(assessment_id);
CREATE INDEX idx_grades_student ON grades(student_id);
CREATE INDEX idx_report_cards_student_period ON report_cards(student_id, period_id);
CREATE INDEX idx_report_cards_class_period ON report_cards(class_id, period_id);
