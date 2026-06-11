package gn.lacaeducation.academics.application.service;

import gn.lacaeducation.academics.application.port.in.GererEmploiDuTempsUseCase;
import gn.lacaeducation.academics.application.port.out.EmploiDuTempsRepository;
import gn.lacaeducation.academics.domain.EmploiDuTemps;
import gn.lacaeducation.academics.domain.valueobject.JourSemaine;
import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class EmploiDuTempsService implements GererEmploiDuTempsUseCase {

    private final EmploiDuTempsRepository edtRepository;

    @Override
    public EmploiDuTemps create(CreateScheduleCommand command) {
        checkConflicts(command.roomId(), command.teacherId(), command.schoolClassId(),
                command.day(), command.startTime(), command.endTime(), null);

        EmploiDuTemps edt = EmploiDuTemps.builder()
                .tenantId(command.tenantId())
                .schoolClassId(command.schoolClassId())
                .subjectId(command.subjectId())
                .teacherId(command.teacherId())
                .roomId(command.roomId())
                .day(command.day())
                .startTime(command.startTime())
                .endTime(command.endTime())
                .build();
        return edtRepository.save(edt);
    }

    @Override
    public EmploiDuTemps update(Long id, CreateScheduleCommand command) {
        EmploiDuTemps e = edtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", id));
        checkConflicts(command.roomId(), command.teacherId(), command.schoolClassId(),
                command.day(), command.startTime(), command.endTime(), id);
        e.setSubjectId(command.subjectId()); e.setTeacherId(command.teacherId()); e.setRoomId(command.roomId());
        e.setDay(command.day()); e.setStartTime(command.startTime()); e.setEndTime(command.endTime());
        return edtRepository.save(e);
    }

    @Override @Transactional(readOnly=true)
    public Optional<EmploiDuTemps> findById(Long id) { return edtRepository.findById(id); }
    @Override @Transactional(readOnly = true)
    public List<EmploiDuTemps> findByClass(Long classId) { return edtRepository.findByClassId(classId); }
    @Override @Transactional(readOnly=true)
    public List<EmploiDuTemps> findByTeacher(Long teacherId) { return edtRepository.findByTeacherId(teacherId); }
    @Override public void delete(Long id) { edtRepository.deleteById(id); }

    /**
     * Checks for scheduling conflicts for room, teacher, and class.
     * @param excludeId when updating, the ID of the slot being updated (excluded from conflict check)
     */
    private void checkConflicts(Long roomId, Long teacherId, Long classId,
                                  JourSemaine day, LocalTime startTime, LocalTime endTime, Long excludeId) {
        boolean roomConflict = excludeId == null
                ? edtRepository.existsRoomConflict(roomId, day, startTime, endTime)
                : edtRepository.existsRoomConflictExcluding(roomId, day, startTime, endTime, excludeId);
        if (roomConflict) {
            throw new ConflictException("Room conflict: room id=" + roomId
                    + " is already booked on " + day + " " + startTime + "-" + endTime);
        }

        if (teacherId != null) {
            boolean teacherConflict = excludeId == null
                    ? edtRepository.existsTeacherConflict(teacherId, day, startTime, endTime)
                    : edtRepository.existsTeacherConflictExcluding(teacherId, day, startTime, endTime, excludeId);
            if (teacherConflict) {
                throw new ConflictException("Teacher conflict: teacher id=" + teacherId
                        + " is already scheduled on " + day + " " + startTime + "-" + endTime);
            }
        }

        if (classId != null) {
            boolean classConflict = excludeId == null
                    ? edtRepository.existsClassConflict(classId, day, startTime, endTime)
                    : edtRepository.existsClassConflictExcluding(classId, day, startTime, endTime, excludeId);
            if (classConflict) {
                throw new ConflictException("Class conflict: class id=" + classId
                        + " already has a slot on " + day + " " + startTime + "-" + endTime);
            }
        }
    }
}
