package gn.lacaeducation.common;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.function.BiPredicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class PortalAccessTest {

    private static final BiPredicate<Long, Long> NEVER_LINKED = (u, s) -> false;
    private static final BiPredicate<Long, Long> ALWAYS_LINKED = (u, s) -> true;

    @Test
    void isRestrictedParent_parentOnly_true() {
        assertThat(PortalAccess.isRestrictedParent(List.of("PARENT"))).isTrue();
    }

    @Test
    void isRestrictedParent_parentWithStaffRole_false() {
        // Un directeur qui est aussi parent garde l'accès complet.
        assertThat(PortalAccess.isRestrictedParent(List.of("PARENT", "DIRECTOR"))).isFalse();
    }

    @Test
    void assertParentCanAccess_restrictedParentUnlinkedChild_throws() {
        assertThatThrownBy(() ->
                PortalAccess.assertParentCanAccessStudent(List.of("PARENT"), 7L, 99L, NEVER_LINKED))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void assertParentCanAccess_restrictedParentLinkedChild_ok() {
        assertThatCode(() ->
                PortalAccess.assertParentCanAccessStudent(List.of("PARENT"), 7L, 11L, ALWAYS_LINKED))
                .doesNotThrowAnyException();
    }

    @Test
    void assertParentCanAccess_staff_neverBlocked() {
        // Le personnel n'est jamais bloqué, même si le lien est absent.
        assertThatCode(() ->
                PortalAccess.assertParentCanAccessStudent(List.of("TEACHER"), 7L, 99L, NEVER_LINKED))
                .doesNotThrowAnyException();
    }
}
