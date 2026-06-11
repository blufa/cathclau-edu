package gn.lacaeducation.attendance.adapters.out.persistence;
import gn.lacaeducation.attendance.application.port.out.TeacherAttendanceRepository;
import gn.lacaeducation.attendance.domain.TeacherAttendance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class TeacherAttendancePersistenceAdapter implements TeacherAttendanceRepository {
    private final TeacherAttendanceJpaRepository repo;
    private final AttendanceMapper mapper;
    @Override public TeacherAttendance save(TeacherAttendance a) { return mapper.toDomain(repo.save(mapper.toEntity(a))); }
    @Override public Optional<TeacherAttendance> findById(Long id) { return repo.findById(id).map(mapper::toDomain); }
    @Override public List<TeacherAttendance> findByTeacherIdAndDateBetween(Long t, LocalDate f, LocalDate to) { return repo.findByTeacherIdAndDateBetween(t,f,to).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { repo.deleteById(id); }
}
