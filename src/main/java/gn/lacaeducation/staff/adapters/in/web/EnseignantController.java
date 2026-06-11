package gn.lacaeducation.staff.adapters.in.web;

import gn.lacaeducation.iam.TenantContext;
import gn.lacaeducation.staff.application.port.in.TeacherUseCase;
import gn.lacaeducation.staff.application.port.in.TeacherUseCase.CreateTeacherCommand;
import gn.lacaeducation.staff.domain.Teacher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController @RequestMapping("/api/staff/teachers") @RequiredArgsConstructor
public class EnseignantController {

    private final TeacherUseCase teacherUseCase;

    @PostMapping
    public ResponseEntity<Teacher> create(@RequestBody CreateTeacherCommand cmd) {
        Teacher t = teacherUseCase.create(new CreateTeacherCommand(
                TenantContext.get(), cmd.userId(), cmd.staffNumber(), cmd.firstName(), cmd.lastName(),
                cmd.phone(), cmd.email(), cmd.specialization(), cmd.hireDate()));
        return ResponseEntity.created(URI.create("/api/staff/teachers/" + t.getId())).body(t);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Teacher> findById(@PathVariable Long id) {
        return teacherUseCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Teacher> findAll() { return teacherUseCase.findByTenantId(TenantContext.get()); }

    @PutMapping("/{id}")
    public Teacher update(@PathVariable Long id, @RequestBody TeacherUseCase.UpdateTeacherCommand cmd) {
        return teacherUseCase.update(id, cmd);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        teacherUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
