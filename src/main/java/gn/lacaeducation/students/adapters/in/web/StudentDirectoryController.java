package gn.lacaeducation.students.adapters.in.web;

import gn.lacaeducation.students.application.service.StudentDirectoryService;
import gn.lacaeducation.students.domain.StudentDirectoryEntry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Cross-school student directory (features §E): a school looks up a transferred student across the
 * platform (exact matricule or name) to see their previous school and last class/decision.
 */
@Tag(name = "Student Directory", description = "Cross-school lookup of a transferred student")
@RestController
@RequestMapping("/api/v1/directory")
@RequiredArgsConstructor
public class StudentDirectoryController {

    private final StudentDirectoryService service;

    public record DirectoryEntryResponse(String firstName, String lastName, String studentNumber,
                                         java.time.LocalDate birthDate, String originSchool, String schoolName,
                                         String lastClass, String lastDecision,
                                         int unpaidMonths, java.math.BigDecimal overallAverage) {}

    @Operation(summary = "Search a student across all schools (exact matricule or name ≥ 3 chars)")
    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    public List<DirectoryEntryResponse> search(@RequestParam(required = false) String matricule,
                                               @RequestParam(required = false) String name) {
        return service.search(matricule, name).stream()
                .map(e -> new DirectoryEntryResponse(e.getFirstName(), e.getLastName(), e.getStudentNumber(),
                        e.getBirthDate(), e.getOriginSchool(), e.getSchoolName(), e.getLastClass(), e.getLastDecision(),
                        e.getUnpaidMonths(), e.getOverallAverage()))
                .toList();
    }

    @Operation(summary = "Suggest platform school names (for a transferred student's origin school)")
    @GetMapping("/schools")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    public List<String> searchSchools(@RequestParam(required = false) String q) {
        return service.searchSchools(q);
    }
}
