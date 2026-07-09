package gn.lacaeducation.students.application.port.in;

import gn.lacaeducation.students.EnrollmentType;
import gn.lacaeducation.students.domain.*;
import java.util.List;

public interface EnrollmentUseCase {
    record EnrollCommand(Long studentId, Long classId, Long academicYearId, EnrollmentType type) {}

    Enrollment enroll(EnrollCommand command);
    Enrollment reenroll(EnrollCommand command);
    Enrollment update(Long id, EnrollCommand command);
    Enrollment validate(Long id);
    Enrollment cancel(Long id);
    Enrollment findById(Long id);
    List<Enrollment> findByStudent(Long studentId);
    List<Enrollment> findByClass(Long classId);
    List<Enrollment> findByAcademicYear(Long academicYearId);
    List<Enrollment> search(Long studentId, Long classId, Long academicYearId, EnrollmentStatus status);
    void delete(Long id);
}
