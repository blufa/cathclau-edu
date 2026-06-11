package gn.lacaeducation.students.application.port.in;

import gn.lacaeducation.students.domain.*;
import java.util.List;
import java.util.Optional;

public interface ParentUseCase {
    record CreateParentCommand(Long tenantId, Long userId, String firstName, String lastName,
                               String phone, String email, String occupation, String address) {}
    record UpdateParentCommand(String firstName, String lastName, String phone,
                               String email, String occupation, String address) {}

    Parent create(CreateParentCommand command);
    Parent update(Long id, UpdateParentCommand command);
    Optional<Parent> findById(Long id);
    List<Parent> findByTenantId(Long tenantId);
    void delete(Long id);
    boolean existsById(Long id);
    StudentParent linkStudent(Long studentId, Long parentId, Relationship relationship);
    void unlinkStudent(Long studentParentId);
    List<Parent> findByStudent(Long studentId);
}
