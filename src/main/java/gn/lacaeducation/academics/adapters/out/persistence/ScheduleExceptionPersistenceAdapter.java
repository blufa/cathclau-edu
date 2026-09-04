package gn.lacaeducation.academics.adapters.out.persistence;

import gn.lacaeducation.academics.adapters.out.persistence.mapper.AcademicsMapper;
import gn.lacaeducation.academics.application.port.out.ScheduleExceptionRepository;
import gn.lacaeducation.academics.domain.ScheduleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class ScheduleExceptionPersistenceAdapter implements ScheduleExceptionRepository {

    private final ScheduleExceptionJpaRepository jpaRepo;
    private final AcademicsMapper mapper;

    @Override
    public ScheduleException save(ScheduleException exception) {
        return mapper.toDomain(jpaRepo.save(mapper.toEntity(exception)));
    }

    @Override
    public Optional<ScheduleException> findById(Long id) {
        return jpaRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ScheduleException> findByOriginalScheduleIdAndWeek(Long originalScheduleId, LocalDate weekStartDate) {
        return jpaRepo.findByOriginalScheduleIdAndWeekStartDate(originalScheduleId, weekStartDate).map(mapper::toDomain);
    }

    @Override
    public List<ScheduleException> findByWeek(LocalDate weekStartDate) {
        return jpaRepo.findByWeekStartDate(weekStartDate).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaRepo.deleteById(id);
    }

    @Override
    public void deleteByOriginalScheduleIdAndWeek(Long originalScheduleId, LocalDate weekStartDate) {
        jpaRepo.deleteByOriginalScheduleIdAndWeekStartDate(originalScheduleId, weekStartDate);
    }
}
