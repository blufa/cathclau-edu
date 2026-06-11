CREATE TABLE student_attendances (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    date DATE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PRESENT','ABSENT','LATE')),
    reason VARCHAR(500),
    parent_notified BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE teacher_attendances (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    date DATE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PRESENT','ABSENT')),
    reason VARCHAR(500)
);

CREATE INDEX idx_student_attendances_class_date ON student_attendances(class_id, date);
CREATE INDEX idx_student_attendances_student ON student_attendances(student_id);
CREATE INDEX idx_teacher_attendances_teacher ON teacher_attendances(teacher_id);
