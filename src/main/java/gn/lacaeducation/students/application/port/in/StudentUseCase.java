package gn.lacaeducation.students.application.port.in;

import gn.lacaeducation.students.domain.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudentUseCase {
    record CreateStudentCommand(Long tenantId, Long userId, String studentNumber,
                                String firstName, String lastName, Gender gender,
                                LocalDate birthDate, String birthPlace, String address,
                                String phone, String email, String nationality, String originSchool, String photo,
                                String birthCertificateNumber, String bloodGroup, String neighborhood, String landmark,
                                String previousClass, Boolean repeating, String transferReason, String medicalNotes) {}
    record UpdateStudentCommand(String studentNumber, String firstName, String lastName,
                                Gender gender, LocalDate birthDate, String birthPlace,
                                String address, String phone, String email, String nationality, String originSchool, String photo,
                                String birthCertificateNumber, String bloodGroup, String neighborhood, String landmark,
                                String previousClass, Boolean repeating, String transferReason, String medicalNotes) {}
    record AddDocumentCommand(Long studentId, DocumentType documentType, String fileUrl) {}

    Student create(CreateStudentCommand command);
    Student update(Long id, UpdateStudentCommand command);
    Student updatePhoto(Long id, String photoUrl);
    Optional<Student> findById(Long id);
    Optional<Student> findByUserId(Long userId);
    List<Student> findByTenantId(Long tenantId);
    org.springframework.data.domain.Page<Student> search(Long tenantId, Gender gender, String search,
            Long classId, EnrollmentStatus enrollmentStatus, boolean notEnrolled, org.springframework.data.domain.Pageable pageable);
    void delete(Long id);
    boolean existsById(Long id);
    long countByTenantId(Long tenantId);
    StudentDocument addDocument(AddDocumentCommand command);
    List<StudentDocument> findDocuments(Long studentId);
    void deleteDocument(Long documentId);
}
