package gn.lacaeducation.iam.adapters.in.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

import static org.assertj.core.api.Assertions.assertThat;

class IamPermissionEvaluatorTest {

    private final IamPermissionEvaluator evaluator = new IamPermissionEvaluator();

    private Authentication authWith(String... authorities) {
        return new UsernamePasswordAuthenticationToken("user", "n/a",
                AuthorityUtils.createAuthorityList(authorities));
    }

    @Test
    void grantsWhenAuthorityMatchesResourceAction() {
        Authentication auth = authWith("user:create", "note:read");
        assertThat(evaluator.hasPermission(auth, "user", "create")).isTrue();
    }

    @Test
    void deniesWhenAuthorityMissing() {
        Authentication auth = authWith("note:read");
        assertThat(evaluator.hasPermission(auth, "user", "create")).isFalse();
    }

    @Test
    void deniesOnNullArguments() {
        Authentication auth = authWith("user:create");
        assertThat(evaluator.hasPermission(auth, null, "create")).isFalse();
        assertThat(evaluator.hasPermission(null, "user", "create")).isFalse();
    }

    @Test
    void targetTypeOverloadMatches() {
        Authentication auth = authWith("eleve:read");
        assertThat(evaluator.hasPermission(auth, 1L, "eleve", "read")).isTrue();
        assertThat(evaluator.hasPermission(auth, 1L, "eleve", "delete")).isFalse();
    }
}
