package gn.lacaeducation.students.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

interface EleveParentJpaRepository extends JpaRepository<EleveParentJpaEntity, Long> {
    List<EleveParentJpaEntity> findByStudentId(Long studentId);
    @Query("SELECT p FROM ParentJpaEntity p JOIN EleveParentJpaEntity sp ON sp.parentId = p.id WHERE sp.studentId = :studentId")
    List<ParentJpaEntity> findParentsByStudentId(@Param("studentId") Long studentId);

    List<EleveParentJpaEntity> findByParentId(Long parentId);

    void deleteByStudentIdAndParentId(Long studentId, Long parentId);
}
