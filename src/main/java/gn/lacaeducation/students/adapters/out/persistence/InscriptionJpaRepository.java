package gn.lacaeducation.students.adapters.out.persistence;

import gn.lacaeducation.students.domain.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

interface InscriptionJpaRepository extends JpaRepository<InscriptionJpaEntity, Long> {
    List<InscriptionJpaEntity> findByStudentId(Long studentId);
    List<InscriptionJpaEntity> findByClassId(Long classId);
    List<InscriptionJpaEntity> findByAcademicYearId(Long academicYearId);

    long countByClassIdAndStatus(Long classId, EnrollmentStatus status);
    boolean existsByStudentIdAndAcademicYearIdAndStatus(Long studentId, Long academicYearId, EnrollmentStatus status);

    @Query("SELECT e FROM InscriptionJpaEntity e WHERE "
            + "(:studentId IS NULL OR e.studentId = :studentId) AND "
            + "(:classId IS NULL OR e.classId = :classId) AND "
            + "(:academicYearId IS NULL OR e.academicYearId = :academicYearId) AND "
            + "(:status IS NULL OR e.status = :status)")
    List<InscriptionJpaEntity> search(@Param("studentId") Long studentId,
                                       @Param("classId") Long classId,
                                       @Param("academicYearId") Long academicYearId,
                                       @Param("status") EnrollmentStatus status);
}
