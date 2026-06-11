package gn.lacaeducation.academics.application.service;

import gn.lacaeducation.academics.application.port.in.GererEmploiDuTempsUseCase;
import gn.lacaeducation.academics.application.port.out.EmploiDuTempsRepository;
import gn.lacaeducation.academics.domain.EmploiDuTemps;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class EmploiDuTempsService implements GererEmploiDuTempsUseCase {

    private final EmploiDuTempsRepository edtRepository;

    @Override
    public EmploiDuTemps create(CreateScheduleCommand command) {
        if (edtRepository.existsRoomConflict(command.roomId(), command.day(),
                command.startTime(), command.endTime())) {
            throw new IllegalStateException(
                    "Room conflict: room " + command.roomId()
                            + " already booked on " + command.day()
                            + " " + command.startTime() + "-" + command.endTime());
        }

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
        EmploiDuTemps e = edtRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + id));
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
}
