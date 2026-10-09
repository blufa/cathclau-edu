package gn.lacaeducation.communication.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.communication.application.port.out.PaymentReminderDataPort;
import gn.lacaeducation.communication.application.port.out.PaymentReminderDataPort.ClassFeeLine;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pour une classe donnée, tous ses élèves avec leur statut de paiement de la mensualité (à jour / en
 * retard / échéance proche) et le nombre de tranches déjà réglées — écran « Relances paiement »
 * (demande utilisateur : choisir une classe, puis filtrer/trier avant d'envoyer un SMS). Qu'un parent
 * paie en une fois (une tranche = une facture) ou mois par mois (opt-in paiement mensuel, une tranche
 * = plusieurs factures partageant le même installmentId) ne change rien au calcul : une tranche compte
 * "payée" seulement si TOUTES ses factures pour cet élève sont PAID.
 */
@Service
@RequiredArgsConstructor
public class PaymentReminderCandidatesService {

    /** Fenêtre par défaut pour détecter une échéance "qui approche" (pas encore en retard). */
    private static final int DEFAULT_UPCOMING_DAYS_AHEAD = 7;

    private final PaymentReminderDataPort dataPort;
    private final StudentDataProvider studentDataProvider;
    private final AcademicsDataProvider academicsDataProvider;

    public enum Status { OVERDUE, UPCOMING, UP_TO_DATE }

    public record ParentContact(Long parentId, String name, String phone) {}

    public record Candidate(Long studentId, String studentName, Long classId, String className,
                            Status status, BigDecimal amountDue, LocalDate dueDate, int daysLate,
                            int tranchesPaid, int totalTranches, List<ParentContact> parents) {}

    public List<Candidate> listByClass(Long tenantId, Long classId) {
        LocalDate today = LocalDate.now();
        List<ClassFeeLine> lines = dataPort.findClassMonthlyFeeLines(tenantId, classId);
        int totalTranches = dataPort.countMonthlyTranches(tenantId, classId);
        String className = academicsDataProvider.findClass(classId)
                .map(gn.lacaeducation.academics.ClassPublicInfo::label).orElse(null);

        Map<Long, List<ClassFeeLine>> linesByStudent = lines.stream()
                .collect(Collectors.groupingBy(ClassFeeLine::studentId));

        return studentDataProvider.findStudentsByClass(classId).stream()
                .map(s -> toCandidate(s, classId, className, linesByStudent.getOrDefault(s.id(), List.of()), totalTranches, today))
                .sorted(Comparator.comparing((Candidate c) -> c.status() == Status.OVERDUE ? 0 : c.status() == Status.UPCOMING ? 1 : 2)
                        .thenComparing(c -> c.dueDate() == null ? LocalDate.MAX : c.dueDate()))
                .toList();
    }

    private Candidate toCandidate(StudentPublicInfo student, Long classId, String className,
                                  List<ClassFeeLine> lines, int totalTranches, LocalDate today) {
        int tranchesPaid = (int) lines.stream()
                .collect(Collectors.groupingBy(ClassFeeLine::installmentId))
                .values().stream()
                .filter(group -> group.stream().allMatch(l -> "PAID".equals(l.status())))
                .count();

        List<ClassFeeLine> due = lines.stream()
                .filter(l -> !"PAID".equals(l.status()) && !"CANCELLED".equals(l.status()))
                .toList();
        List<ClassFeeLine> overdue = due.stream().filter(l -> l.dueDate().isBefore(today)).toList();
        List<ClassFeeLine> upcoming = due.stream()
                .filter(l -> !l.dueDate().isBefore(today) && !l.dueDate().isAfter(today.plusDays(DEFAULT_UPCOMING_DAYS_AHEAD)))
                .toList();

        Status status = !overdue.isEmpty() ? Status.OVERDUE : !upcoming.isEmpty() ? Status.UPCOMING : Status.UP_TO_DATE;
        List<ClassFeeLine> relevant = status == Status.OVERDUE ? overdue : status == Status.UPCOMING ? upcoming : List.of();
        BigDecimal amountDue = relevant.stream().map(ClassFeeLine::remainingBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate dueDate = relevant.stream().map(ClassFeeLine::dueDate).min(Comparator.naturalOrder()).orElse(null);
        int daysLate = dueDate != null ? (int) (today.toEpochDay() - dueDate.toEpochDay()) : 0;

        List<ParentContact> parents = studentDataProvider.hasPlatformAccessOptIn(student.id())
                ? studentDataProvider.findParentsByStudent(student.id()).stream()
                        .filter(p -> p.phone() != null && !p.phone().isBlank())
                        .map(p -> new ParentContact(p.id(), (p.firstName() + " " + p.lastName()).trim(), p.phone()))
                        .toList()
                : List.of();

        return new Candidate(student.id(), student.firstName() + " " + student.lastName(), classId, className,
                status, amountDue, dueDate, daysLate, tranchesPaid, totalTranches, parents);
    }
}
