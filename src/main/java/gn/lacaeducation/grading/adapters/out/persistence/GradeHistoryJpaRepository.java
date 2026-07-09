package gn.lacaeducation.grading.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface GradeHistoryJpaRepository extends JpaRepository<GradeHistoryJpaEntity, Long> {
    List<GradeHistoryJpaEntity> findByGradeIdOrderByChangedAtDesc(Long gradeId);
    List<GradeHistoryJpaEntity> findByAssessmentIdOrderByChangedAtDesc(Long assessmentId);
    List<GradeHistoryJpaEntity> findByStudentIdOrderByChangedAtDesc(Long studentId);
}
