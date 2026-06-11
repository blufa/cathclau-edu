package gn.lacaeducation.students.adapters.out.persistence;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.ParentPublicInfo;
import gn.lacaeducation.students.StudentPublicInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
public class StudentDataProviderAdapter implements StudentDataProvider {
    private final EleveJpaRepository eleveRepo;
    private final ParentJpaRepository parentRepo;
    private final EleveParentJpaRepository eleveParentRepo;
    private final StudentsMapper mapper;

    @Override
    public Optional<StudentPublicInfo> findStudent(Long studentId) {
        return eleveRepo.findById(studentId).map(e ->
                new StudentPublicInfo(e.getId(), e.getTenantId(), e.getFirstName(), e.getLastName(), e.getEmail(), e.getPhone()));
    }

    @Override
    public List<ParentPublicInfo> findParentsByStudent(Long studentId) {
        return eleveParentRepo.findByStudentId(studentId).stream()
                .map(ep -> parentRepo.findById(ep.getParentId()).orElse(null))
                .filter(p -> p != null)
                .map(p -> new ParentPublicInfo(p.getId(), p.getFirstName(), p.getLastName(), p.getEmail(), p.getPhone()))
                .toList();
    }
}
