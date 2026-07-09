package gn.lacaeducation.discipline.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.discipline.application.port.out.DisciplineRepository;
import gn.lacaeducation.discipline.domain.DisciplineCategory;
import gn.lacaeducation.discipline.domain.DisciplineIncident;
import gn.lacaeducation.discipline.domain.DisciplineSeverity;
import gn.lacaeducation.discipline.domain.DisciplineStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Discipline (inspiré OpenEduCat) : signalement d'incidents par élève (type, gravité, sanction) avec
 * un cycle REPORTED → RESOLVED / DISMISSED. Module autonome : il ne référence les élèves / classes que
 * par identifiant et délègue la notification des parents au module communication existant.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DisciplineService {

    private final DisciplineRepository repo;

    public record CreateIncidentCommand(Long tenantId, Long studentId, Long classId, LocalDate incidentDate,
                                        DisciplineCategory category, DisciplineSeverity severity,
                                        String description, String sanction, Long reportedBy,
                                        gn.lacaeducation.discipline.domain.MeasureKind measureKind,
                                        gn.lacaeducation.discipline.domain.DisciplineMeasure measure,
                                        gn.lacaeducation.discipline.domain.Suspension suspension,
                                        Integer exclusionDays,
                                        gn.lacaeducation.discipline.domain.DecisionBody decisionBody) {}

    public DisciplineIncident create(CreateIncidentCommand c) {
        if (c.studentId() == null) throw new BusinessRuleException("L'élève est requis");
        if (c.description() == null || c.description().isBlank()) throw new BusinessRuleException("La description est requise");
        return repo.save(DisciplineIncident.builder()
                .tenantId(c.tenantId()).studentId(c.studentId()).classId(c.classId())
                .incidentDate(c.incidentDate() != null ? c.incidentDate() : LocalDate.now())
                .category(c.category() != null ? c.category() : DisciplineCategory.OTHER)
                .severity(c.severity() != null ? c.severity() : DisciplineSeverity.MINOR)
                .description(c.description()).sanction(c.sanction()).reportedBy(c.reportedBy())
                .measureKind(c.measureKind() != null ? c.measureKind() : gn.lacaeducation.discipline.domain.MeasureKind.NONE)
                .measure(c.measure()).suspension(c.suspension()).exclusionDays(c.exclusionDays()).decisionBody(c.decisionBody())
                .status(DisciplineStatus.REPORTED).parentNotified(false)
                .createdAt(LocalDateTime.now()).build());
    }

    public DisciplineIncident update(Long id, CreateIncidentCommand c) {
        DisciplineIncident i = load(id);
        i.setClassId(c.classId()); i.setIncidentDate(c.incidentDate());
        i.setCategory(c.category()); i.setSeverity(c.severity());
        i.setDescription(c.description()); i.setSanction(c.sanction());
        i.setMeasureKind(c.measureKind()); i.setMeasure(c.measure());
        i.setSuspension(c.suspension()); i.setExclusionDays(c.exclusionDays()); i.setDecisionBody(c.decisionBody());
        return repo.save(i);
    }

    public DisciplineIncident resolve(Long id) { return transition(id, DisciplineStatus.RESOLVED); }

    public DisciplineIncident dismiss(Long id) { return transition(id, DisciplineStatus.DISMISSED); }

    public DisciplineIncident markParentNotified(Long id) {
        DisciplineIncident i = load(id);
        i.setParentNotified(true);
        return repo.save(i);
    }

    public void delete(Long id) { repo.deleteById(id); }

    @Transactional(readOnly = true)
    public List<DisciplineIncident> find(Long tenantId, Long studentId, DisciplineStatus status) {
        if (studentId != null) return repo.findByTenantIdAndStudentId(tenantId, studentId);
        if (status != null) return repo.findByTenantIdAndStatus(tenantId, status);
        return repo.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public DisciplineIncident findById(Long id) { return load(id); }

    private DisciplineIncident transition(Long id, DisciplineStatus target) {
        DisciplineIncident i = load(id);
        i.setStatus(target);
        return repo.save(i);
    }

    private DisciplineIncident load(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("DisciplineIncident", id));
    }
}
