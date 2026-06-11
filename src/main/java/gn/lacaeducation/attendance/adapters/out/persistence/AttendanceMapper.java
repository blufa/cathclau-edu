package gn.lacaeducation.attendance.adapters.out.persistence;
import gn.lacaeducation.attendance.domain.StudentAttendance;
import gn.lacaeducation.attendance.domain.TeacherAttendance;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")
interface AttendanceMapper {
    StudentAttendance toDomain(StudentAttendanceJpaEntity e);
    StudentAttendanceJpaEntity toEntity(StudentAttendance d);
    TeacherAttendance toDomain(TeacherAttendanceJpaEntity e);
    TeacherAttendanceJpaEntity toEntity(TeacherAttendance d);
}
