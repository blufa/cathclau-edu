ALTER TABLE enrollments ADD COLUMN departure_date DATE;
ALTER TABLE enrollments ADD COLUMN departure_reason VARCHAR(20);
ALTER TABLE enrollments ADD COLUMN destination_school VARCHAR(255);
ALTER TABLE students ADD COLUMN admission_type VARCHAR(20) NOT NULL DEFAULT 'NOUVELLE';
