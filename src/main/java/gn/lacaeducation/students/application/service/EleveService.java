package gn.lacaeducation.students.application.service;

import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.common.QuotaExceededException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.platform.TenantQuotaService;
import gn.lacaeducation.students.application.event.StudentCreatedEvent;
import gn.lacaeducation.students.application.port.in.StudentUseCase;
import gn.lacaeducation.students.application.port.out.StudentDocumentRepository;
import gn.lacaeducation.students.application.port.out.StudentRepository;
import gn.lacaeducation.students.domain.Student;
import gn.lacaeducation.students.domain.StudentDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class EleveService implements StudentUseCase {

    private final StudentRepository studentRepository;
    private final StudentDocumentRepository documentRepository;
    private final TenantQuotaService tenantQuotaService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Student create(CreateStudentCommand cmd) {
        long count = studentRepository.countByTenantId(cmd.tenantId());
        if (!tenantQuotaService.isQuotaAvailable(cmd.tenantId(), (int) count)) {
            throw new QuotaExceededException("Student quota exceeded for tenant " + cmd.tenantId());
        }
        String matricule = resolveStudentNumber(cmd.tenantId(), cmd.studentNumber());
        Student saved = studentRepository.save(Student.builder()
                .tenantId(cmd.tenantId()).userId(cmd.userId())
                .studentNumber(matricule).firstName(cmd.firstName()).lastName(cmd.lastName())
                .gender(cmd.gender()).birthDate(cmd.birthDate()).birthPlace(cmd.birthPlace())
                .address(cmd.address()).phone(cmd.phone()).email(cmd.email())
                .nationality(cmd.nationality()).createdAt(LocalDateTime.now()).build());
        eventPublisher.publishEvent(new StudentCreatedEvent(saved.getTenantId(), saved.getId(), saved.getStudentNumber()));
        return saved;
    }

    /**
     * Returns a tenant-unique matricule: validates a provided one, or auto-generates one in the
     * {@code YEAR-SEQ} format (e.g. {@code 2026-00042}).
     */
    private String resolveStudentNumber(Long tenantId, String provided) {
        if (provided != null && !provided.isBlank()) {
            if (studentRepository.existsByTenantIdAndStudentNumber(tenantId, provided)) {
                throw new ConflictException("Student number already exists for this tenant: " + provided);
            }
            return provided;
        }
        String year = String.valueOf(Year.now().getValue());
        long seq = studentRepository.countByTenantId(tenantId) + 1;
        String candidate;
        do {
            candidate = String.format("%s-%05d", year, seq++);
        } while (studentRepository.existsByTenantIdAndStudentNumber(tenantId, candidate));
        return candidate;
    }

    @Override
    public Student update(Long id, UpdateStudentCommand cmd) {
        Student s = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
        s.setStudentNumber(cmd.studentNumber()); s.setFirstName(cmd.firstName()); s.setLastName(cmd.lastName());
        s.setGender(cmd.gender()); s.setBirthDate(cmd.birthDate()); s.setBirthPlace(cmd.birthPlace());
        s.setAddress(cmd.address()); s.setPhone(cmd.phone()); s.setEmail(cmd.email());
        s.setNationality(cmd.nationality());
        return studentRepository.save(s);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Student> findById(Long id) { return studentRepository.findById(id); }

    @Override
    @Transactional(readOnly = true)
    public List<Student> findByTenantId(Long tenantId) { return studentRepository.findByTenantId(tenantId); }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Student> search(
            Long tenantId, gn.lacaeducation.students.domain.Gender gender, String search,
            Long classId, gn.lacaeducation.students.domain.EnrollmentStatus enrollmentStatus,
            org.springframework.data.domain.Pageable pageable) {
        String q = (search == null || search.isBlank()) ? null : search.toLowerCase();
        return studentRepository.search(tenantId, gender, q, classId, enrollmentStatus, pageable);
    }

    @Override
    public Student updatePhoto(Long id, String photoUrl) {
        Student s = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
        s.setPhoto(photoUrl);
        return studentRepository.save(s);
    }

    @Override
    public void delete(Long id) {
        // Soft delete (archive): never physically remove a student that may carry grades/payments.
        Student s = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
        s.setDeletedAt(LocalDateTime.now());
        studentRepository.save(s);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) { return studentRepository.existsById(id); }

    @Override
    @Transactional(readOnly = true)
    public long countByTenantId(Long tenantId) { return studentRepository.countByTenantId(tenantId); }

    @Override
    public StudentDocument addDocument(AddDocumentCommand cmd) {
        return documentRepository.save(StudentDocument.builder()
                .studentId(cmd.studentId()).documentType(cmd.documentType())
                .fileUrl(cmd.fileUrl()).addedAt(LocalDateTime.now()).build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentDocument> findDocuments(Long studentId) {
        return documentRepository.findByStudentId(studentId);
    }

    @Override
    public void deleteDocument(Long documentId) { documentRepository.deleteDocumentById(documentId); }
}
