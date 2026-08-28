package gn.lacaeducation.students.application.port.in;

import gn.lacaeducation.students.domain.*;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Optional;

public interface ParentUseCase {
    /** Le numéro de téléphone est obligatoire : c'est le seul canal fiable de notification SMS des parents. */
    record CreateParentCommand(Long tenantId, Long userId, String firstName, String lastName,
                               @NotBlank(message = "Le numéro de téléphone est obligatoire") String phone,
                               String email, String occupation, String address) {}
    record UpdateParentCommand(String firstName, String lastName,
                               @NotBlank(message = "Le numéro de téléphone est obligatoire") String phone,
                               String email, String occupation, String address) {}
    /** Champs modifiables par le parent lui-même (portail parent §12) — jamais son nom. */
    record UpdateProfileCommand(@NotBlank(message = "Le numéro de téléphone est obligatoire") String phone,
                                String email, String address, String occupation) {}

    Parent create(CreateParentCommand command);
    Parent update(Long id, UpdateParentCommand command);
    /** Mise à jour self-service du profil du parent identifié par son compte {@code userId}. */
    Parent updateOwnProfile(Long userId, UpdateProfileCommand command);
    /** Mise à jour self-service de la photo du parent identifié par son compte {@code userId}. */
    Parent updateOwnPhoto(Long userId, String photoUrl);
    Optional<Parent> findById(Long id);
    Optional<Parent> findByUserId(Long userId);
    List<Parent> findByTenantId(Long tenantId);
    void delete(Long id);
    boolean existsById(Long id);
    StudentParent linkStudent(Long studentId, Long parentId, Relationship relationship);
    /** Désigne le responsable principal (« responsable 1 ») de l'élève (un seul à la fois). */
    void setPrimaryGuardian(Long studentId, Long parentId);
    void unlinkStudent(Long studentParentId);
    void unlinkStudentFromParent(Long studentId, Long parentId);
    List<Parent> findByStudent(Long studentId);
    List<Student> findStudentsByParent(Long parentId);
}
