package gn.lacaeducation.academics.application.port.out;

import gn.lacaeducation.academics.domain.Periode;

import java.util.List;
import java.util.Optional;

public interface PeriodeRepository {
    Periode save(Periode period);
    Optional<Periode> findById(Long id);
    List<Periode> findByAcademicYearId(Long academicYearId);
    void deleteById(Long id);
}
