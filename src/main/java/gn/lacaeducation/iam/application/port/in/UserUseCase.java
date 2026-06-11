package gn.lacaeducation.iam.application.port.in;

import gn.lacaeducation.iam.domain.Role;
import gn.lacaeducation.iam.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserUseCase {
    User create(CreateUserCommand command);
    Optional<User> findById(Long id);
    List<User> findByTenantId(Long tenantId);
    User update(Long id, UpdateUserCommand command);
    void delete(Long id);
    void assignRole(Long userId, Long roleId);
    List<Role> findUserRoles(Long userId);

    record CreateUserCommand(
        Long tenantId,
        String firstName,
        String lastName,
        String email,
        String password,
        String phone
    ) {}

    record UpdateUserCommand(
        String firstName,
        String lastName,
        String phone,
        Boolean active
    ) {}
}
