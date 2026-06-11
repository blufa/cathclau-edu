package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.grading.application.port.in.GradeUseCase;
import gn.lacaeducation.grading.application.port.out.GradeRepository;
import gn.lacaeducation.grading.domain.Grade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class GradeService implements GradeUseCase {

    private final GradeRepository repo;

    @Override
    public Grade enter(EnterGradeCommand c) {
        return repo.save(Grade.builder()
                .assessmentId(c.assessmentId())
                .studentId(c.studentId())
                .value(c.value())
                .validated(false)
                .enteredAt(LocalDateTime.now())
                .build());
    }

    @Override
    public List<Grade> enterBulk(Long assessmentId, List<BulkGradeEntry> entries) {
        List<Grade> grades = entries.stream()
                .map(e -> Grade.builder()
                        .assessmentId(assessmentId)
                        .studentId(e.studentId())
                        .value(e.value())
                        .validated(false)
                        .enteredAt(LocalDateTime.now())
                        .build())
                .toList();
        return repo.saveAll(grades);
    }

    @Override
    public List<Grade> validateAllForAssessment(Long assessmentId) {
        List<Grade> grades = repo.findByAssessmentId(assessmentId);
        grades.forEach(g -> g.setValidated(true));
        return repo.saveAll(grades);
    }

    @Override
    public Grade update(Long id, BigDecimal value) {
        Grade g = load(id);
        g.setValue(value);
        return repo.save(g);
    }

    @Override
    public Grade validate(Long id) {
        Grade g = load(id);
        g.setValidated(true);
        return repo.save(g);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Grade> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Grade> findByAssessment(Long a) {
        return repo.findByAssessmentId(a);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Grade> findByStudent(Long s) {
        return repo.findByStudentId(s);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Grade> findByStudentAndPeriod(Long studentId, Long periodId) {
        return repo.findByStudentIdAndPeriodId(studentId, periodId);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    private Grade load(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grade", id));
    }
}
