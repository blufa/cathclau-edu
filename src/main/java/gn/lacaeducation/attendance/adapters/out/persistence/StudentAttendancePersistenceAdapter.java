package gn.lacaeducation.attendance.adapters.out.persistence;
import gn.lacaeducation.attendance.application.port.out.StudentAttendanceRepository;
import gn.lacaeducation.attendance.domain.AttendanceStatus;
import gn.lacaeducation.attendance.domain.StudentAttendance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class StudentAttendancePersistenceAdapter implements StudentAttendanceRepository {
    private final StudentAttendanceJpaRepository repo;
    private final AttendanceMapper mapper;
    @Override public StudentAttendance save(StudentAttendance a) { return mapper.toDomain(repo.save(mapper.toEntity(a))); }
    @Override public Optional<StudentAttendance> findById(Long id) { return repo.findById(id).map(mapper::toDomain); }
    @Override public Optional<StudentAttendance> findByStudentIdAndClassIdAndDate(Long s, Long c, LocalDate d) { return repo.findByStudentIdAndClassIdAndDate(s,c,d).map(mapper::toDomain); }
    @Override public List<StudentAttendance> findByClassIdAndDate(Long c, LocalDate d) { return repo.findByClassIdAndDate(c,d).stream().map(mapper::toDomain).toList(); }
    @Override public List<StudentAttendance> findByStudentIdAndDateBetween(Long s, LocalDate f, LocalDate t) { return repo.findByStudentIdAndDateBetween(s,f,t).stream().map(mapper::toDomain).toList(); }
    @Override public List<StudentAttendance> findByClassIdAndDateBetweenAndStatus(Long c, LocalDate f, LocalDate t, AttendanceStatus st) { return repo.findByClassIdAndDateBetweenAndStatus(c,f,t,st).stream().map(mapper::toDomain).toList(); }
    @Override public long countByStudentIdAndDateBetweenAndStatus(Long s, LocalDate f, LocalDate t, AttendanceStatus st) { return repo.countByStudentIdAndDateBetweenAndStatus(s,f,t,st); }
    @Override public long countByStudentIdAndDateBetween(Long s, LocalDate f, LocalDate t) { return repo.countByStudentIdAndDateBetween(s,f,t); }
    @Override public void deleteById(Long id) { repo.deleteById(id); }
}
