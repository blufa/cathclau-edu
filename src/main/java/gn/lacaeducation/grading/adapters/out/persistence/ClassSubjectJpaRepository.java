package gn.lacaeducation.grading.adapters.out.persistence;

import gn.lacaeducation.academics.adapters.out.persistence.ClasseMatiereJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Read-only cross-module JPA repository that reads ClasseMatiere from the academics module's table.
 * Cross-module DB reads via a separate port/adapter are allowed in this architecture for read-only queries.
 */
interface ClassSubjectJpaRepository extends JpaRepository<ClasseMatiereJpaEntity, Long> {
    List<ClasseMatiereJpaEntity> findBySchoolClassId(Long schoolClassId);
}
