package gn.lacaeducation.students.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.students.EnrollmentValidationApi;
import gn.lacaeducation.students.application.event.EnrollmentAbandonedEvent;
import gn.lacaeducation.students.application.event.EnrollmentCancelledEvent;
import gn.lacaeducation.students.application.event.EnrollmentClassChangedEvent;
import gn.lacaeducation.students.application.event.EnrollmentCreatedEvent;
import gn.lacaeducation.students.application.event.EnrollmentValidatedEvent;
import gn.lacaeducation.students.application.port.in.EnrollmentUseCase;
import gn.lacaeducation.students.ClassCapacityPort;
import gn.lacaeducation.students.application.port.out.EnrollmentRepository;
import gn.lacaeducation.students.domain.Enrollment;
import gn.lacaeducation.students.domain.EnrollmentStatus;
import gn.lacaeducation.students.EnrollmentType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service @Transactional @RequiredArgsConstructor
public class InscriptionService implements EnrollmentUseCase, EnrollmentValidationApi {

    private final EnrollmentRepository enrollmentRepository;
    private final ClassCapacityPort classCapacityPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Enrollment enroll(EnrollCommand cmd) {
        return create(cmd, cmd.type() != null ? cmd.type() : EnrollmentType.NEW);
    }

    @Override
    public Enrollment reenroll(EnrollCommand cmd) {
        return create(cmd, EnrollmentType.RE_ENROLLMENT);
    }

    private Enrollment create(EnrollCommand cmd, EnrollmentType type) {
        requireNoActiveEnrollment(cmd.studentId(), cmd.academicYearId());
        Enrollment saved = enrollmentRepository.save(Enrollment.builder()
                .studentId(cmd.studentId()).classId(cmd.classId())
                .academicYearId(cmd.academicYearId())
                .type(type)
                .status(EnrollmentStatus.PENDING).enrolledAt(LocalDate.now())
                .platformAccessOptIn(Boolean.TRUE.equals(cmd.platformAccessOptIn()))
                .monthlyPaymentOptIn(Boolean.TRUE.equals(cmd.monthlyPaymentOptIn())).build());
        publishCreated(saved);
        return saved;
    }

    @Override
    public Enrollment updatePlatformAccessOptIn(Long id, boolean optIn) {
        Enrollment e = load(id);
        e.setPlatformAccessOptIn(optIn);
        return enrollmentRepository.save(e);
    }

    @Override
    public Enrollment updateMonthlyPaymentOptIn(Long id, boolean optIn) {
        Enrollment e = load(id);
        e.setMonthlyPaymentOptIn(optIn);
        return enrollmentRepository.save(e);
    }

    @Override
    public Enrollment update(Long id, EnrollCommand cmd) {
        Enrollment e = load(id);
        if (e.getStatus() != EnrollmentStatus.PENDING) {
            throw new BusinessRuleException("Only a pending enrollment can be modified: id=" + id);
        }
        boolean yearChanged = !e.getAcademicYearId().equals(cmd.academicYearId());
        boolean classChanged = !e.getClassId().equals(cmd.classId());
        if (yearChanged && enrollmentRepository.existsActiveForStudentYear(e.getStudentId(), cmd.academicYearId())) {
            throw new ConflictException("Student " + e.getStudentId()
                    + " already has an active enrollment for academic year " + cmd.academicYearId());
        }
        e.setClassId(cmd.classId());
        e.setAcademicYearId(cmd.academicYearId());
        if (cmd.type() != null) e.setType(cmd.type());
        Enrollment saved = enrollmentRepository.save(e);
        // Re-issue the enrollment invoice against the (possibly new) class fee.
        if (classChanged) publishCreated(saved);
        return saved;
    }

    @Override
    public Enrollment validate(Long id) {
        Enrollment e = load(id);
        if (e.getStatus() == EnrollmentStatus.VALIDATED) {
            throw new BusinessRuleException("Enrollment is already validated: id=" + id);
        }
        return doValidate(e);
    }

    @Override
    public void validatePaidEnrollment(Long studentId, Long classId) {
        enrollmentRepository.findPendingByStudentAndClass(studentId, classId)
                .ifPresent(this::doValidate);
    }

    /** Shared validation: capacity + one-validated-per-year checks, then transition to VALIDATED. */
    private Enrollment doValidate(Enrollment e) {
        if (e.getStatus() == EnrollmentStatus.VALIDATED) return e;
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
        Enrollment saved = enrollmentRepository.save(e);
        eventPublisher.publishEvent(new EnrollmentCancelledEvent(
                null, e.getStudentId(), e.getClassId(), e.getAcademicYearId()));
        return saved;
    }

    @Override
    public Enrollment abandon(Long id) {
        Enrollment e = load(id);
        // Un abandon en cours d'année ne se déclare que sur une inscription déjà validée — sinon
        // c'est une simple annulation (cancel) qu'il faut utiliser à la place.
        if (e.getStatus() != EnrollmentStatus.VALIDATED) {
            throw new BusinessRuleException(
                    "Seule une inscription validée peut être marquée comme abandon : id=" + id);
        }
        e.setStatus(EnrollmentStatus.ABANDONED);
        Enrollment saved = enrollmentRepository.save(e);
        eventPublisher.publishEvent(new EnrollmentAbandonedEvent(
                null, e.getStudentId(), e.getClassId(), e.getAcademicYearId()));
        return saved;
    }

    @Override
    public Enrollment transferClass(Long id, Long newClassId) {
        Enrollment e = load(id);
        if (e.getStatus() != EnrollmentStatus.VALIDATED) {
            throw new BusinessRuleException(
                    "Seule une inscription validée peut être transférée vers une autre classe : id=" + id);
        }
        if (e.getClassId().equals(newClassId)) {
            throw new BusinessRuleException("L'élève est déjà inscrit dans cette classe.");
        }
        Integer max = classCapacityPort.maxCapacity(newClassId);
        if (max != null) {
            long validated = enrollmentRepository.countByClassIdAndStatus(newClassId, EnrollmentStatus.VALIDATED);
            if (validated >= max) {
                throw new ConflictException("Class " + newClassId + " has reached its capacity (" + max + ").");
            }
        }
        Long oldClassId = e.getClassId();
        e.setClassId(newClassId);
        Enrollment saved = enrollmentRepository.save(e);
        eventPublisher.publishEvent(new EnrollmentClassChangedEvent(
                null, e.getStudentId(), e.getId(), oldClassId, newClassId, e.getAcademicYearId()));
        return saved;
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

    private void requireNoActiveEnrollment(Long studentId, Long academicYearId) {
        if (enrollmentRepository.existsActiveForStudentYear(studentId, academicYearId)) {
            throw new ConflictException("Student " + studentId
                    + " already has an active enrollment for academic year " + academicYearId
                    + "; cancel it or modify the existing one.");
        }
    }

    private void publishCreated(Enrollment e) {
        eventPublisher.publishEvent(new EnrollmentCreatedEvent(
                e.getStudentId(), e.getClassId(), e.getAcademicYearId(), e.getId(), e.getType()));
    }

    private Enrollment load(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", id));
    }
}
