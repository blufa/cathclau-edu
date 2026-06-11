package gn.lacaeducation.students.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface DocumentEleveJpaRepository extends JpaRepository<DocumentEleveJpaEntity, Long> {
    List<DocumentEleveJpaEntity> findByStudentId(Long studentId);
}
