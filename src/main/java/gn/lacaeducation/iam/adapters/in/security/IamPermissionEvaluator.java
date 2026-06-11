package gn.lacaeducation.iam.adapters.in.security;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serializable;

/**
 * Centralizes authorization so controllers can use
 * {@code @PreAuthorize("hasPermission('user', 'create')")}. The granted authorities carry
 * the {@code resource:action} permissions extracted from the JWT (see SecurityConfig).
 */
public class IamPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || targetDomainObject == null || permission == null) {
            return false;
        }
        return hasAuthority(authentication, targetDomainObject + ":" + permission);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId,
                                 String targetType, Object permission) {
        if (authentication == null || targetType == null || permission == null) {
            return false;
        }
        return hasAuthority(authentication, targetType + ":" + permission);
    }

    private boolean hasAuthority(Authentication authentication, String required) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(required::equals);
    }
}
