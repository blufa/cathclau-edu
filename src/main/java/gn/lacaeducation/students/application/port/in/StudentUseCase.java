package gn.lacaeducation.students.application.port.in;

import gn.lacaeducation.students.domain.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudentUseCase {
    record CreateStudentCommand(Long tenantId, Long userId, String studentNumber,
                                String firstName, String lastName, Gender gender,
                                LocalDate birthDate, String birthPlace, String address,
                                String phone, String email, String nationality) {}
    record UpdateStudentCommand(String studentNumber, String firstName, String lastName,
                                Gender gender, LocalDate birthDate, String birthPlace,
                                String address, String phone, String email, String nationality) {}
    record AddDocumentCommand(Long studentId, DocumentType documentType, String fileUrl) {}

    Student create(CreateStudentCommand command);
    Student update(Long id, UpdateStudentCommand command);
    Optional<Student> findById(Long id);
    List<Student> findByTenantId(Long tenantId);
    void delete(Long id);
    boolean existsById(Long id);
    long countByTenantId(Long tenantId);
    StudentDocument addDocument(AddDocumentCommand command);
    List<StudentDocument> findDocuments(Long studentId);
    void deleteDocument(Long documentId);
}
