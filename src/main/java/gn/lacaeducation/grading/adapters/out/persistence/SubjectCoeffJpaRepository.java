package gn.lacaeducation.grading.adapters.out.persistence;

import gn.lacaeducation.academics.adapters.out.persistence.MatiereJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Read-only cross-module JPA repository for subject coefficients (subjects table).
 */
interface SubjectCoeffJpaRepository extends JpaRepository<MatiereJpaEntity, Long> {
}
