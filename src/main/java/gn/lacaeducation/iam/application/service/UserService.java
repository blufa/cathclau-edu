package gn.lacaeducation.iam.application.service;

import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.iam.application.port.in.UserUseCase;
import gn.lacaeducation.iam.application.port.out.PasswordHasher;
import gn.lacaeducation.iam.application.port.out.RoleRepository;
import gn.lacaeducation.iam.application.port.out.UserRepository;
import gn.lacaeducation.iam.domain.Role;
import gn.lacaeducation.iam.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService implements UserUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordHasher passwordHasher;

    @Override
    public User create(CreateUserCommand command) {
        User user = User.builder()
                .tenantId(command.tenantId())
                .firstName(command.firstName())
                .lastName(command.lastName())
                .email(command.email())
                .passwordHash(passwordHasher.hash(command.password()))
                .phone(command.phone())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findByTenantId(Long tenantId) {
        return userRepository.findByTenantId(tenantId);
    }

    @Override
    public User update(Long id, UpdateUserCommand command) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setFirstName(command.firstName());
        user.setLastName(command.lastName());
        user.setPhone(command.phone());
        if (command.active() != null) {
            user.setActive(command.active());
        }
        return userRepository.save(user);
    }

    @Override
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public void assignRole(Long userId, Long roleId) {
        userRepository.assignRole(userId, roleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> findUserRoles(Long userId) {
        return roleRepository.findUserRoles(userId);
    }
}
