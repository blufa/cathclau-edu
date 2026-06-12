package gn.lacaeducation.students.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.students.application.event.EnrollmentValidatedEvent;
import gn.lacaeducation.students.application.port.in.EnrollmentUseCase;
import gn.lacaeducation.students.ClassCapacityPort;
import gn.lacaeducation.students.application.port.out.EnrollmentRepository;
import gn.lacaeducation.students.domain.Enrollment;
import gn.lacaeducation.students.domain.EnrollmentStatus;
import gn.lacaeducation.students.domain.EnrollmentType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service @Transactional @RequiredArgsConstructor
public class InscriptionService implements EnrollmentUseCase {

    private final EnrollmentRepository enrollmentRepository;
    private final ClassCapacityPort classCapacityPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Enrollment enroll(EnrollCommand cmd) {
        return enrollmentRepository.save(Enrollment.builder()
                .studentId(cmd.studentId()).classId(cmd.classId())
                .academicYearId(cmd.academicYearId())
                .type(cmd.type() != null ? cmd.type() : EnrollmentType.NEW)
                .status(EnrollmentStatus.PENDING).enrolledAt(LocalDate.now()).build());
    }

    @Override
    public Enrollment reenroll(EnrollCommand cmd) {
        return enrollmentRepository.save(Enrollment.builder()
                .studentId(cmd.studentId()).classId(cmd.classId())
                .academicYearId(cmd.academicYearId())
                .type(EnrollmentType.RE_ENROLLMENT)
                .status(EnrollmentStatus.PENDING).enrolledAt(LocalDate.now()).build());
    }

    @Override
    public Enrollment validate(Long id) {
        Enrollment e = load(id);
        if (e.getStatus() == EnrollmentStatus.VALIDATED) {
            throw new BusinessRuleException("Enrollment is already validated: id=" + id);
        }
        // One validated enrollment per student and academic year
        if (enrollmentRepository.existsValidatedForStudentYear(e.getStudentId(), e.getAcademicYearId())) {
            throw new ConflictException("Student " + e.getStudentId()
                    + " already has a validated enrollment for academic year " + e.getAcademicYearId());
        }
        // Class capacity check
        Integer max = classCapacityPort.maxCapacity(e.getClassId());
        if (max != null) {
            long validated = enrollmentRepository.countByClassIdAndStatus(e.getClassId(), EnrollmentStatus.VALIDATED);
            if (validated >= max) {
                throw new ConflictException("Class " + e.getClassId() + " has reached its capacity (" + max + ").");
            }
        }
        e.setStatus(EnrollmentStatus.VALIDATED);
        Enrollment saved = enrollmentRepository.save(e);
        eventPublisher.publishEvent(new EnrollmentValidatedEvent(
                null, e.getStudentId(), e.getClassId(), e.getAcademicYearId()));
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

    @Override @Transactional(readOnly = true)
    public List<Enrollment> search(Long studentId, Long classId, Long academicYearId, EnrollmentStatus status) {
        return enrollmentRepository.search(studentId, classId, academicYearId, status);
    }

    @Override
    public void delete(Long id) { enrollmentRepository.deleteById(id); }

    private Enrollment load(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", id));
    }
}
