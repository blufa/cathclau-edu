package gn.lacaeducation.academics.application.service;

import gn.lacaeducation.academics.application.event.PeriodeClotureeEvent;
import gn.lacaeducation.academics.application.port.in.GererAnneeScolaireUseCase;
import gn.lacaeducation.academics.application.port.out.AnneeScolaireRepository;
import gn.lacaeducation.academics.application.port.out.PeriodeRepository;
import gn.lacaeducation.academics.domain.AnneeScolaire;
import gn.lacaeducation.academics.domain.Periode;
import gn.lacaeducation.academics.domain.valueobject.StatutAnneeScolaire;
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
    public AnneeScolaire start(Long academicYearId) {
        AnneeScolaire annee = load(academicYearId);
        anneeRepository.removeCurrent(annee.getTenantId());
        annee.setStatus(StatutAnneeScolaire.EN_COURS);
        annee.setCurrent(true);
        return anneeRepository.save(annee);
    }

    @Override
    public AnneeScolaire close(Long academicYearId) {
        AnneeScolaire annee = load(academicYearId);
        annee.setStatus(StatutAnneeScolaire.CLOTUREE);
        annee.setCurrent(false);
        return anneeRepository.save(annee);
    }

    @Override
    public AnneeScolaire archive(Long academicYearId) {
        AnneeScolaire annee = load(academicYearId);
        annee.setStatus(StatutAnneeScolaire.ARCHIVEE);
        return anneeRepository.save(annee);
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
    public Periode closePeriod(Long periodId) {
        Periode periode = periodeRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Period not found: " + periodId));

        if (Boolean.TRUE.equals(periode.getClosed())) {
            throw new IllegalStateException("Period already closed: " + periodId);
        }

        periode.setClosed(true);
        Periode saved = periodeRepository.save(periode);

        AnneeScolaire annee = anneeRepository.findById(periode.getAcademicYearId())
                .orElseThrow(() -> new IllegalStateException("Academic year missing for period " + periodId));

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

    private AnneeScolaire load(Long id) {
        return anneeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Academic year not found: " + id));
    }
}
