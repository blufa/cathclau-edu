package gn.lacaeducation.attendance.adapters.out.persistence;
import gn.lacaeducation.attendance.application.port.out.EmployeeAttendanceRepository;
import gn.lacaeducation.attendance.domain.EmployeeAttendance;
import gn.lacaeducation.attendance.domain.EmployeeAttendanceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class EmployeeAttendancePersistenceAdapter implements EmployeeAttendanceRepository {
    private final EmployeeAttendanceJpaRepository repo;
    private final AttendanceMapper mapper;
    @Override public EmployeeAttendance save(EmployeeAttendance a) { return mapper.toDomain(repo.save(mapper.toEntity(a))); }
    @Override public Optional<EmployeeAttendance> findById(Long id) { return repo.findById(id).map(mapper::toDomain); }
    @Override public Optional<EmployeeAttendance> findByEmployeeIdAndDate(Long e, LocalDate d) { return repo.findByEmployeeIdAndDate(e, d).map(mapper::toDomain); }
    @Override public Optional<EmployeeAttendance> lockByEmployeeIdAndDate(Long e, LocalDate d) { return repo.lockByEmployeeIdAndDate(e, d).map(mapper::toDomain); }
    @Override public List<EmployeeAttendance> findByEmployeeIdAndDateBetween(Long e, LocalDate f, LocalDate to) { return repo.findByEmployeeIdAndDateBetween(e,f,to).stream().map(mapper::toDomain).toList(); }
    @Override public long countByEmployeeIdAndDateBetween(Long e, LocalDate f, LocalDate to) { return repo.countByEmployeeIdAndDateBetween(e,f,to); }
    @Override public long countByEmployeeIdAndDateBetweenAndStatus(Long e, LocalDate f, LocalDate to, EmployeeAttendanceStatus st) { return repo.countByEmployeeIdAndDateBetweenAndStatus(e,f,to,st); }
    @Override public void deleteById(Long id) { repo.deleteById(id); }
}
