package gn.lacaeducation.staff.application.service;

import gn.lacaeducation.staff.application.port.in.TeacherUseCase;
import gn.lacaeducation.staff.application.port.out.TeacherRepository;
import gn.lacaeducation.staff.domain.Teacher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service @Transactional @RequiredArgsConstructor
public class EnseignantService implements TeacherUseCase {

    private final TeacherRepository teacherRepository;

    @Override
    public Teacher create(CreateTeacherCommand cmd) {
        return teacherRepository.save(Teacher.builder()
                .tenantId(cmd.tenantId()).userId(cmd.userId()).staffNumber(cmd.staffNumber())
                .firstName(cmd.firstName()).lastName(cmd.lastName()).phone(cmd.phone())
                .email(cmd.email()).specialization(cmd.specialization()).hireDate(cmd.hireDate()).build());
    }

    @Override
    public Teacher update(Long id, UpdateTeacherCommand cmd) {
        Teacher t = teacherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + id));
        t.setStaffNumber(cmd.staffNumber()); t.setFirstName(cmd.firstName()); t.setLastName(cmd.lastName());
        t.setPhone(cmd.phone()); t.setEmail(cmd.email()); t.setSpecialization(cmd.specialization()); t.setHireDate(cmd.hireDate());
        return teacherRepository.save(t);
    }

    @Override @Transactional(readOnly = true)
    public Optional<Teacher> findById(Long id) { return teacherRepository.findById(id); }

    @Override @Transactional(readOnly = true)
    public List<Teacher> findByTenantId(Long tenantId) { return teacherRepository.findByTenantId(tenantId); }

    @Override
    public void delete(Long id) { teacherRepository.deleteById(id); }

    @Override @Transactional(readOnly = true)
    public boolean existsById(Long id) { return teacherRepository.existsById(id); }
}
