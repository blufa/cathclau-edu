package gn.lacaeducation.academics.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.academics.application.port.in.GererClasseUseCase;
import gn.lacaeducation.academics.application.port.in.GererClasseUseCase.CreateClassCommand;
import gn.lacaeducation.academics.application.port.in.GererClasseUseCase.EnrollmentCountInfo;
import gn.lacaeducation.academics.domain.Classe;
import gn.lacaeducation.academics.domain.ClasseMatiere;
import gn.lacaeducation.academics.domain.ClassStudent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@Tag(name = "Classes", description = "Manage school classes, subject assignments, and class rosters")
@RestController
@RequestMapping("/api/v1/academics/classes")
@RequiredArgsConstructor
public class ClasseController {

    private final GererClasseUseCase useCase;

    @Operation(summary = "Create a new class for an academic year")
    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    public ResponseEntity<Classe> create(@Valid @RequestBody CreateClassCommand cmd) {
        Classe classe = useCase.create(new CreateClassCommand(
                TenantContext.get(), cmd.levelId(), cmd.academicYearId(),
                cmd.headTeacherId(), cmd.roomId(), cmd.label(), cmd.maxCapacity(), cmd.classType()
        ));
        return ResponseEntity.created(URI.create("/api/academics/classes/" + classe.getId())).body(classe);
    }

    @Operation(summary = "Get a class by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Classe> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "List all classes for a given academic year")
    @GetMapping
    public List<Classe> findByAcademicYear(@RequestParam Long academicYearId) {
        return useCase.findByAcademicYear(academicYearId);
    }

    @Operation(summary = "Update a class")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    public Classe update(@PathVariable Long id, @Valid @RequestBody CreateClassCommand cmd) {
        return useCase.update(id, new CreateClassCommand(
                TenantContext.get(), cmd.levelId(), cmd.academicYearId(),
                cmd.headTeacherId(), cmd.roomId(), cmd.label(), cmd.maxCapacity(), cmd.classType()));
    }

    @Operation(summary = "Désigner (ou retirer) le professeur principal de la classe, depuis le tableau matières/enseignants")
    @PatchMapping("/{id}/head-teacher")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    public Classe setHeadTeacher(@PathVariable Long id, @RequestParam(required = false) Long teacherId) {
        Classe c = useCase.findById(id)
                .orElseThrow(() -> new gn.lacaeducation.common.ResourceNotFoundException("Classe", id));
        return useCase.update(id, new CreateClassCommand(TenantContext.get(), c.getLevelId(), c.getAcademicYearId(),
                teacherId, c.getRoomId(), c.getLabel(), c.getMaxCapacity(), c.getClassType()));
    }

    @Operation(summary = "Désigner (ou retirer, sans studentId) le chef de classe — un élève inscrit (validé) dans cette classe")
    @PatchMapping("/{id}/chef-de-classe")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    public Classe setClassRepresentative(@PathVariable Long id, @RequestParam(required = false) Long studentId) {
        return useCase.setClassRepresentative(id, studentId);
    }

    @Operation(summary = "Delete a class")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { useCase.delete(id); }

    @Operation(summary = "List all students enrolled in a class (validated enrollments)")
    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','TEACHER','SECRETARY')")
    public List<ClassStudent> findStudents(@PathVariable Long id) {
        return useCase.findStudentsByClassId(id);
    }

    @Operation(summary = "Get enrollment count vs capacity for a class")
    @GetMapping("/{id}/enrollment-count")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    public EnrollmentCountInfo getEnrollmentCount(@PathVariable Long id) {
        return useCase.getEnrollmentCount(id);
    }

    @Operation(summary = "Assign a subject (and optionally a teacher) to a class")
    @PostMapping("/{classId}/subjects")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    public ResponseEntity<ClasseMatiere> assignSubject(
            @PathVariable Long classId,
            @RequestParam Long subjectId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) java.math.BigDecimal coefficient,
            @RequestParam(required = false) String code) {
        return ResponseEntity.ok(useCase.assignSubject(classId, subjectId, teacherId, coefficient, code));
    }

    @Operation(summary = "List all subjects assigned to a class")
    @GetMapping("/{classId}/subjects")
    public List<ClasseMatiere> findSubjects(@PathVariable Long classId) {
        return useCase.findSubjects(classId);
    }

    @Operation(summary = "Remove a subject assignment from a class")
    @DeleteMapping("/{classId}/subjects/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSubject(@PathVariable Long classId, @PathVariable Long id) {
        useCase.removeSubject(id);
    }
}
