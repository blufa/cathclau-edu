package gn.lacaeducation.students.application.service;

import gn.lacaeducation.students.application.port.out.DuplicateDetectionPort;
import gn.lacaeducation.students.application.port.out.DuplicateDetectionPort.StudentFinancialRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Détection de doublons élèves (§retour comptable, requête élargie : même prénom, même nom, ET même
 * nom de famille d'un parent — plus limité à la même classe, deux fiches du même élève pouvant être
 * réparties sur deux classes différentes). Une homonymie seule n'est jamais traitée comme un doublon
 * probable. Le signal retenu est l'écart de facturation RÉELLE au sein du groupe : si un membre du
 * groupe a nettement moins de mensualités facturées que les autres, sa fiche est probablement une
 * fiche "vide" créée par erreur (doublon), jamais l'inverse. Rien n'est jamais supprimé
 * automatiquement — seulement signalé pour décision.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DuplicateDetectionService {

    private final DuplicateDetectionPort port;

    public record Candidate(Long studentId, String firstName, String lastName, String photo,
                            Long classId, String className,
                            java.math.BigDecimal totalInvoiced, java.math.BigDecimal totalPaid,
                            long monthlyInvoiceCount, boolean suspect) {}

    public record Group(String firstName, String lastName, String parentLastName, List<Candidate> members) {}

    public List<Group> findDuplicateGroups(Long tenantId) {
        List<StudentFinancialRow> rows = port.findDuplicateCandidates(tenantId);
        // Regroupe par (prénom normalisé, nom normalisé, nom de famille du parent normalisé) — même
        // clé que la requête SQL, qui a déjà filtré aux groupes de 2+ ; reconstruit juste la structure
        // ici pour calculer "suspect".
        Map<String, List<StudentFinancialRow>> byKey = new LinkedHashMap<>();
        for (StudentFinancialRow r : rows) {
            String key = normalize(r.firstName()) + "|" + normalize(r.lastName()) + "|" + normalize(r.parentLastName());
            byKey.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(r);
        }
        return byKey.values().stream()
                .map(this::toGroup)
                .sorted(Comparator.comparing(Group::lastName, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Group::firstName, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private Group toGroup(List<StudentFinancialRow> members) {
        long maxMonthly = members.stream().mapToLong(StudentFinancialRow::monthlyInvoiceCount).max().orElse(0);
        List<Candidate> candidates = members.stream()
                .map(r -> new Candidate(r.studentId(), r.firstName(), r.lastName(), r.photo(),
                        r.classId(), r.className(), r.totalInvoiced(), r.totalPaid(), r.monthlyInvoiceCount(),
                        // Aucun signal financier du tout (personne n'a de mensualité facturée dans le
                        // groupe) : rien à comparer, on ne marque jamais suspect sans preuve.
                        maxMonthly > 0 && r.monthlyInvoiceCount() < maxMonthly))
                .toList();
        StudentFinancialRow first = members.get(0);
        return new Group(first.firstName(), first.lastName(), first.parentLastName(), candidates);
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
