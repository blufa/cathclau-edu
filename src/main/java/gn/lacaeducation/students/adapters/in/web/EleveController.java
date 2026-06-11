package gn.lacaeducation.students.adapters.in.web;

import gn.lacaeducation.iam.TenantContext;
import gn.lacaeducation.students.application.port.in.EnrollmentUseCase;
import gn.lacaeducation.students.application.port.in.EnrollmentUseCase.EnrollCommand;
import gn.lacaeducation.students.application.port.in.StudentUseCase;
import gn.lacaeducation.students.application.port.in.StudentUseCase.AddDocumentCommand;
import gn.lacaeducation.students.application.port.in.StudentUseCase.CreateStudentCommand;
import gn.lacaeducation.students.domain.Enrollment;
import gn.lacaeducation.students.domain.Student;
import gn.lacaeducation.students.domain.StudentDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController @RequestMapping("/api/students") @RequiredArgsConstructor
public class EleveController {

    private final StudentUseCase studentUseCase;
    private final EnrollmentUseCase enrollmentUseCase;

    @PostMapping
    public ResponseEntity<Student> create(@RequestBody CreateStudentCommand cmd) {
        Student s = studentUseCase.create(new CreateStudentCommand(
                TenantContext.get(), cmd.userId(), cmd.studentNumber(), cmd.firstName(), cmd.lastName(),
                cmd.gender(), cmd.birthDate(), cmd.birthPlace(), cmd.address(), cmd.phone(), cmd.email(), cmd.nationality()));
        return ResponseEntity.created(URI.create("/api/students/" + s.getId())).body(s);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Student> findById(@PathVariable Long id) {
        return studentUseCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Student> findAll() { return studentUseCase.findByTenantId(TenantContext.get()); }

    @PutMapping("/{id}")
    public Student update(@PathVariable Long id, @RequestBody StudentUseCase.UpdateStudentCommand cmd) {
        return studentUseCase.update(id, cmd);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studentUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/documents")
    public StudentDocument addDocument(@PathVariable Long id, @RequestBody AddDocumentCommand cmd) {
        return studentUseCase.addDocument(new AddDocumentCommand(id, cmd.documentType(), cmd.fileUrl()));
    }

    @GetMapping("/{id}/documents")
    public List<StudentDocument> findDocuments(@PathVariable Long id) { return studentUseCase.findDocuments(id); }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long documentId) {
        studentUseCase.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{studentId}/enrollments")
    public ResponseEntity<Enrollment> enroll(@PathVariable Long studentId, @RequestBody EnrollCommand cmd) {
        Enrollment e = enrollmentUseCase.enroll(new EnrollCommand(studentId, cmd.classId(), cmd.academicYearId(), cmd.type()));
        return ResponseEntity.ok(e);
    }

    @GetMapping("/{studentId}/enrollments")
    public List<Enrollment> findEnrollments(@PathVariable Long studentId) { return enrollmentUseCase.findByStudent(studentId); }

    @PutMapping("/enrollments/{enrollmentId}/validate")
    public Enrollment validateEnrollment(@PathVariable Long enrollmentId) { return enrollmentUseCase.validate(enrollmentId); }

    @PutMapping("/enrollments/{enrollmentId}/cancel")
    public Enrollment cancelEnrollment(@PathVariable Long enrollmentId) { return enrollmentUseCase.cancel(enrollmentId); }

    @DeleteMapping("/enrollments/{enrollmentId}")
    public ResponseEntity<Void> deleteEnrollment(@PathVariable Long enrollmentId) {
        enrollmentUseCase.delete(enrollmentId);
        return ResponseEntity.noContent().build();
    }
}
