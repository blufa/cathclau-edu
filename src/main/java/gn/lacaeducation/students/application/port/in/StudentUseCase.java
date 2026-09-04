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
                                String previousClass, Boolean repeating, String transferReason, String medicalNotes,
                                /** Créer un accès au portail (connexion par matricule) + son mot de passe. */
                                Boolean createLogin, String loginPassword,
                                /** Clé d'idempotence fournie par le client (mode hors-ligne) : rejouer avec la même
                                 *  clé après une coupure réseau renvoie l'élève déjà créé au lieu d'en créer un second. */
                                String idempotencyKey) {}
    record UpdateStudentCommand(String studentNumber, String firstName, String lastName,
                                Gender gender, LocalDate birthDate, String birthPlace,
                                String address, String phone, String email, String nationality, String originSchool, String photo,
                                String birthCertificateNumber, String bloodGroup, String neighborhood, String landmark,
                                String previousClass, Boolean repeating, String transferReason, String medicalNotes) {}
    record AddDocumentCommand(Long studentId, DocumentType documentType, String fileUrl) {}
    /** Champs modifiables par l'élève lui-même (portail élève) — jamais matricule/état civil/notes médicales. */
    record UpdateOwnProfileCommand(String phone, String email, String address) {}

    Student create(CreateStudentCommand command);
    Student update(Long id, UpdateStudentCommand command);
    Student updatePhoto(Long id, String photoUrl);
    /** Mise à jour self-service du profil de l'élève identifié par son compte {@code userId}. */
    Student updateOwnProfile(Long userId, UpdateOwnProfileCommand command);
    /** Mise à jour self-service de la photo de l'élève identifié par son compte {@code userId}. */
    Student updateOwnPhoto(Long userId, String photoUrl);
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
