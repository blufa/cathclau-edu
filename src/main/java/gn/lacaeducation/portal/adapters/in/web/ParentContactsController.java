package gn.lacaeducation.portal.adapters.in.web;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ClassSubjectPublicInfo;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.iam.SchoolStaffDirectory;
import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import gn.lacaeducation.students.ParentAccessProvider;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Annuaire des contacts de l'établissement pour le parent connecté (§portal) : pour chaque enfant,
 * les enseignants de sa classe (une matière = un enseignant, dédoublonné) avec leur téléphone, plus
 * la direction de l'école (directeur, responsable pédagogique). Lecture seule, strictement cloisonné
 * aux enfants rattachés au compte.
 */
@Tag(name = "Parent Portal", description = "Annuaire des contacts (enseignants de la classe, direction)")
@RestController
@RequestMapping("/api/v1/parent")
@RequiredArgsConstructor
public class ParentContactsController {

    private final ParentAccessProvider parentAccessProvider;
    private final StudentDataProvider studentDataProvider;
    private final AcademicsDataProvider academicsDataProvider;
    private final TeacherPublicInfoProvider teacherPublicInfoProvider;
    private final SchoolStaffDirectory schoolStaffDirectory;

    public record TeacherContact(Long teacherId, String fullName, String phone, String subject) {}
    public record ChildContacts(Long studentId, String firstName, String lastName, String className,
                                List<TeacherContact> teachers) {}
    public record LeadershipContact(String fullName, String phone, String role) {}
    public record ParentContacts(List<ChildContacts> children, List<LeadershipContact> leadership) {}

    @Operation(summary = "Contacts joignables de l'établissement pour le parent connecté : enseignants "
            + "de la classe de chaque enfant (téléphone) et direction de l'école (directeur, "
            + "responsable pédagogique)")
    @PreAuthorize("hasRole('PARENT')")
    @GetMapping("/contacts")
    public ParentContacts contacts(@AuthenticationPrincipal Jwt jwt) {
        Long userId = currentUserId(jwt);
        List<ChildContacts> children = parentAccessProvider.findChildStudentIds(userId).stream()
                .map(this::childContacts)
                .filter(java.util.Objects::nonNull)
                .toList();

        Long tenantId = TenantContext.get();
        List<LeadershipContact> leadership = schoolStaffDirectory.findLeadershipContacts(tenantId).stream()
                .map(c -> new LeadershipContact(c.fullName(), c.phone(), c.role()))
                .toList();

        return new ParentContacts(children, leadership);
    }

    private ChildContacts childContacts(Long studentId) {
        StudentPublicInfo student = studentDataProvider.findStudent(studentId).orElse(null);
        if (student == null) return null;

        Long classId = studentDataProvider.findCurrentClassId(studentId).orElse(null);
        if (classId == null) {
            return new ChildContacts(studentId, student.firstName(), student.lastName(), null, List.of());
        }
        String className = academicsDataProvider.findClass(classId).map(c -> c.label()).orElse(null);

        // Une matière = un enseignant (ClasseMatiere) ; un même enseignant peut couvrir plusieurs
        // matières de la classe — dédoublonné par teacherId, matières concaténées pour affichage.
        Map<Long, List<String>> subjectsByTeacher = new LinkedHashMap<>();
        Map<Long, String> subjectLabels = academicsDataProvider.subjectLabels(
                academicsDataProvider.findClassSubjects(classId).stream().map(ClassSubjectPublicInfo::subjectId).toList());
        for (ClassSubjectPublicInfo cs : academicsDataProvider.findClassSubjects(classId)) {
            if (cs.teacherId() == null) continue;
            String subjectLabel = subjectLabels.getOrDefault(cs.subjectId(), "Matière");
            subjectsByTeacher.computeIfAbsent(cs.teacherId(), k -> new java.util.ArrayList<>()).add(subjectLabel);
        }
        List<TeacherContact> teachers = subjectsByTeacher.entrySet().stream()
                .map(e -> teacherPublicInfoProvider.findTeacher(e.getKey())
                        .map(t -> new TeacherContact(t.id(), (safe(t.firstName()) + " " + safe(t.lastName())).trim(),
                                t.phone(), String.join(", ", e.getValue())))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();

        return new ChildContacts(studentId, student.firstName(), student.lastName(), className, teachers);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private Long currentUserId(Jwt jwt) {
        if (jwt == null) return null;
        try { return Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { return null; }
    }
}
