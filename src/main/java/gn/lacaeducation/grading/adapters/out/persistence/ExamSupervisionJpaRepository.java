package gn.lacaeducation.grading.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ExamSupervisionJpaRepository extends JpaRepository<ExamSupervisionJpaEntity, Long> {
    List<ExamSupervisionJpaEntity> findByExamId(Long examId);
    List<ExamSupervisionJpaEntity> findByTeacherId(Long teacherId);
    boolean existsByExamIdAndTeacherId(Long examId, Long teacherId);
}
