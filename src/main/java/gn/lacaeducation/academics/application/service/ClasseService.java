package gn.lacaeducation.academics.application.service;

import gn.lacaeducation.academics.application.port.in.GererClasseUseCase;
import gn.lacaeducation.academics.application.port.out.ClasseRepository;
import gn.lacaeducation.academics.application.port.out.EnrollmentQueryPort;
import gn.lacaeducation.academics.domain.Classe;
import gn.lacaeducation.academics.domain.ClasseMatiere;
import gn.lacaeducation.academics.domain.ClassStudent;
import gn.lacaeducation.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class ClasseService implements GererClasseUseCase {

    private final ClasseRepository classeRepository;
    private final EnrollmentQueryPort enrollmentQueryPort;

    @Override
    public Classe create(CreateClassCommand command) {
        Classe classe = Classe.builder()
                .tenantId(command.tenantId())
                .levelId(command.levelId())
                .academicYearId(command.academicYearId())
                .headTeacherId(command.headTeacherId())
                .roomId(command.roomId())
                .label(command.label())
                .headcount(0)
                .maxCapacity(command.maxCapacity())
                .build();
        return classeRepository.save(classe);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Classe> findById(Long id) {
        return classeRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Classe> findByAcademicYear(Long academicYearId) {
        return classeRepository.findByAcademicYearId(academicYearId);
    }

    @Override
    public ClasseMatiere assignSubject(Long classId, Long subjectId, Long teacherId) {
        ClasseMatiere cm = ClasseMatiere.builder()
                .schoolClassId(classId)
                .subjectId(subjectId)
                .teacherId(teacherId)
                .build();
        return classeRepository.saveClassSubject(cm);
    }

    @Override
    public Classe update(Long id, CreateClassCommand command) {
        Classe c = classeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Class", id));
        c.setLevelId(command.levelId()); c.setAcademicYearId(command.academicYearId());
        c.setHeadTeacherId(command.headTeacherId()); c.setRoomId(command.roomId());
        c.setLabel(command.label()); c.setMaxCapacity(command.maxCapacity());
        return classeRepository.save(c);
    }

    @Override @Transactional(readOnly=true)
    public List<Classe> findByTenantId(Long tenantId) { return classeRepository.findByTenantId(tenantId); }

    @Override public void removeSubject(Long classSubjectId) { classeRepository.deleteClassSubjectById(classSubjectId); }

    @Override public void delete(Long id) { classeRepository.deleteById(id); }

    @Override @Transactional(readOnly = true)
    public List<ClasseMatiere> findSubjects(Long classId) { return classeRepository.findSubjectsByClassId(classId); }

    @Override @Transactional(readOnly = true)
    public List<ClassStudent> findStudentsByClassId(Long classId) {
        return classeRepository.findStudentsByClassId(classId);
    }

    @Override @Transactional(readOnly = true)
    public EnrollmentCountInfo getEnrollmentCount(Long classId) {
        Classe classe = classeRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class", classId));
        long current = enrollmentQueryPort.countValidatedByClassId(classId);
        long available = classe.getMaxCapacity() != null ? classe.getMaxCapacity() - current : Long.MAX_VALUE;
        return new EnrollmentCountInfo(classId, current, classe.getMaxCapacity(), available);
    }
}
