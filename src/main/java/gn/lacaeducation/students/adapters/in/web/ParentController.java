package gn.lacaeducation.students.adapters.in.web;

import gn.lacaeducation.iam.TenantContext;
import gn.lacaeducation.students.application.port.in.ParentUseCase;
import gn.lacaeducation.students.application.port.in.ParentUseCase.CreateParentCommand;
import gn.lacaeducation.students.domain.Parent;
import gn.lacaeducation.students.domain.Relationship;
import gn.lacaeducation.students.domain.StudentParent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController @RequestMapping("/api/students/parents") @RequiredArgsConstructor
public class ParentController {

    private final ParentUseCase parentUseCase;

    @PostMapping
    public ResponseEntity<Parent> create(@RequestBody CreateParentCommand cmd) {
        Parent p = parentUseCase.create(new CreateParentCommand(
                TenantContext.get(), cmd.userId(), cmd.firstName(), cmd.lastName(),
                cmd.phone(), cmd.email(), cmd.occupation(), cmd.address()));
        return ResponseEntity.created(URI.create("/api/students/parents/" + p.getId())).body(p);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Parent> findById(@PathVariable Long id) {
        return parentUseCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Parent> findAll() { return parentUseCase.findByTenantId(TenantContext.get()); }

    @PutMapping("/{id}")
    public Parent update(@PathVariable Long id, @RequestBody ParentUseCase.UpdateParentCommand cmd) {
        return parentUseCase.update(id, cmd);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        parentUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{parentId}/students/{studentId}")
    public StudentParent linkStudent(@PathVariable Long parentId, @PathVariable Long studentId,
                                     @RequestParam Relationship relationship) {
        return parentUseCase.linkStudent(studentId, parentId, relationship);
    }

    @DeleteMapping("/student-parents/{id}")
    public ResponseEntity<Void> unlinkStudent(@PathVariable Long id) {
        parentUseCase.unlinkStudent(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-student/{studentId}")
    public List<Parent> findByStudent(@PathVariable Long studentId) { return parentUseCase.findByStudent(studentId); }
}
