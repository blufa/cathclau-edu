package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.academics.application.event.PeriodeClotureeEvent;
import gn.lacaeducation.grading.application.port.out.AssessmentRepository;
import gn.lacaeducation.grading.domain.Assessment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Listens to {@link PeriodeClotureeEvent} published by the academics module and
 * automatically triggers report-card generation for every class that has assessments
 * in the closed period.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PeriodClosedEventListener {

    private final AssessmentRepository assessmentRepository;
    private final GradeCalculationService calculationService;

    @Async
    @EventListener
    public void onPeriodClosed(PeriodeClotureeEvent event) {
        log.info("Received PeriodeClotureeEvent: periodId={}, academicYearId={}",
                event.periodId(), event.academicYearId());

        List<Assessment> assessments = assessmentRepository.findByPeriodId(event.periodId());
        if (assessments.isEmpty()) {
            log.info("No assessments found for period {}. Skipping report-card generation.", event.periodId());
            return;
        }

        List<Long> classIds = assessments.stream()
                .map(Assessment::getClassId)
                .distinct()
                .toList();

        for (Long classId : classIds) {
            try {
                log.info("Generating report cards for classId={}, periodId={}", classId, event.periodId());
                calculationService.generateForClass(classId, event.periodId());
                log.info("Report cards generated for classId={}", classId);
            } catch (Exception e) {
                log.error("Failed to generate report cards for classId={}, periodId={}: {}",
                        classId, event.periodId(), e.getMessage(), e);
            }
        }
    }
}
