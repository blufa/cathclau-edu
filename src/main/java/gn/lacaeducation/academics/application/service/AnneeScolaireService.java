package gn.lacaeducation.academics.application.service;

import gn.lacaeducation.academics.application.event.AnneeScolaireArchiveeEvent;
import gn.lacaeducation.academics.application.event.AnneeScolaireClotureeEvent;
import gn.lacaeducation.academics.application.event.PeriodeClotureeEvent;
import gn.lacaeducation.academics.application.port.in.GererAnneeScolaireUseCase;
import gn.lacaeducation.academics.application.port.out.AnneeScolaireRepository;
import gn.lacaeducation.academics.application.port.out.PeriodeRepository;
import gn.lacaeducation.academics.domain.AnneeScolaire;
import gn.lacaeducation.academics.domain.Periode;
import gn.lacaeducation.academics.domain.valueobject.StatutAnneeScolaire;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class AnneeScolaireService implements GererAnneeScolaireUseCase {

    private final AnneeScolaireRepository anneeRepository;
    private final PeriodeRepository periodeRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public AnneeScolaire create(CreateAcademicYearCommand command) {
        AnneeScolaire annee = AnneeScolaire.builder()
                .tenantId(command.tenantId())
                .label(command.label())
                .startDate(command.startDate())
                .endDate(command.endDate())
                .status(StatutAnneeScolaire.PLANIFIEE)
                .current(false)
                .createdAt(LocalDateTime.now())
                .build();
        return anneeRepository.save(annee);
    }

    @Override
    public AnneeScolaire update(Long academicYearId, CreateAcademicYearCommand command) {
        AnneeScolaire annee = load(academicYearId);
        if (annee.getStatus() == StatutAnneeScolaire.ARCHIVEE) {
            throw new BusinessRuleException("Archived academic years are read-only and cannot be updated.");
        }
        annee.setLabel(command.label());
        annee.setStartDate(command.startDate());
        annee.setEndDate(command.endDate());
        return anneeRepository.save(annee);
    }

    @Override
    public AnneeScolaire setCurrent(Long academicYearId) {
        AnneeScolaire annee = load(academicYearId);
        if (annee.getStatus() == StatutAnneeScolaire.ARCHIVEE) {
            throw new BusinessRuleException("An archived academic year cannot be set as current.");
        }
        // Exactly one current year per tenant: flip the previous current off, then set this one.
        anneeRepository.removeCurrent(annee.getTenantId());
        annee.setCurrent(true);
        return anneeRepository.save(annee);
    }

    @Override
    public AnneeScolaire start(Long academicYearId) {
        AnneeScolaire annee = load(academicYearId);
        validateTransition(annee.getStatus(), StatutAnneeScolaire.PLANIFIEE, StatutAnneeScolaire.EN_COURS);
        anneeRepository.removeCurrent(annee.getTenantId());
        annee.setStatus(StatutAnneeScolaire.EN_COURS);
        annee.setCurrent(true);
        return anneeRepository.save(annee);
    }

    @Override
    public AnneeScolaire close(Long academicYearId) {
        AnneeScolaire annee = load(academicYearId);
        validateTransition(annee.getStatus(), StatutAnneeScolaire.EN_COURS, StatutAnneeScolaire.CLOTUREE);

        // Refuse if any open periods remain
        List<Periode> openPeriods = periodeRepository.findByAcademicYearId(academicYearId).stream()
                .filter(p -> !Boolean.TRUE.equals(p.getClosed()))
                .toList();
        if (!openPeriods.isEmpty()) {
            throw new BusinessRuleException(
                    "Cannot close academic year id=" + academicYearId
                            + ": " + openPeriods.size() + " period(s) are still open. Close all periods first.");
        }

        annee.setStatus(StatutAnneeScolaire.CLOTUREE);
        annee.setCurrent(false);
        AnneeScolaire saved = anneeRepository.save(annee);
        eventPublisher.publishEvent(new AnneeScolaireClotureeEvent(saved.getTenantId(), saved.getId()));
        return saved;
    }

    @Override
    public AnneeScolaire archive(Long academicYearId) {
        AnneeScolaire annee = load(academicYearId);
        validateTransition(annee.getStatus(), StatutAnneeScolaire.CLOTUREE, StatutAnneeScolaire.ARCHIVEE);
        annee.setStatus(StatutAnneeScolaire.ARCHIVEE);
        AnneeScolaire saved = anneeRepository.save(annee);
        eventPublisher.publishEvent(new AnneeScolaireArchiveeEvent(saved.getTenantId(), saved.getId()));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnneeScolaire> findById(Long id) {
        return anneeRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnneeScolaire> findCurrent(Long tenantId) {
        return anneeRepository.findCurrent(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnneeScolaire> findByTenantId(Long tenantId) {
        return anneeRepository.findByTenantId(tenantId);
    }

    @Override
    public Periode createPeriod(CreatePeriodCommand command) {
        Periode periode = Periode.builder()
                .academicYearId(command.academicYearId())
                .label(command.label())
                .type(command.type())
                .sortOrder(command.sortOrder())
                .startDate(command.startDate())
                .endDate(command.endDate())
                .closed(false)
                .build();
        return periodeRepository.save(periode);
    }

    @Override
    public Periode updatePeriod(Long periodId, CreatePeriodCommand command) {
        Periode periode = periodeRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Period", periodId));
        if (Boolean.TRUE.equals(periode.getClosed())) {
            throw new BusinessRuleException("A closed period is read-only and cannot be updated: id=" + periodId);
        }
        periode.setLabel(command.label());
        periode.setType(command.type());
        periode.setSortOrder(command.sortOrder());
        periode.setStartDate(command.startDate());
        periode.setEndDate(command.endDate());
        return periodeRepository.save(periode);
    }

    @Override
    public Periode closePeriod(Long periodId) {
        Periode periode = periodeRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Period", periodId));

        if (Boolean.TRUE.equals(periode.getClosed())) {
            throw new BusinessRuleException("Period is already closed: id=" + periodId);
        }

        periode.setClosed(true);
        Periode saved = periodeRepository.save(periode);

        AnneeScolaire annee = anneeRepository.findById(periode.getAcademicYearId())
                .orElseThrow(() -> new ResourceNotFoundException("Academic year for period id=" + periodId));

        eventPublisher.publishEvent(new PeriodeClotureeEvent(
                annee.getTenantId(), periode.getId(), periode.getAcademicYearId()
        ));

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Periode> findPeriods(Long academicYearId) {
        return periodeRepository.findByAcademicYearId(academicYearId);
    }

    /**
     * Validates that the current status matches the expected source status for a transition.
     * Throws BusinessRuleException (HTTP 422) on illegal transitions.
     */
    private void validateTransition(StatutAnneeScolaire current, StatutAnneeScolaire expectedFrom, StatutAnneeScolaire targetTo) {
        if (current == StatutAnneeScolaire.ARCHIVEE) {
            throw new BusinessRuleException(
                    "Archived academic years cannot change status.");
        }
        if (current != expectedFrom) {
            throw new BusinessRuleException(
                    "Illegal status transition: cannot move from " + current + " to " + targetTo
                            + ". Expected current status: " + expectedFrom + ".");
        }
    }

    private AnneeScolaire load(Long id) {
        return anneeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", id));
    }
}
