package gn.lacaeducation.academics;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
public interface AcademicsDataProvider {
    Optional<ClassPublicInfo> findClass(Long classId);
    Optional<PeriodPublicInfo> findPeriod(Long periodId);
    /** Périodes d'une année scolaire, triées par ordre (ex. les 2 semestres, pour le bulletin annuel). */
    List<PeriodPublicInfo> findPeriodsByAcademicYear(Long academicYearId);
    /** Libellés des matières par identifiant (pour les documents : bulletins, relevés). */
    Map<Long, String> subjectLabels(Collection<Long> subjectIds);
    /** True only if the period is not closed and its academic year is EN_COURS (grade entry allowed). */
    boolean isPeriodOpenForGrades(Long periodId);
    /** True si une autre période pointe vers celle-ci via compositionPeriodId (§retour fondateur) — cette
     * période n'est qu'une composition fusionnée dans le bulletin de sa période "cours" associée, elle ne
     * doit jamais produire son propre bulletin indépendant. */
    boolean isCompositionTargetPeriod(Long periodId);
    List<ClassSubjectPublicInfo> findClassSubjects(Long classId);
    List<ClassPublicInfo> findClassesByHeadTeacher(Long teacherId);
    List<ClassSubjectPublicInfo> findClassSubjectsByTeacher(Long teacherId);
    List<ScheduleSlotPublicInfo> findScheduleByTeacher(Long teacherId);
    List<ScheduleSlotPublicInfo> findScheduleByClass(Long classId);
    /** Toutes les classes d'un tenant (job planifié parcourant tout l'établissement, ex. marquage
     * automatique des absences en fin de journée). */
    List<ClassPublicInfo> findClassesByTenant(Long tenantId);
    /** Nombre de classes d'un tenant pour une année scolaire donnée (ex. l'année courante) — évite de
     * compter les classes d'années révolues dans un effectif affiché. */
    long countClassesByTenantAndYear(Long tenantId, Long academicYearId);
    /** Libellé de l'année scolaire courante du tenant (pour les documents). */
    Optional<String> currentAcademicYearLabel(Long tenantId);
    /** Année scolaire courante complète (id + date de fin) — ex. pour borner la validité d'une carte élève. */
    Optional<AcademicYearPublicInfo> currentAcademicYear(Long tenantId);
    /** Année scolaire par id (pas forcément la courante) — ex. pour dériver sa durée réelle
     * (projection de paie sur ce qu'il reste de l'année, plutôt qu'un nombre de mois fixe). */
    Optional<AcademicYearPublicInfo> findAcademicYear(Long academicYearId);
    /** Événements école (kermesse, match, entraînement...) du tenant dont la période chevauche
     * [from, to] — pour que la paie garde payées les heures des enseignants concernés (§payroll). */
    java.util.List<SchoolEventPublicInfo> findSchoolEvents(Long tenantId, java.time.LocalDate from, java.time.LocalDate to);
    /** Vrai si la direction a explicitement crédité (payé) cet enseignant pour un événement de type
     *  OTHER — sans objet pour un HOLIDAY, toujours payé (voir §payroll). */
    boolean isTeacherCreditedForSchoolEvent(Long schoolEventId, Long teacherId);
}
