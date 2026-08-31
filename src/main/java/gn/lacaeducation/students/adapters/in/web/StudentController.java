package gn.lacaeducation.students.adapters.in.web;

import gn.lacaeducation.common.PageResponse;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.students.application.port.in.EnrollmentUseCase;
import gn.lacaeducation.students.application.port.in.ParentUseCase;
import gn.lacaeducation.students.application.port.in.StudentUseCase;
import gn.lacaeducation.students.domain.Enrollment;
import gn.lacaeducation.students.domain.EnrollmentStatus;
import gn.lacaeducation.students.domain.Gender;
import gn.lacaeducation.students.domain.Parent;
import gn.lacaeducation.students.domain.Relationship;
import gn.lacaeducation.students.domain.Student;
import gn.lacaeducation.students.domain.StudentDocument;
import gn.lacaeducation.students.domain.StudentParent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
    private final ParentUseCase parentUseCase;
    private final gn.lacaeducation.students.application.service.StudentCertificateService certificateService;
    private final gn.lacaeducation.students.application.service.StudentImportService importService;

    public record CertificateRequest(String schoolName, String motif) {}

    @Operation(summary = "Generate a school-leaving / transfer certificate (PDF) for a student")
    @PostMapping("/{id}/certificat-radiation")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    public java.util.Map<String, String> transferCertificate(@PathVariable Long id, @RequestBody(required = false) CertificateRequest req) {
        String url = certificateService.generateTransferCertificate(id,
                req == null ? null : req.schoolName(), req == null ? null : req.motif());
        return java.util.Map.of("url", url);
    }

    @Operation(summary = "Bulk import students from a CSV file (returns a per-row report)")
    @PostMapping(value = "/import", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN','SECRETARY','DIRECTOR')")
    public gn.lacaeducation.students.application.service.StudentImportService.ImportReport importCsv(
            @org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return importService.importCsv(file);
    }

    @Operation(summary = "Register a new student (checks enrollment quota)")
    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','SECRETARY','DIRECTOR')")
    public ResponseEntity<Student> create(@Valid @RequestBody StudentUseCase.CreateStudentCommand cmd) {
        Student s = studentUseCase.create(new StudentUseCase.CreateStudentCommand(
                TenantContext.get(), cmd.userId(), cmd.studentNumber(), cmd.firstName(), cmd.lastName(),
                cmd.gender(), cmd.birthDate(), cmd.birthPlace(), cmd.address(), cmd.phone(), cmd.email(), cmd.nationality(), cmd.originSchool(), cmd.photo(),
                cmd.birthCertificateNumber(), cmd.bloodGroup(), cmd.neighborhood(), cmd.landmark(),
                cmd.previousClass(), cmd.repeating(), cmd.transferReason(), cmd.medicalNotes(),
                cmd.createLogin(), cmd.loginPassword()));
        return ResponseEntity.created(URI.create("/api/v1/students/" + s.getId())).body(s);
    }

    @Operation(summary = "List students in current tenant (filters: class, enrollment status, gender, name/matricule search)")
    @GetMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY','TEACHER')")
    public PageResponse<Student> findAll(@RequestParam(required = false) Long classId,
                                         @RequestParam(required = false) EnrollmentStatus enrollmentStatus,
                                         @RequestParam(required = false) Gender gender,
                                         @RequestParam(required = false) String search,
                                         @RequestParam(required = false, defaultValue = "false") boolean notEnrolled,
                                         @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(studentUseCase.search(
                TenantContext.get(), gender, search, classId, enrollmentStatus, notEnrolled, pageable));
    }

    @Operation(summary = "List students with their current class and principal parent (guichet d'accueil)")
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY','ACCOUNTANT','TEACHER')")
    public PageResponse<StudentSummaryResponse> summary(@RequestParam(required = false) String search,
                                                         @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(studentUseCase.search(TenantContext.get(), null, search, null, null, false, pageable)
                .map(s -> {
                    Long classId = enrollmentUseCase.findByStudent(s.getId()).stream()
                            .filter(e -> e.getStatus() == EnrollmentStatus.VALIDATED)
                            .reduce((first, last) -> last)
                            .map(Enrollment::getClassId)
                            .orElse(null);
                    List<Parent> parents = parentUseCase.findByStudent(s.getId());
                    Parent principal = parents.stream().filter(Parent::isPrimaryGuardian).findFirst()
                            .or(() -> parents.stream().findFirst())
                            .orElse(null);
                    return new StudentSummaryResponse(s, classId,
                            principal == null ? null : (principal.getFirstName() + " " + principal.getLastName()).trim(),
                            principal == null ? null : principal.getPhone());
                }));
    }

    /** Row shown at the guichet d'accueil: student + current class + principal (or first) linked parent. */
    public record StudentSummaryResponse(Student student, Long classId, String principalParentName, String principalParentPhone) {}

    @Operation(summary = "Get the student record of the currently authenticated user (student portal)")
    @GetMapping("/me")
    public ResponseEntity<Student> me(@AuthenticationPrincipal Jwt jwt) {
        Long userId;
        try { userId = Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { userId = null; }
        return studentUseCase.findByUserId(userId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get a student's full record (enrollments, parents, documents)")
    @GetMapping("/{id}")
    public ResponseEntity<StudentDetailResponse> findById(@PathVariable Long id) {
        return studentUseCase.findById(id)
                .map(s -> ResponseEntity.ok(new StudentDetailResponse(
                        s,
                        enrollmentUseCase.findByStudent(id),
                        parentUseCase.findByStudent(id),
                        studentUseCase.findDocuments(id))))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Aggregated student record returned by GET /students/{id}. */
    public record StudentDetailResponse(Student student, List<Enrollment> enrollments,
                                        List<Parent> parents, List<StudentDocument> documents) {}

    @Operation(summary = "Update student information")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','SECRETARY','DIRECTOR')")
    public Student update(@PathVariable Long id, @Valid @RequestBody StudentUseCase.UpdateStudentCommand cmd) {
        return studentUseCase.update(id, cmd);
    }

    @Operation(summary = "Update a student's photo URL")
    @PatchMapping("/{id}/photo")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','SECRETARY','DIRECTOR')")
    public Student updatePhoto(@PathVariable Long id, @RequestParam String photoUrl) {
        return studentUseCase.updatePhoto(id, photoUrl);
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

    // ---- Parents ----

    @Operation(summary = "Link a parent to this student (PERE/MERE/TUTEUR)")
    @PostMapping("/{studentId}/parents")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','SECRETARY','DIRECTOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentParent linkParent(@PathVariable Long studentId,
                                     @RequestParam Long parentId,
                                     @RequestParam Relationship relationship) {
        return parentUseCase.linkStudent(studentId, parentId, relationship);
    }

    @Operation(summary = "Get the parents of this student")
    @GetMapping("/{studentId}/parents")
    public List<Parent> findParents(@PathVariable Long studentId) {
        return parentUseCase.findByStudent(studentId);
    }

    @Operation(summary = "Unlink a parent from this student")
    @DeleteMapping("/{studentId}/parents/{parentId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlinkParent(@PathVariable Long studentId, @PathVariable Long parentId) {
        parentUseCase.unlinkStudentFromParent(studentId, parentId);
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
