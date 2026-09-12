package gn.lacaeducation.payroll.application.port.out;

import gn.lacaeducation.payroll.domain.TeacherVacationPayMonth;

import java.util.List;

public interface TeacherVacationPayRepository {
    TeacherVacationPayMonth save(TeacherVacationPayMonth m);
    List<TeacherVacationPayMonth> findByTeacherId(Long teacherId);
    boolean existsByTeacherIdAndYearAndMonth(Long teacherId, int year, int month);
    void deleteById(Long id);
}
