package gn.lacaeducation.communication.application.service;

import gn.lacaeducation.communication.application.port.in.CommunicationUseCase.RecipientTarget;
import gn.lacaeducation.communication.application.port.out.PaymentReminderDataPort;
import gn.lacaeducation.communication.domain.BroadcastAudience;
import gn.lacaeducation.communication.domain.CommunicationChannel;
import gn.lacaeducation.staff.EmployeePublicInfoProvider;
import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import gn.lacaeducation.students.ParentPublicInfo;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Résout une {@link BroadcastAudience} en une liste de destinataires réels (téléphone ou e-mail selon
 * le canal). Combine les API publiques déjà exposées par {@code students} et {@code staff} — aucun
 * accès direct à leurs repositories.
 */
@Service
@RequiredArgsConstructor
class RecipientResolverService {

    /** Fenêtre par défaut pour l'audience UPCOMING_PAYERS (échéance qui approche) — cohérent avec
     * l'horizon "relance avant échéance" ; distinct des 30 jours du widget dashboard (lecture passive,
     * pas un envoi de SMS). */
    private static final int UPCOMING_PAYERS_DAYS_AHEAD = 7;

    private final StudentDataProvider studentDataProvider;
    private final TeacherPublicInfoProvider teacherPublicInfoProvider;
    private final EmployeePublicInfoProvider employeePublicInfoProvider;
    private final PaymentReminderDataPort paymentReminderDataPort;

    List<RecipientTarget> resolve(Long tenantId, BroadcastAudience audience, Long classId, Long studentId,
                                  CommunicationChannel channel) {
        if (audience == BroadcastAudience.ALL_TEACHERS) {
            return teacherPublicInfoProvider.listTeachers(tenantId).stream()
                    .map(t -> destinationOf(t.email(), t.phone(), channel))
                    .filter(d -> d != null && !d.isBlank())
                    .map(d -> new RecipientTarget(null, d))
                    .toList();
        }
        if (audience == BroadcastAudience.ALL_STAFF) {
            List<String> destinations = new ArrayList<>();
            teacherPublicInfoProvider.listTeachers(tenantId)
                    .forEach(t -> destinations.add(destinationOf(t.email(), t.phone(), channel)));
            employeePublicInfoProvider.listEmployees(tenantId).stream()
                    .filter(EmployeePublicInfoProvider.EmployeeInfo::active)
                    .forEach(e -> destinations.add(destinationOf(e.email(), e.phone(), channel)));
            return destinations.stream()
                    .filter(d -> d != null && !d.isBlank())
                    .map(d -> new RecipientTarget(null, d))
                    .toList();
        }
        if (audience == BroadcastAudience.OVERDUE_PAYERS) {
            List<Long> studentIds = paymentReminderDataPort.findOverdueLines(tenantId, java.time.LocalDate.now()).stream()
                    .map(PaymentReminderDataPort.DueLine::studentId).distinct().toList();
            return resolveParentsOf(studentIds, channel);
        }

        if (audience == BroadcastAudience.UPCOMING_PAYERS) {
            java.time.LocalDate today = java.time.LocalDate.now();
            List<Long> studentIds = paymentReminderDataPort
                    .findUpcomingLines(tenantId, today, today.plusDays(UPCOMING_PAYERS_DAYS_AHEAD)).stream()
                    .map(PaymentReminderDataPort.DueLine::studentId).distinct().toList();
            return resolveParentsOf(studentIds, channel);
        }

        if (audience == BroadcastAudience.ALL) {
            // Union parents + enseignants + personnel, dédupliquée par destination (un enseignant qui
            // est aussi parent d'élève dans la même école ne reçoit le message qu'une seule fois).
            Map<String, RecipientTarget> byDestination = new LinkedHashMap<>();
            resolve(tenantId, BroadcastAudience.ALL_PARENTS, null, null, channel)
                    .forEach(r -> byDestination.putIfAbsent(r.destination(), r));
            resolve(tenantId, BroadcastAudience.ALL_STAFF, null, null, channel)
                    .forEach(r -> byDestination.putIfAbsent(r.destination(), r));
            return List.copyOf(byDestination.values());
        }

        // Frais annexe optionnel (§billing) : la diffusion de masse (annonces, etc.) ne touche que les
        // familles ayant accepté l'accès à la plateforme — jamais les autres, quel que soit le public visé.
        List<ParentPublicInfo> parents = switch (audience) {
            case ALL_PARENTS -> studentDataProvider.findAllParentsWithPlatformAccess(tenantId);
            case CLASS_PARENTS -> parentsOfClass(classId, null);
            case CLASS_PARENTS_BOYS -> parentsOfClass(classId, "MALE");
            case CLASS_PARENTS_GIRLS -> parentsOfClass(classId, "FEMALE");
            case STUDENT_PARENTS -> studentDataProvider.hasPlatformAccessOptIn(studentId)
                    ? studentDataProvider.findParentsByStudent(studentId) : List.of();
            case ALL_TEACHERS, ALL_STAFF, ALL, OVERDUE_PAYERS, UPCOMING_PAYERS -> throw new IllegalStateException("traité plus haut");
        };

        return parents.stream()
                .map(p -> destinationOf(p.email(), p.phone(), channel))
                .filter(d -> d != null && !d.isBlank())
                .map(d -> new RecipientTarget(null, d))
                .toList();
    }

    /** Parents des élèves d'une classe, filtrés par sexe si fourni, dédupliqués par identifiant parent
     * (un parent avec plusieurs enfants dans la classe ne reçoit qu'un seul message). */
    private List<ParentPublicInfo> parentsOfClass(Long classId, String genderFilter) {
        List<StudentPublicInfo> students = studentDataProvider.findStudentsByClass(classId);
        Map<Long, ParentPublicInfo> byParentId = new LinkedHashMap<>();
        for (StudentPublicInfo s : students) {
            if (genderFilter != null && !genderFilter.equalsIgnoreCase(s.gender())) continue;
            if (!studentDataProvider.hasPlatformAccessOptIn(s.id())) continue;
            for (ParentPublicInfo p : studentDataProvider.findParentsByStudent(s.id())) {
                byParentId.putIfAbsent(p.id(), p);
            }
        }
        return List.copyOf(byParentId.values());
    }

    /** Parents de ces élèves (dédupliqués), même règle de confidentialité que les autres audiences
     * élève (§billing, frais annexe optionnel) : un parent n'ayant pas accepté l'accès plateforme pour
     * cet élève ne reçoit jamais de notification, retard de paiement ou pas. */
    private List<RecipientTarget> resolveParentsOf(List<Long> studentIds, CommunicationChannel channel) {
        Map<Long, ParentPublicInfo> byParentId = new LinkedHashMap<>();
        for (Long studentId : studentIds) {
            if (!studentDataProvider.hasPlatformAccessOptIn(studentId)) continue;
            for (ParentPublicInfo p : studentDataProvider.findParentsByStudent(studentId)) {
                byParentId.putIfAbsent(p.id(), p);
            }
        }
        return byParentId.values().stream()
                .map(p -> destinationOf(p.email(), p.phone(), channel))
                .filter(d -> d != null && !d.isBlank())
                .map(d -> new RecipientTarget(null, d))
                .toList();
    }

    private static String destinationOf(String email, String phone, CommunicationChannel channel) {
        return channel == CommunicationChannel.EMAIL ? email : phone;
    }
}
