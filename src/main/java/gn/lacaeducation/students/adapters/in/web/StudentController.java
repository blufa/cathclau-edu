package gn.lacaeducation.students.adapters.in.web;

import gn.lacaeducation.common.PageResponse;
import gn.lacaeducation.iam.TenantContext;
import gn.lacaeducation.students.application.port.in.EnrollmentUseCase;
import gn.lacaeducation.students.application.port.in.StudentUseCase;
import gn.lacaeducation.students.domain.Enrollment;
import gn.lacaeducation.students.domain.Student;
import gn.lacaeducation.students.domain.StudentDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Students", description = "Student registration and enrollment management")
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentUseCase studentUseCase;
    private final EnrollmentUseCase enrollmentUseCase;

    @Operation(summary = "Register a new student (checks enrollment quota)")
    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','SECRETARY','DIRECTOR')")
    public ResponseEntity<Student> create(@Valid @RequestBody StudentUseCase.CreateStudentCommand cmd) {
        Student s = studentUseCase.create(new StudentUseCase.CreateStudentCommand(
                TenantContext.get(), cmd.userId(), cmd.studentNumber(), cmd.firstName(), cmd.lastName(),
                cmd.gender(), cmd.birthDate(), cmd.birthPlace(), cmd.address(), cmd.phone(), cmd.email(), cmd.nationality()));
        return ResponseEntity.created(URI.create("/api/v1/students/" + s.getId())).body(s);
    }

    @Operation(summary = "List all students in current tenant (paginated)")
    @GetMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY','TEACHER')")
    public PageResponse<Student> findAll(@PageableDefault(size = 20) Pageable pageable) {
        List<Student> list = studentUseCase.findByTenantId(TenantContext.get());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        return PageResponse.of(new PageImpl<>(list.subList(start, end), pageable, list.size()));
    }

    @Operation(summary = "Get a student by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Student> findById(@PathVariable Long id) {
        return studentUseCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update student information")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','SECRETARY','DIRECTOR')")
    public Student update(@PathVariable Long id, @Valid @RequestBody StudentUseCase.UpdateStudentCommand cmd) {
        return studentUseCase.update(id, cmd);
    }

    @Operation(summary = "Delete (archive) a student")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { studentUseCase.delete(id); }

    // ---- Documents ----

    @Operation(summary = "Upload a document for a student")
    @PostMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','SECRETARY','DIRECTOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentDocument addDocument(@PathVariable Long id,
                                        @Valid @RequestBody StudentUseCase.AddDocumentCommand cmd) {
        return studentUseCase.addDocument(new StudentUseCase.AddDocumentCommand(id, cmd.documentType(), cmd.fileUrl()));
    }

    @Operation(summary = "List documents for a student")
    @GetMapping("/{id}/documents")
    public List<StudentDocument> findDocuments(@PathVariable Long id) { return studentUseCase.findDocuments(id); }

    @Operation(summary = "Delete a student document")
    @DeleteMapping("/{id}/documents/{documentId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','SECRETARY','DIRECTOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(@PathVariable Long id, @PathVariable Long documentId) {
        studentUseCase.deleteDocument(documentId);
    }

    // ---- Enrollments ----

    @Operation(summary = "Enroll a student in a class for an academic year")
    @PostMapping("/{studentId}/enrollments")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','SECRETARY','DIRECTOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public Enrollment enroll(@PathVariable Long studentId, @Valid @RequestBody EnrollmentUseCase.EnrollCommand cmd) {
        return enrollmentUseCase.enroll(new EnrollmentUseCase.EnrollCommand(
                studentId, cmd.classId(), cmd.academicYearId(), cmd.type()));
    }

    @Operation(summary = "Get all enrollments for a student")
    @GetMapping("/{studentId}/enrollments")
    public List<Enrollment> findEnrollments(@PathVariable Long studentId) {
        return enrollmentUseCase.findByStudent(studentId);
    }

    @Operation(summary = "Validate a pending enrollment")
    @PatchMapping("/enrollments/{enrollmentId}/validate")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    public Enrollment validateEnrollment(@PathVariable Long enrollmentId) {
        return enrollmentUseCase.validate(enrollmentId);
    }

    @Operation(summary = "Cancel an enrollment")
    @PatchMapping("/enrollments/{enrollmentId}/cancel")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    public Enrollment cancelEnrollment(@PathVariable Long enrollmentId) {
        return enrollmentUseCase.cancel(enrollmentId);
    }

    @Operation(summary = "Delete an enrollment")
    @DeleteMapping("/enrollments/{enrollmentId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEnrollment(@PathVariable Long enrollmentId) { enrollmentUseCase.delete(enrollmentId); }
}
