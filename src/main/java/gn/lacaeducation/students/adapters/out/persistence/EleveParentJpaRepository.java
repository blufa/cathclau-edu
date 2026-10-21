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

    /** Tous les parentId ayant au moins un lien enfant, toutes écoles confondues (sans jointure tenant
     * ici — le tenant est déjà filtré côté appelant via la liste des parents de l'école, voir §7
     * cahier des charges "répertoire de parents/prospects sans enfant lié"). */
    @Query("SELECT DISTINCT sp.parentId FROM EleveParentJpaEntity sp")
    List<Long> findDistinctParentIds();
}
