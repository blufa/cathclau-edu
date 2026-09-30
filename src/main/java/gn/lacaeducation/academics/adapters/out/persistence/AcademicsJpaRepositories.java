package gn.lacaeducation.academics.adapters.out.persistence;

import gn.lacaeducation.academics.domain.valueobject.JourSemaine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

interface AnneeScolaireJpaRepository extends JpaRepository<AnneeScolaireJpaEntity, Long> {
    Optional<AnneeScolaireJpaEntity> findByTenantIdAndCurrentTrue(Long tenantId);
    List<AnneeScolaireJpaEntity> findByTenantId(Long tenantId);
    @Modifying
    @Query("UPDATE AnneeScolaireJpaEntity a SET a.current = false WHERE a.tenantId = :tenantId AND a.current = true")
    void removeCurrent(@Param("tenantId") Long tenantId);
}

interface PeriodeJpaRepository extends JpaRepository<PeriodeJpaEntity, Long> {
    List<PeriodeJpaEntity> findByAcademicYearIdOrderBySortOrder(Long academicYearId);
}

interface SalleJpaRepository extends JpaRepository<SalleJpaEntity, Long> {
    // Ordre explicite par id (création) : sans lui, Postgres peut faire réapparaître une salle modifiée
    // en fin de liste (une mise à jour crée une nouvelle version physique de la ligne côté MVCC).
    List<SalleJpaEntity> findByTenantIdOrderById(Long tenantId);
}

interface NiveauJpaRepository extends JpaRepository<NiveauJpaEntity, Long> {
    List<NiveauJpaEntity> findByTenantIdOrderBySortOrder(Long tenantId);
}

interface ClasseJpaRepository extends JpaRepository<ClasseJpaEntity, Long> {
    List<ClasseJpaEntity> findByAcademicYearId(Long academicYearId);
    List<ClasseJpaEntity> findByTenantId(Long tenantId);
    List<ClasseJpaEntity> findByHeadTeacherId(Long headTeacherId);
    long countByTenantIdAndAcademicYearId(Long tenantId, Long academicYearId);
}

interface MatiereJpaRepository extends JpaRepository<MatiereJpaEntity, Long> {
    List<MatiereJpaEntity> findByTenantId(Long tenantId);
}

interface ClasseMatiereJpaRepository extends JpaRepository<ClasseMatiereJpaEntity, Long> {
    List<ClasseMatiereJpaEntity> findBySchoolClassId(Long schoolClassId);
    List<ClasseMatiereJpaEntity> findByTeacherId(Long teacherId);
    List<ClasseMatiereJpaEntity> findBySubjectId(Long subjectId);
}

interface EmploiDuTempsJpaRepository extends JpaRepository<EmploiDuTempsJpaEntity, Long> {
    List<EmploiDuTempsJpaEntity> findBySchoolClassId(Long schoolClassId);
    List<EmploiDuTempsJpaEntity> findByTeacherId(Long teacherId);

    @Query("SELECT e FROM EmploiDuTempsJpaEntity e WHERE "
            + "(:classId IS NULL OR e.schoolClassId = :classId) AND "
            + "(:teacherId IS NULL OR e.teacherId = :teacherId) AND "
            + "(:roomId IS NULL OR e.roomId = :roomId) AND "
            + "(:day IS NULL OR e.day = :day)")
    List<EmploiDuTempsJpaEntity> search(@Param("classId") Long classId,
                                         @Param("teacherId") Long teacherId,
                                         @Param("roomId") Long roomId,
                                         @Param("day") JourSemaine day);

    @Query("SELECT COUNT(e) > 0 FROM EmploiDuTempsJpaEntity e WHERE e.roomId = :roomId AND e.day = :day " +
           "AND e.startTime < :endTime AND e.endTime > :startTime")
    boolean existsRoomConflict(@Param("roomId") Long roomId, @Param("day") JourSemaine day,
                                @Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime);

    @Query("SELECT COUNT(e) > 0 FROM EmploiDuTempsJpaEntity e WHERE e.teacherId = :teacherId AND e.day = :day " +
           "AND e.startTime < :endTime AND e.endTime > :startTime")
    boolean existsTeacherConflict(@Param("teacherId") Long teacherId, @Param("day") JourSemaine day,
                                   @Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime);

    @Query("SELECT COUNT(e) > 0 FROM EmploiDuTempsJpaEntity e WHERE e.schoolClassId = :classId AND e.day = :day " +
           "AND e.startTime < :endTime AND e.endTime > :startTime")
    boolean existsClassConflict(@Param("classId") Long classId, @Param("day") JourSemaine day,
                                 @Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime);

    @Query("SELECT COUNT(e) > 0 FROM EmploiDuTempsJpaEntity e WHERE e.roomId = :roomId AND e.day = :day " +
           "AND e.startTime < :endTime AND e.endTime > :startTime AND e.id <> :excludeId")
    boolean existsRoomConflictExcluding(@Param("roomId") Long roomId, @Param("day") JourSemaine day,
                                         @Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime,
                                         @Param("excludeId") Long excludeId);

    @Query("SELECT COUNT(e) > 0 FROM EmploiDuTempsJpaEntity e WHERE e.teacherId = :teacherId AND e.day = :day " +
           "AND e.startTime < :endTime AND e.endTime > :startTime AND e.id <> :excludeId")
    boolean existsTeacherConflictExcluding(@Param("teacherId") Long teacherId, @Param("day") JourSemaine day,
                                            @Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime,
                                            @Param("excludeId") Long excludeId);

    @Query("SELECT COUNT(e) > 0 FROM EmploiDuTempsJpaEntity e WHERE e.schoolClassId = :classId AND e.day = :day " +
           "AND e.startTime < :endTime AND e.endTime > :startTime AND e.id <> :excludeId")
    boolean existsClassConflictExcluding(@Param("classId") Long classId, @Param("day") JourSemaine day,
                                          @Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime,
                                          @Param("excludeId") Long excludeId);
}

interface ScheduleExceptionJpaRepository extends JpaRepository<ScheduleExceptionJpaEntity, Long> {
    List<ScheduleExceptionJpaEntity> findByWeekStartDate(LocalDate weekStartDate);
    Optional<ScheduleExceptionJpaEntity> findByOriginalScheduleIdAndWeekStartDate(Long originalScheduleId, LocalDate weekStartDate);
    void deleteByOriginalScheduleIdAndWeekStartDate(Long originalScheduleId, LocalDate weekStartDate);
}

interface TeacherFlagJpaRepository extends JpaRepository<TeacherFlagJpaEntity, Long> {
}

interface SchoolEventJpaRepository extends JpaRepository<SchoolEventJpaEntity, Long> {
    List<SchoolEventJpaEntity> findByTenantIdOrderByStartDateDesc(Long tenantId);

    @Query("SELECT e FROM SchoolEventJpaEntity e WHERE e.tenantId = :tenantId "
            + "AND e.startDate <= :to AND e.endDate >= :from")
    List<SchoolEventJpaEntity> findByTenantIdAndDateRangeOverlap(
            @Param("tenantId") Long tenantId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}

interface SchoolEventCreditedTeacherJpaRepository extends JpaRepository<SchoolEventCreditedTeacherJpaEntity, Long> {
    List<SchoolEventCreditedTeacherJpaEntity> findBySchoolEventId(Long schoolEventId);
    boolean existsBySchoolEventIdAndTeacherId(Long schoolEventId, Long teacherId);
    void deleteBySchoolEventId(Long schoolEventId);
}
