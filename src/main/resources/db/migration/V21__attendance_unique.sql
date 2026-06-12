-- Attendance: one student attendance per student, class and date
ALTER TABLE student_attendances DROP CONSTRAINT IF EXISTS uk_student_attendance_unique;
ALTER TABLE student_attendances
    ADD CONSTRAINT uk_student_attendance_unique UNIQUE (student_id, class_id, date);
