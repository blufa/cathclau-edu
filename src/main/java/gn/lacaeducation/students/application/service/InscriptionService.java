package gn.lacaeducation.students.application.service;

import gn.lacaeducation.students.application.event.EnrollmentValidatedEvent;
import gn.lacaeducation.students.application.port.in.EnrollmentUseCase;
import gn.lacaeducation.students.application.port.out.EnrollmentRepository;
import gn.lacaeducation.students.domain.Enrollment;
import gn.lacaeducation.students.domain.EnrollmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service @Transactional @RequiredArgsConstructor
public class InscriptionService implements EnrollmentUseCase {

    private final EnrollmentRepository enrollmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Enrollment enroll(EnrollCommand cmd) {
        return enrollmentRepository.save(Enrollment.builder()
                .studentId(cmd.studentId()).classId(cmd.classId())
                .academicYearId(cmd.academicYearId()).type(cmd.type())
                .status(EnrollmentStatus.PENDING).enrolledAt(LocalDate.now()).build());
    }

    @Override
    public Enrollment validate(Long id) {
        Enrollment e = load(id);
        e.setStatus(EnrollmentStatus.VALIDATED);
        Enrollment saved = enrollmentRepository.save(e);
        eventPublisher.publishEvent(new EnrollmentValidatedEvent(null, e.getStudentId(), e.getClassId(), e.getAcademicYearId()));
        return saved;
    }

    @Override
    public Enrollment cancel(Long id) {
        Enrollment e = load(id);
        e.setStatus(EnrollmentStatus.CANCELLED);
        return enrollmentRepository.save(e);
    }

    @Override @Transactional(readOnly = true)
    public Enrollment findById(Long id) { return load(id); }

    @Override @Transactional(readOnly = true)
    public List<Enrollment> findByStudent(Long studentId) { return enrollmentRepository.findByStudentId(studentId); }

    @Override @Transactional(readOnly = true)
    public List<Enrollment> findByClass(Long classId) { return enrollmentRepository.findByClassId(classId); }

    @Override @Transactional(readOnly = true)
    public List<Enrollment> findByAcademicYear(Long academicYearId) { return enrollmentRepository.findByAcademicYearId(academicYearId); }

    @Override
    public void delete(Long id) { enrollmentRepository.deleteById(id); }

    private Enrollment load(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + id));
    }
}
