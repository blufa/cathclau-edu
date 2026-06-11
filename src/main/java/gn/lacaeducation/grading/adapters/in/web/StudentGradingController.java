package gn.lacaeducation.grading.adapters.in.web;

import gn.lacaeducation.grading.application.port.in.GradeUseCase;
import gn.lacaeducation.grading.application.port.in.ReportCardUseCase;
import gn.lacaeducation.grading.domain.Grade;
import gn.lacaeducation.grading.domain.ReportCard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Student-centric grading endpoints.
 * Exposes grade and report-card queries under /api/v1/students/{id}/...
 * so that student and parent portals get a clean, resource-oriented URL.
 */
@Tag(name = "Student Grading", description = "Student-centric views of grades and report cards")
@RestController
@RequestMapping("/api/v1/students/{studentId}")
@RequiredArgsConstructor
public class StudentGradingController {

    private final GradeUseCase gradeUseCase;
    private final ReportCardUseCase reportCardUseCase;

    @Operation(summary = "List all grades for a student, optionally filtered by periodId")
    @GetMapping("/grades")
    public List<Grade> getGrades(@PathVariable Long studentId,
                                  @RequestParam(required = false) Long periodId) {
        if (periodId != null) {
            return gradeUseCase.findByStudentAndPeriod(studentId, periodId);
        }
        return gradeUseCase.findByStudent(studentId);
    }

    @Operation(summary = "List all report cards for a student across all periods")
    @GetMapping("/report-cards")
    public List<ReportCard> getReportCards(@PathVariable Long studentId) {
        return reportCardUseCase.findByStudentId(studentId);
    }
}
