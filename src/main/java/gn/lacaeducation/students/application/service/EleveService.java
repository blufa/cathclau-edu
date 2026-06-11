package gn.lacaeducation.students.application.service;

import gn.lacaeducation.platform.TenantQuotaService;
import gn.lacaeducation.students.application.port.in.StudentUseCase;
import gn.lacaeducation.students.application.port.out.StudentDocumentRepository;
import gn.lacaeducation.students.application.port.out.StudentRepository;
import gn.lacaeducation.students.domain.Student;
import gn.lacaeducation.students.domain.StudentDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class EleveService implements StudentUseCase {

    private final StudentRepository studentRepository;
    private final StudentDocumentRepository documentRepository;
    private final TenantQuotaService tenantQuotaService;

    @Override
    public Student create(CreateStudentCommand cmd) {
        long count = studentRepository.countByTenantId(cmd.tenantId());
        if (!tenantQuotaService.isQuotaAvailable(cmd.tenantId(), (int) count)) {
            throw new IllegalStateException("Student quota exceeded for tenant " + cmd.tenantId());
        }
        return studentRepository.save(Student.builder()
                .tenantId(cmd.tenantId()).userId(cmd.userId())
                .studentNumber(cmd.studentNumber()).firstName(cmd.firstName()).lastName(cmd.lastName())
                .gender(cmd.gender()).birthDate(cmd.birthDate()).birthPlace(cmd.birthPlace())
                .address(cmd.address()).phone(cmd.phone()).email(cmd.email())
                .nationality(cmd.nationality()).createdAt(LocalDateTime.now()).build());
    }

    @Override
    public Student update(Long id, UpdateStudentCommand cmd) {
        Student s = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + id));
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
    public void delete(Long id) { studentRepository.deleteById(id); }

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
