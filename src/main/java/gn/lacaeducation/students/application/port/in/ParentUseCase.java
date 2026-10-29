package gn.lacaeducation.students.application.port.in;

import gn.lacaeducation.students.domain.*;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Optional;

public interface ParentUseCase {
    /** Le numéro de téléphone est obligatoire : c'est le seul canal fiable de notification SMS des parents. */
    record CreateParentCommand(Long tenantId, Long userId, String firstName, String lastName,
                               @NotBlank(message = "Le numéro de téléphone est obligatoire") String phone,
                               String email, String occupation, String address,
                               /** Clé d'idempotence fournie par le client (mode hors-ligne) : rejouer avec la même
                                *  clé après une coupure réseau renvoie le parent déjà créé au lieu d'en créer un second. */
                               String idempotencyKey,
                               /** Lien direct vers une fiche Teacher/Employee, SANS compte de connexion — pour le
                                *  personnel qui ne veut/ne peut pas en avoir un (ex. pas de smartphone Android).
                                *  Null si non applicable (lien par compte, ou pas de personnel). */
                               StaffLinkType staffType, Long staffId) {}
    record UpdateParentCommand(String firstName, String lastName,
                               @NotBlank(message = "Le numéro de téléphone est obligatoire") String phone,
                               String email, String occupation, String address,
                               /** Lie un compte de connexion créé APRÈS coup (ex. « Créer un accès » oublié à la
                                *  création) — null = ne touche pas au lien existant, jamais utilisé pour le retirer
                                *  (même garde que TeacherUseCase/EmployeeUseCase#UpdateXCommand). */
                               Long userId,
                               /** Même principe pour le lien direct personnel sans compte. */
                               StaffLinkType staffType, Long staffId,
                               /** Réglage comptable (§7 payroll) : vrai si ce parent, membre du personnel, prend en
                                *  charge lui-même les frais de ses enfants — exclut l'élève de toute retenue
                                *  automatique sur salaire. Sans effet sur un parent non-personnel. */
                               boolean excludeFromAutoDeduction) {}
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
    /** Enfants de toutes les fiches Parent partageant ce compte (voir {@link #findByUserId}) : une
     *  fiche dupliquée peut porter des liens enfant différents de la fiche "canonique". */
    List<Student> findStudentsByUserId(Long userId);
    /** Parmi les parents de cette école, ceux qui ont au moins un enfant lié — le complément (les
     * autres ids de {@link #findByTenantId}) sont des prospects/contacts sans enfant (§7 cahier des
     * charges). Ne renvoie que des ids, pas de comptage détaillé : évite le N+1 sur un annuaire. */
    List<Long> findLinkedParentIds(Long tenantId);
}
