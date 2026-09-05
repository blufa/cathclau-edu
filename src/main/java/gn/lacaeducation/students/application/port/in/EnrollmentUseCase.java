package gn.lacaeducation.students.application.port.in;

import gn.lacaeducation.students.EnrollmentType;
import gn.lacaeducation.students.domain.*;
import java.util.List;

public interface EnrollmentUseCase {
    /** platformAccessOptIn : le parent accepte-t-il que l'élève soit connecté à la plateforme (frais
     * annexe ajouté aux mensualités si oui) ? Null/absent traité comme false. */
    record EnrollCommand(Long studentId, Long classId, Long academicYearId, EnrollmentType type,
                         Boolean platformAccessOptIn) {}

    Enrollment enroll(EnrollCommand command);
    Enrollment reenroll(EnrollCommand command);
    Enrollment update(Long id, EnrollCommand command);
    /** Modifie après coup le choix d'accès à la plateforme (le parent peut changer d'avis en cours d'année). */
    Enrollment updatePlatformAccessOptIn(Long id, boolean optIn);
    Enrollment validate(Long id);
    Enrollment cancel(Long id);
    Enrollment findById(Long id);
    List<Enrollment> findByStudent(Long studentId);
    List<Enrollment> findByClass(Long classId);
    List<Enrollment> findByAcademicYear(Long academicYearId);
    List<Enrollment> search(Long studentId, Long classId, Long academicYearId, EnrollmentStatus status);
    void delete(Long id);
}
