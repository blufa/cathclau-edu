package gn.lacaeducation.students.application.port.out;

import gn.lacaeducation.students.domain.Parent;
import java.util.List;
import java.util.Optional;

public interface ParentRepository {
    Parent save(Parent parent);
    Optional<Parent> findById(Long id);
    Optional<Parent> findByUserId(Long userId);
    List<Parent> findByTenantId(Long tenantId);
    void deleteById(Long id);
    boolean existsById(Long id);
}
