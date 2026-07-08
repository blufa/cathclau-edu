-- Student's previous/origin school (for transfers and re-enrolment context).
ALTER TABLE students ADD COLUMN IF NOT EXISTS origin_school VARCHAR(200);
