package gn.lacaeducation.staff.application.port.in;

import gn.lacaeducation.academics.ClassPublicInfo;
import gn.lacaeducation.academics.ClassSubjectPublicInfo;
import gn.lacaeducation.academics.ScheduleSlotPublicInfo;
import gn.lacaeducation.staff.domain.Teacher;
import gn.lacaeducation.staff.domain.TeacherMonthlyWorkload;
import gn.lacaeducation.staff.domain.TeacherWorkload;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TeacherUseCase {
    record CreateTeacherCommand(Long tenantId, Long userId, String staffNumber,
                                String firstName, String lastName, String phone,
                                String email, String specialization, LocalDate hireDate,
                                String employmentStatus, String diploma, String remunerationType,
                                BigDecimal remunerationRate, String teachingAuthorizationNumber, String cnssNumber,
                                /** Compte bancaire (RIB/IBAN) optionnel, pour le fichier bancaire de paie. */
                                String bankAccount,
                                /** Clé d'idempotence fournie par le client (mode hors-ligne) : rejouer avec la même
                                 *  clé après une coupure réseau renvoie l'enseignant déjà créé au lieu d'en créer un second. */
                                String idempotencyKey) {}
    record UpdateTeacherCommand(String staffNumber, String firstName, String lastName,
                                String phone, String email, String specialization, LocalDate hireDate,
                                String employmentStatus, String diploma, String remunerationType,
                                BigDecimal remunerationRate, String teachingAuthorizationNumber, String cnssNumber,
                                String bankAccount) {}
    /** Champs modifiables par l'enseignant lui-même — jamais matricule/spécialité/dossier professionnel. */
    record UpdateOwnProfileCommand(String phone, String email) {}

    Teacher create(CreateTeacherCommand command);
    Teacher update(Long id, UpdateTeacherCommand command);
    Teacher setActive(Long id, boolean active);
    /** Mise à jour self-service du profil de l'enseignant identifié par son compte {@code userId}. */
    Teacher updateOwnProfile(Long userId, UpdateOwnProfileCommand command);
    /** Mise à jour self-service de la photo de l'enseignant identifié par son compte {@code userId}. */
    Teacher updateOwnPhoto(Long userId, String photoUrl);
    Optional<Teacher> findById(Long id);
    Optional<Teacher> findByUserId(Long userId);
    List<Teacher> findByTenantId(Long tenantId);
    void delete(Long id);
    boolean existsById(Long id);

    // Consolidated views (read-through to academics)
    List<ClassPublicInfo> findClasses(Long teacherId);
    List<ClassSubjectPublicInfo> findSubjects(Long teacherId);
    List<ScheduleSlotPublicInfo> findSchedule(Long teacherId);

    /** Charge horaire hebdomadaire consolidée (somme des créneaux d'emploi du temps). */
    TeacherWorkload computeWorkload(Long teacherId);

    /** Charge horaire d'un mois donné (1-12), en projetant l'emploi du temps hebdomadaire récurrent
     * sur le nombre d'occurrences de chaque jour dans ce mois — alimente le compteur mensuel (§2). */
    TeacherMonthlyWorkload computeMonthlyWorkload(Long teacherId, int year, int month);
}
