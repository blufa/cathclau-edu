package gn.lacaeducation.academics.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface AcademicsStudentJpaRepository extends JpaRepository<AcademicsStudentJpaEntity, Long> {
    List<AcademicsStudentJpaEntity> findByIdIn(List<Long> ids);
}
