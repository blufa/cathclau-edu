package gn.lacaeducation.payroll.application.service;

import gn.lacaeducation.billing.application.event.StaffChildDeductionExcludedEvent;
import gn.lacaeducation.payroll.application.port.out.PayslipRepository;
import gn.lacaeducation.payroll.domain.EmployeeType;
import gn.lacaeducation.payroll.domain.PayslipStatus;
import gn.lacaeducation.staff.EmployeePublicInfoProvider;
import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import gn.lacaeducation.students.ParentAccessProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Quand un enfant est retiré/réintégré de la retenue sur salaire DEPUIS UN ÉCRAN DE FACTURATION (fiche
 * élève, modal d'encaissement — voir InvoiceService#setStaffChildDeductionExcluded), la ligne "Reste
 * avantage personnel" d'une fiche de paie DRAFT/VALIDÉE déjà générée pour son parent salarié ne se
 * mettait jamais à jour (§retour comptable : "je retire et réintègre depuis la fiche de l'élève et
 * même l'encaissement, mais je ne vois pas de changement sur le montant") — contrairement au même geste
 * fait DEPUIS le détail de la fiche de paie elle-même, qui rafraîchit déjà explicitement côté frontend
 * (voir PayrollService#refreshStaffBenefitDeductionLine). Ce listener referme la boucle.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StaffChildDeductionRefreshListener {

    private final ParentAccessProvider parentAccessProvider;
    private final TeacherPublicInfoProvider teacherProvider;
    private final EmployeePublicInfoProvider employeeProvider;
    private final PayslipRepository payslipRepo;
    private final PayrollService payrollService;

    /** {@code teacher} : vrai si enseignant (référentiel Teacher), faux si personnel non-enseignant
     * (référentiel Employee, qui regroupe ADMINISTRATIVE/DIRECTION/SUPPORT côté paie — le lien
     * élève↔personnel ne distingue jamais lequel précisément, voir ParentAccessProvider.DirectStaffLink,
     * donc on filtre seulement "pas enseignant" côté fiches de paie plutôt qu'un type exact). */
    private record StaffPair(boolean teacher, Long staffId) {}

    private List<StaffPair> resolveStaffPairs(Long studentId) {
        List<StaffPair> pairs = new ArrayList<>();
        parentAccessProvider.findParentUserIdsByStudent(studentId).forEach(uid -> {
            teacherProvider.findTeacherIdByUserId(uid).ifPresent(id -> pairs.add(new StaffPair(true, id)));
            employeeProvider.findEmployeeIdByUserId(uid).ifPresent(id -> pairs.add(new StaffPair(false, id)));
        });
        parentAccessProvider.findDirectStaffLinksByStudent(studentId).forEach(link ->
                pairs.add(new StaffPair("TEACHER".equals(link.staffType()), link.staffId())));
        return pairs;
    }

    @EventListener
    @Transactional
    public void onStaffChildDeductionExcluded(StaffChildDeductionExcludedEvent event) {
        for (StaffPair pair : resolveStaffPairs(event.studentId())) {
            payslipRepo.findByTenantIdAndTeacherId(event.tenantId(), pair.staffId()).stream()
                    .filter(p -> (p.getEmployeeType() == EmployeeType.TEACHER) == pair.teacher())
                    .filter(p -> p.getStatus() != PayslipStatus.PAID)
                    .forEach(p -> {
                        try {
                            payrollService.refreshStaffBenefitDeductionLine(p.getId());
                        } catch (Exception e) {
                            log.warn("Rafraîchissement de la retenue impossible pour la fiche de paie id={} "
                                            + "après modification de l'élève id={} : {}",
                                    p.getId(), event.studentId(), e.getMessage());
                        }
                    });
        }
    }
}
