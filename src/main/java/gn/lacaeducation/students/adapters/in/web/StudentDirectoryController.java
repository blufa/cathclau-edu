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
 *
 * Role-restricted to SCHOOL_ADMIN/DIRECTOR/SECRETARY (§1.2/1.3 cahier des charges) — matching the
 * "Annuaire inter-écoles" nav entry exactly. {@code eleve:read} alone used to be the only gate, but
 * that permission is also granted to TEACHER (to look up their own students) and PARENT, which would
 * have let either query transferred-student financial/academic data (unpaid months, averages) across
 * EVERY school on the platform, not just their own tenant.
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
    @PreAuthorize("hasAuthority('eleve:read') and hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
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
    @PreAuthorize("hasAuthority('eleve:read') and hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    public List<String> searchSchools(@RequestParam(required = false) String q) {
        return service.searchSchools(q);
    }

    @Operation(summary = "Suggest platform 'previous class' labels already entered (ex. CM2, 10ème)")
    @GetMapping("/previous-classes")
    @PreAuthorize("hasAuthority('eleve:read') and hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    public List<String> searchPreviousClasses(@RequestParam(required = false) String q) {
        return service.searchPreviousClasses(q);
    }

    @Operation(summary = "Suggest first names already used by another student/parent of this tenant")
    @GetMapping("/first-names")
    @PreAuthorize("hasAuthority('eleve:read') and hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    public List<String> suggestFirstNames(@RequestParam(required = false) String q) {
        return service.suggestFirstNames(q);
    }

    @Operation(summary = "Suggest last names already used by another student/parent of this tenant")
    @GetMapping("/last-names")
    @PreAuthorize("hasAuthority('eleve:read') and hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    public List<String> suggestLastNames(@RequestParam(required = false) String q) {
        return service.suggestLastNames(q);
    }

    @Operation(summary = "Suggest addresses already used by another student/parent of this tenant")
    @GetMapping("/addresses")
    @PreAuthorize("hasAuthority('eleve:read') and hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    public List<String> suggestAddresses(@RequestParam(required = false) String q) {
        return service.suggestAddresses(q);
    }
}
