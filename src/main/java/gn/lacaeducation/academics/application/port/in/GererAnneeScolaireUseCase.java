package gn.lacaeducation.academics.application.port.in;

import gn.lacaeducation.academics.domain.AnneeScolaire;
import gn.lacaeducation.academics.domain.Periode;
import gn.lacaeducation.academics.domain.valueobject.SchoolCycle;
import gn.lacaeducation.academics.domain.valueobject.TypePeriode;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GererAnneeScolaireUseCase {

    AnneeScolaire create(CreateAcademicYearCommand command);

    AnneeScolaire update(Long academicYearId, CreateAcademicYearCommand command);

    AnneeScolaire setCurrent(Long academicYearId);

    AnneeScolaire start(Long academicYearId);

    AnneeScolaire close(Long academicYearId);

    AnneeScolaire archive(Long academicYearId);

    Optional<AnneeScolaire> findById(Long id);

    Optional<AnneeScolaire> findCurrent(Long tenantId);

    List<AnneeScolaire> findByTenantId(Long tenantId);

    Periode createPeriod(CreatePeriodCommand command);

    Periode updatePeriod(Long periodId, CreatePeriodCommand command);

    Periode closePeriod(Long periodId);

    /** Supprime une période créée par erreur — impossible si elle est déjà clôturée (bulletins générés). */
    void deletePeriod(Long periodId);

    List<Periode> findPeriods(Long academicYearId);

    record CreateAcademicYearCommand(Long tenantId, String label, LocalDate startDate, LocalDate endDate) {}

    record CreatePeriodCommand(
            Long academicYearId, String label, TypePeriode type,
            SchoolCycle cycle,
            Integer sortOrder, LocalDate startDate, LocalDate endDate,
            /** Période "composition" associée (§retour fondateur) — voir {@link Periode#getCompositionPeriodId()}. */
            Long compositionPeriodId
    ) {}
}
