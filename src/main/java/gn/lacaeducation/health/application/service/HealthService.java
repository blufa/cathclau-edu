package gn.lacaeducation.health.application.service;

import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.health.application.port.out.HealthRecordRepository;
import gn.lacaeducation.health.application.port.out.InfirmaryVisitRepository;
import gn.lacaeducation.health.domain.HealthRecord;
import gn.lacaeducation.health.domain.InfirmaryVisit;
import gn.lacaeducation.health.domain.VisitOutcome;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Module Santé / infirmerie : fiche santé de l'élève (allergies, PAI, contacts d'urgence) et passages
 * à l'infirmerie. Module autonome : ne référence l'élève que par identifiant.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class HealthService {

    private final HealthRecordRepository recordRepo;
    private final InfirmaryVisitRepository visitRepo;

    // ── Fiche santé ────────────────────────────────────────────────────────────

    public record HealthRecordCommand(Long tenantId, Long studentId, String bloodGroup, String allergies,
                                      String chronicConditions, String medications, Boolean pai, String paiDetails,
                                      String dietaryRestrictions, String doctorName, String doctorPhone,
                                      String emergencyContactName, String emergencyContactPhone,
                                      String emergencyContactRelation, String insurance, String notes) {}

    /** Crée ou met à jour la fiche santé de l'élève (une par élève). */
    public HealthRecord upsertRecord(HealthRecordCommand c) {
        HealthRecord r = recordRepo.findByStudentId(c.studentId())
                .orElseGet(() -> HealthRecord.builder().tenantId(c.tenantId()).studentId(c.studentId()).build());
        r.setBloodGroup(c.bloodGroup());
        r.setAllergies(c.allergies());
        r.setChronicConditions(c.chronicConditions());
        r.setMedications(c.medications());
        r.setPai(c.pai() != null && c.pai());
        r.setPaiDetails(c.paiDetails());
        r.setDietaryRestrictions(c.dietaryRestrictions());
        r.setDoctorName(c.doctorName());
        r.setDoctorPhone(c.doctorPhone());
        r.setEmergencyContactName(c.emergencyContactName());
        r.setEmergencyContactPhone(c.emergencyContactPhone());
        r.setEmergencyContactRelation(c.emergencyContactRelation());
        r.setInsurance(c.insurance());
        r.setNotes(c.notes());
        r.setUpdatedAt(LocalDateTime.now());
        return recordRepo.save(r);
    }

    @Transactional(readOnly = true)
    public Optional<HealthRecord> recordByStudent(Long studentId) { return recordRepo.findByStudentId(studentId); }

    // ── Passages à l'infirmerie ─────────────────────────────────────────────────

    public record VisitCommand(Long tenantId, Long studentId, LocalDate visitDate, LocalTime visitTime, String reason,
                               String careGiven, BigDecimal temperature, VisitOutcome outcome, Long recordedBy) {}

    public InfirmaryVisit recordVisit(VisitCommand c) {
        return visitRepo.save(InfirmaryVisit.builder()
                .tenantId(c.tenantId()).studentId(c.studentId())
                .visitDate(c.visitDate() != null ? c.visitDate() : LocalDate.now())
                .visitTime(c.visitTime() != null ? c.visitTime() : LocalTime.now())
                .reason(c.reason()).careGiven(c.careGiven()).temperature(c.temperature())
                .outcome(c.outcome() != null ? c.outcome() : VisitOutcome.RETURNED_TO_CLASS)
                .parentNotified(false).recordedBy(c.recordedBy())
                .createdAt(LocalDateTime.now()).build());
    }

    public InfirmaryVisit updateVisit(Long id, VisitCommand c) {
        InfirmaryVisit v = loadVisit(id);
        v.setVisitDate(c.visitDate()); v.setVisitTime(c.visitTime());
        v.setReason(c.reason()); v.setCareGiven(c.careGiven()); v.setTemperature(c.temperature());
        v.setOutcome(c.outcome());
        return visitRepo.save(v);
    }

    public InfirmaryVisit markParentNotified(Long id) {
        InfirmaryVisit v = loadVisit(id);
        v.setParentNotified(true);
        return visitRepo.save(v);
    }

    public void deleteVisit(Long id) { visitRepo.deleteById(id); }

    @Transactional(readOnly = true)
    public List<InfirmaryVisit> visits(Long tenantId, Long studentId) {
        return studentId != null ? visitRepo.findByStudentId(studentId) : visitRepo.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public InfirmaryVisit visit(Long id) { return loadVisit(id); }

    private InfirmaryVisit loadVisit(Long id) {
        return visitRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("InfirmaryVisit", id));
    }
}
