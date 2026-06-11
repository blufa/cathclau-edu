package gn.lacaeducation.students.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface InscriptionJpaRepository extends JpaRepository<InscriptionJpaEntity, Long> {
    List<InscriptionJpaEntity> findByStudentId(Long studentId);
    List<InscriptionJpaEntity> findByClassId(Long classId);
    List<InscriptionJpaEntity> findByAcademicYearId(Long academicYearId);
}
