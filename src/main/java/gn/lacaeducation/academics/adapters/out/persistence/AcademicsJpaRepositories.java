package gn.lacaeducation.academics.adapters.out.persistence;

import gn.lacaeducation.academics.domain.valueobject.JourSemaine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    List<SalleJpaEntity> findByTenantId(Long tenantId);
}

interface NiveauJpaRepository extends JpaRepository<NiveauJpaEntity, Long> {
    List<NiveauJpaEntity> findByTenantIdOrderBySortOrder(Long tenantId);
}

interface ClasseJpaRepository extends JpaRepository<ClasseJpaEntity, Long> {
    List<ClasseJpaEntity> findByAcademicYearId(Long academicYearId);
    List<ClasseJpaEntity> findByTenantId(Long tenantId);
}

interface MatiereJpaRepository extends JpaRepository<MatiereJpaEntity, Long> {
    List<MatiereJpaEntity> findByTenantId(Long tenantId);
}

interface ClasseMatiereJpaRepository extends JpaRepository<ClasseMatiereJpaEntity, Long> {
    List<ClasseMatiereJpaEntity> findBySchoolClassId(Long schoolClassId);
}

interface EmploiDuTempsJpaRepository extends JpaRepository<EmploiDuTempsJpaEntity, Long> {
    List<EmploiDuTempsJpaEntity> findBySchoolClassId(Long schoolClassId);
    List<EmploiDuTempsJpaEntity> findByTeacherId(Long teacherId);

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
