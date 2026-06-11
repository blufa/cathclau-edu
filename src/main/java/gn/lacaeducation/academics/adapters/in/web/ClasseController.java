package gn.lacaeducation.academics.adapters.in.web;

import gn.lacaeducation.iam.TenantContext;
import gn.lacaeducation.academics.application.port.in.GererClasseUseCase;
import gn.lacaeducation.academics.application.port.in.GererClasseUseCase.CreateClassCommand;
import gn.lacaeducation.academics.domain.Classe;
import gn.lacaeducation.academics.domain.ClasseMatiere;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/academics/classes")
@RequiredArgsConstructor
public class ClasseController {

    private final GererClasseUseCase useCase;

    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    public ResponseEntity<Classe> create(@Valid @RequestBody CreateClassCommand cmd) {
        Classe classe = useCase.create(new CreateClassCommand(
                TenantContext.get(), cmd.levelId(), cmd.academicYearId(),
                cmd.headTeacherId(), cmd.roomId(), cmd.label(), cmd.maxCapacity()
        ));
        return ResponseEntity.created(URI.create("/api/academics/classes/" + classe.getId())).body(classe);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Classe> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Classe> findByAcademicYear(@RequestParam Long academicYearId) {
        return useCase.findByAcademicYear(academicYearId);
    }

    @PostMapping("/{classId}/subjects")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    public ResponseEntity<ClasseMatiere> assignSubject(
            @PathVariable Long classId,
            @RequestParam Long subjectId,
            @RequestParam(required = false) Long teacherId) {
        return ResponseEntity.ok(useCase.assignSubject(classId, subjectId, teacherId));
    }

    @GetMapping("/{classId}/subjects")
    public List<ClasseMatiere> findSubjects(@PathVariable Long classId) {
        return useCase.findSubjects(classId);
    }
}
